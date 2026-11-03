package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.account.api.spi.*;
import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.internal.careplan.domain.*;
import com.eldercare.backend.care.internal.careplan.persistence.CarePlanStore;
import com.eldercare.backend.care.internal.careplan.service.CarePlanService;
import com.eldercare.backend.care.internal.catalog.domain.TaskTypeAttributes;
import com.eldercare.backend.care.internal.catalog.service.CareCatalogService;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.dto.*;
import com.eldercare.backend.support.*;
import com.eldercare.backend.support.fake.administration.*;
import com.eldercare.backend.support.fake.resident.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;

/** Hai transaction MySQL thật: snapshot RR cũ không được ghi đè mục vừa commit sau khi đợi resident lock. */
@Import({FR_028a_ReasonCatalogHttpIT.Users.class, FakeM15Config.class, FakeM01Config.class})
class FR_003_CarePlanConcurrencyIT extends MySqlIntegrationTestBase {
    @Autowired
    CarePlanService service;

    @Autowired
    CarePlanStore store;

    @Autowired
    CareCatalogService catalogs;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser user;

    @Autowired
    FakeResidents residents;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    PlatformTransactionManager manager;

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.eldercare.backend.care.internal.status.persistence.CareLockStore locks;

    private CarePlanItemData data(long typeId, String content) {
        return new CarePlanItemData(
                typeId,
                content,
                "ONCE_PER_SHIFT",
                null,
                null,
                null,
                List.of(),
                List.of(),
                0,
                0,
                Role.CAREGIVER,
                ResultType.PERFORMED,
                Importance.NORMAL,
                false,
                null);
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(15, TimeUnit.SECONDS)) throw new IllegalStateException("Không tới điểm đồng bộ test");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(error);
        }
    }

    @Test
    void FR_003_submitUsesCurrentItemsAfterWaitingForConcurrentUpdate() throws Exception {
        long residentId = TestUsers.nextId();
        var users = new TestUsers(jdbc, time);
        user.set(users.staff(Role.NURSING_HOME_MANAGER).user());
        var catalog = catalogs.createTaskType(
                "Loại concurrency " + residentId,
                new TaskTypeAttributes(
                        "OTHER",
                        false,
                        "PERFORMED",
                        Role.CAREGIVER,
                        "NORMAL",
                        null,
                        null,
                        false,
                        null,
                        null,
                        null,
                        Set.of(),
                        null));
        var nurse = users.staff(Role.NURSING_STAFF);
        user.set(nurse.user());
        residents.resident(residentId, "[Họ tên kiểm thử]", Gender.MALE, ResidentStatus.IN_RESIDENCE);
        assignments.assign(
                nurse.staffId(),
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        null,
                        null,
                        residentId,
                        null,
                        time.now().minus(Duration.ofDays(1)),
                        null));
        var target = CarePlanService.residentTarget(residentId, 0);
        var plan = service.create(target, residentId, false);
        service.replaceItems(
                target,
                plan.id(),
                "\"0\"",
                List.of(new CarePlanItem(0, plan.id(), null, 0, data(catalog.id(), "Nội dung trước"))));
        var item = store.items(plan.id()).getFirst();
        var written = new CountDownLatch(1);
        var snapshot = new CountDownLatch(1);
        var commit = new CountDownLatch(1);
        var transaction = new TransactionTemplate(manager);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var update = executor.submit(() -> transaction.executeWithoutResult(tx -> {
                service.replaceItems(
                        target,
                        plan.id(),
                        "\"1\"",
                        List.of(new CarePlanItem(item.id(), plan.id(), null, 0, data(catalog.id(), "Nội dung mới"))));
                written.countDown();
                await(commit);
            }));
            await(written);
            var submit = executor.submit(() -> transaction.executeWithoutResult(tx -> {
                store.require(plan.id()); // Snapshot cũ trong transaction gọi, giống đọc parent trước khi chờ khóa.
                snapshot.countDown();
                service.submit(target, plan.id(), time.today().plusDays(1), null);
            }));
            await(snapshot);
            commit.countDown();
            update.get(25, TimeUnit.SECONDS);
            submit.get(25, TimeUnit.SECONDS);
            assertThat(store.require(plan.id()).status()).isEqualTo(CarePlanStatus.PENDING_APPROVAL);
            assertThat(store.items(plan.id()).getFirst().data().content()).isEqualTo("Nội dung mới");
        } finally {
            commit.countDown();
            try (var connection = adminConnection();
                    var statement = connection.createStatement()) {
                statement.executeUpdate("UPDATE care_plan SET status='DRAFT' WHERE id=" + plan.id());
                statement.executeUpdate("DELETE FROM care_plan_item WHERE care_plan_id=" + plan.id());
                statement.executeUpdate("DELETE FROM care_plan WHERE id=" + plan.id());
                statement.executeUpdate("DELETE FROM catalog_item WHERE id=" + catalog.id());
                statement.executeUpdate(
                        "DELETE FROM resident_module_state WHERE resident_id=" + residentId + " AND module='M04'");
            }
        }
    }

    @Test
    void FR_038_replaceUsesCurrentCatalogAfterWaitingForResidentLock() throws Exception {
        long residentId = TestUsers.nextId();
        var users = new TestUsers(jdbc, time);
        var nurse = users.staff(Role.NURSING_STAFF, Role.NURSING_HOME_MANAGER);
        user.set(nurse.user());
        var catalog = catalogs.createTaskType(
                "Loại catalog race " + residentId,
                new TaskTypeAttributes(
                        "OTHER",
                        false,
                        "PERFORMED",
                        Role.CAREGIVER,
                        "NORMAL",
                        null,
                        null,
                        false,
                        null,
                        null,
                        null,
                        Set.of(),
                        null));
        residents.resident(residentId, "[Họ tên kiểm thử]", Gender.MALE, ResidentStatus.IN_RESIDENCE);
        assignments.assign(
                nurse.staffId(),
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        null,
                        null,
                        residentId,
                        null,
                        time.now().minus(Duration.ofDays(1)),
                        null));
        var target = CarePlanService.residentTarget(residentId, 0);
        var plan = service.create(target, residentId, false);
        var written = new CountDownLatch(1);
        var snapshot = new CountDownLatch(1);
        var commit = new CountDownLatch(1);
        var resume = new CountDownLatch(1);
        org.mockito.Mockito.doAnswer(invocation -> {
                    if (Thread.currentThread().getName().equals("care-catalog-reader")) {
                        snapshot.countDown();
                        await(resume);
                    }
                    return invocation.callRealMethod();
                })
                .when(locks)
                .lock(org.mockito.ArgumentMatchers.anyCollection());
        var transaction = new TransactionTemplate(manager);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var deactivate = executor.submit(() -> transaction.executeWithoutResult(tx -> {
                locks.lock(List.of(residentId));
                catalogs.deactivateTaskType(
                        catalog.id(), new com.eldercare.backend.common.Reason("Ngừng loại kiểm thử"));
                written.countDown();
                await(commit);
            }));
            await(written);
            var replace = executor.submit(() -> {
                Thread.currentThread().setName("care-catalog-reader");
                service.replaceItems(
                        target,
                        plan.id(),
                        "\"0\"",
                        List.of(new CarePlanItem(
                                0, plan.id(), null, 0, data(catalog.id(), "Không được lưu loại đã ngừng"))));
            });
            await(snapshot);
            commit.countDown();
            deactivate.get(25, TimeUnit.SECONDS);
            resume.countDown();
            assertThatThrownBy(() -> replace.get(25, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(com.eldercare.backend.common.error.InvalidValueException.class);
            assertThat(store.items(plan.id())).isEmpty();
        } finally {
            commit.countDown();
            resume.countDown();
            try (var connection = adminConnection();
                    var statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM care_plan_item WHERE care_plan_id=" + plan.id());
                statement.executeUpdate("DELETE FROM care_plan WHERE id=" + plan.id());
                statement.executeUpdate("DELETE FROM catalog_item WHERE id=" + catalog.id());
                statement.executeUpdate(
                        "DELETE FROM resident_module_state WHERE resident_id=" + residentId + " AND module='M04'");
            }
        }
    }

    @Test
    void FR_003_createChecksResidentStateInsideTheResidentLock() throws Exception {
        long residentId = TestUsers.nextId();
        var nurse = new TestUsers(jdbc, time).staff(Role.NURSING_STAFF);
        user.set(nurse.user());
        residents.resident(residentId, "[Họ tên kiểm thử]", Gender.MALE, ResidentStatus.IN_RESIDENCE);
        assignments.assign(
                nurse.staffId(),
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        null,
                        null,
                        residentId,
                        null,
                        time.now().minus(Duration.ofDays(1)),
                        null));
        // Đổi nguồn ngay tại ranh giới vào khóa để mô phỏng trạng thái đổi khi lệnh đang chờ.
        org.mockito.Mockito.doAnswer(invocation -> {
                    residents.changeStatus(residentId, ResidentStatus.RESIDENCY_ENDED, time.now());
                    return invocation.callRealMethod();
                })
                .when(locks)
                .lock(org.mockito.ArgumentMatchers.eq(List.of(residentId)));
        try {
            assertThatThrownBy(() -> service.create(CarePlanService.residentTarget(residentId, 0), residentId, false))
                    .isInstanceOf(com.eldercare.backend.common.error.BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo("RESIDENT_FINAL_STATUS");
        } finally {
            try (var connection = adminConnection();
                    var statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM care_plan WHERE resident_id=" + residentId);
                statement.executeUpdate(
                        "DELETE FROM resident_module_state WHERE resident_id=" + residentId + " AND module='M04'");
            }
        }
    }
}
