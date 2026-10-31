package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.roombedallocation.internal.daycare.service.DayCareConfigurationService;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.roombedallocation.FakeDayCareArrivals;
import java.time.LocalTime;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

/** Khóa thật giữa hai transaction cấu hình đồng thời, không chỉ kiểm overlap tuần tự. */
class FR_038_DayCareConcurrencyIT extends StructureTestSupport {
    @Autowired
    DayCareConfigurationService service;

    @Autowired
    FakeDayCareArrivals arrivals;

    @Autowired
    com.eldercare.backend.roombedallocation.api.DayCareCapacityApi capacity;

    @Autowired
    org.springframework.transaction.PlatformTransactionManager transactions;

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.eldercare.backend.administration.parameter.api.ParameterApi parameters;

    long rest;
    String prefix;

    @BeforeEach
    void dayCareFixture() {
        arrivals.reset();
        rest = TestUsers.nextId();
        prefix = "Concurrent " + rest;
        jdbc.update(
                "INSERT INTO day_care_rest_area(id,code,name,floor_id) VALUES(?,?,?,?)",
                rest,
                "D" + rest,
                prefix,
                floor);
    }

    @AfterEach
    void cleanup() {
        jdbc.update("UPDATE day_care_session SET status='INACTIVE' WHERE name LIKE ?", prefix + "%");
        jdbc.update("UPDATE day_care_rest_area SET status='INACTIVE' WHERE id=?", rest);
    }

    @Test
    void FR_038_onlyOneConcurrentOverlappingSessionCommits() throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> create(prefix + " A", ready, start));
            var second = executor.submit(() -> create(prefix + " B", ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(java.util.List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("CREATED", "DAY_CARE_SESSION_OVERLAP");
        } finally {
            start.countDown();
        }
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM day_care_session WHERE status='ACTIVE' AND name LIKE ?",
                        Integer.class,
                        prefix + "%"))
                .isEqualTo(1);
    }

    @Test
    void FR_005_activeRestAreaPreventsParentFloorDeactivation() {
        long emptyFloor = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO floor(id,area_id,code,name) VALUES(?,?,?,?)",
                emptyFloor,
                area,
                "F" + emptyFloor,
                "Tầng trống " + emptyFloor);
        jdbc.update("UPDATE day_care_rest_area SET floor_id=? WHERE id=?", emptyFloor, rest);
        assertThatThrownBy(() -> structures.deactivate(
                        com.eldercare.backend.roombedallocation.internal.structure.domain.StructureKind.FLOOR,
                        emptyFloor,
                        "\"0\"",
                        new com.eldercare.backend.common.Reason("Ngừng tầng")))
                .isInstanceOf(com.eldercare.backend.common.error.ConditionNotMetException.class);
    }

    @Test
    void FR_005_movingRestAreaAndDeactivatingItsTargetCannotBothCommit() throws Exception {
        long emptyFloor = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO floor(id,area_id,code,name) VALUES(?,?,?,?)",
                emptyFloor,
                area,
                "F" + emptyFloor,
                "Tầng trống " + emptyFloor);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var move = executor.submit(() -> {
                start.await();
                try {
                    service.updateArea(rest, "\"0\"", "D" + rest, prefix, emptyFloor, null);
                    return true;
                } catch (BusinessException e) {
                    return false;
                }
            });
            var deactivate = executor.submit(() -> {
                start.await();
                try {
                    structures.deactivate(
                            com.eldercare.backend.roombedallocation.internal.structure.domain.StructureKind.FLOOR,
                            emptyFloor,
                            "\"0\"",
                            new com.eldercare.backend.common.Reason("Ngừng tầng"));
                    return true;
                } catch (BusinessException e) {
                    return false;
                }
            });
            start.countDown();
            assertThat(java.util.List.of(move.get(20, TimeUnit.SECONDS), deactivate.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        } finally {
            start.countDown();
        }
    }

    @Test
    void FR_039_lockAndOverlapReadCurrentFramesAfterOlderRepeatableReadSnapshot() throws Exception {
        var created = service.createSession(prefix + " A", LocalTime.of(7, 0), LocalTime.NOON)
                .session();
        assertThat(created.startTime()).isEqualTo(LocalTime.of(7, 0));
        org.mockito.Mockito.doReturn(new com.eldercare.backend.administration.parameter.api.dto.ParameterValue(
                        com.eldercare.backend.administration.parameter.api.dto.ParameterCode.CFG_M03_01,
                        com.eldercare.backend.administration.parameter.api.dto.ParameterKind.INTEGER,
                        tools.jackson.databind.json.JsonMapper.builder().build().readTree("3")))
                .when(parameters)
                .read(com.eldercare.backend.administration.parameter.api.dto.ParameterCode.CFG_M03_01);
        var transaction = new org.springframework.transaction.support.TransactionTemplate(transactions);
        transaction.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ);
        try (var executor = Executors.newSingleThreadExecutor()) {
            transaction.executeWithoutResult(status -> {
                jdbc.queryForObject("SELECT COUNT(*) FROM day_care_session", Integer.class);
                var update = executor.submit(() -> service.updateSession(
                        created.id(), "\"0\"", created.name(), LocalTime.of(8, 0), LocalTime.NOON));
                try {
                    update.get(20, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
                assertThat(capacity.lock().sessions())
                        .anySatisfy(session -> assertThat(session.startTime()).isEqualTo(LocalTime.of(8, 0)));
                assertThat(capacity.occupiedSessions(time.today().plusDays(1), LocalTime.of(7, 0), LocalTime.of(8, 0)))
                        .isEmpty();
            });
        }
    }

    private String create(String name, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Chờ bắt đầu quá hạn");
        try {
            service.createSession(name, LocalTime.of(7, 0), LocalTime.NOON);
            return "CREATED";
        } catch (BusinessException e) {
            return e.errorCode();
        }
    }
}
