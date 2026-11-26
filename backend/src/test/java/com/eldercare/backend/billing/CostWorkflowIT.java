package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.billing.internal.item.domain.CostItemSelection;
import com.eldercare.backend.billing.internal.item.service.CostItemService;
import com.eldercare.backend.billing.internal.period.service.CostStatementService;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.*;
import org.springframework.transaction.annotation.Transactional;

/** T016: trạng thái/quyền thật, MySQL thật; không lập nguồn hoặc khoản bằng job. */
@Transactional
@Import(CostWorkflowIT.Users.class)
@RecordApplicationEvents
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
class CostWorkflowIT extends MySqlIntegrationTestBase {
    @TestConfiguration(proxyBeanMethods = false)
    static class Users {
        @Bean
        @Primary
        SwitchableUser costWorkflowUser() {
            return new SwitchableUser();
        }
    }

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser current;

    @Autowired
    CostItemService items;

    @Autowired
    CostStatementService statements;

    @Autowired
    ApplicationEvents events;

    @Autowired
    org.springframework.test.web.servlet.MockMvc mvc;

    @Autowired
    org.springframework.transaction.PlatformTransactionManager transactions;

    @Autowired
    com.eldercare.backend.billing.internal.period.service.CostLock costLock;

    @Autowired
    com.eldercare.backend.billing.internal.period.service.CostDailyTask calendar;

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    AuditLogApi audit;

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.eldercare.backend.resident.api.ResidentApi residentApi;

    TestUsers.TestUser office, manager;
    long period, statement, resident;
    YearMonth billedMonth;

    @BeforeEach
    void fixture() {
        // Fixture chỉ tạo bảng/khoản 010, không tạo hồ sơ 001. Không quét hồ sơ sót của IT khác.
        org.mockito.Mockito.doReturn(List.of()).when(residentApi).byStatus(org.mockito.ArgumentMatchers.anySet());
        var users = new TestUsers(jdbc, time);
        office = users.staff(Role.OFFICE_STAFF);
        manager = users.staff(Role.NURSING_HOME_MANAGER);
        current.set(office.user());
        resident = TestUsers.nextId();
        period = TestUsers.nextId();
        statement = TestUsers.nextId();
        long seed = Math.floorMod(resident, 12000);
        var month = YearMonth.of(1000 + (int) seed / 12, 1 + (int) seed % 12);
        billedMonth = month;
        var at = Timestamp.from(time.now());
        jdbc.update("""
                INSERT INTO cost_statement(id,kind,period_month,period_starts_on,period_ends_on,
                  status,unique_key,created_at)
                VALUES (?,'PERIOD',?,?,?,'AWAITING_FINALIZATION',?,?)
                """, period, month.toString(), month.atDay(1), month.atEndOfMonth(), "PERIOD:" + month, at);
        jdbc.update("""
                INSERT INTO cost_statement(id,kind,period_id,resident_id,statement_type,unique_key,created_at)
                VALUES (?,'STATEMENT',?,?,'REGULAR',?,?)
                """, statement, period, resident, "STATEMENT:" + period + ":" + resident + ":REGULAR", at);
    }

    long item(String origin, Long amount, String flags) {
        long id = TestUsers.nextId();
        String source = "source-" + id;
        jdbc.update(
                """
                INSERT INTO cost_item(id,resident_id,statement_id,period_id,cost_type,origin,
                  source_type,source_id,billing_unit,description,incurred_on,quantity,unit,unit_price,
                  amount,flags,unique_key,reason,created_at,created_by)
                VALUES (?,?,?,?,'SERVICE',?,?,?,?, 'Dịch vụ kiểm thử',?,1,'UNIT',?,?,?,?, 'Kiểm thử',?,?)
                """,
                id,
                resident,
                statement,
                period,
                origin,
                origin.equals("EVENT_BASED") ? "CARE_TASK_RESULT" : null,
                origin.equals("EVENT_BASED") ? source : null,
                origin.equals("EVENT_BASED") ? "UNIT" : null,
                billedMonth.atDay(1),
                amount,
                amount,
                flags,
                origin.equals("EVENT_BASED") ? "CARE_TASK_RESULT:" + source + ":UNIT" : null,
                Timestamp.from(time.now()),
                office.accountId());
        return id;
    }

    CostItemSelection selection(long... ids) {
        return new CostItemSelection(Arrays.stream(ids).boxed().toList(), null);
    }

    String status(String table, long id) {
        return jdbc.queryForObject("SELECT status FROM " + table + " WHERE id=?", String.class, id);
    }

    void approve(long id) {
        items.check(selection(id));
        current.set(manager.user());
        assertThat(items.approve(selection(id), null).succeededIds()).containsExactly(id);
        current.set(office.user());
    }

    @Test
    void FR_015_returningAStatementDoesNotAcceptItemsFromAnotherStatement() {
        long id = item("EVENT_BASED", 300L, "");
        items.check(selection(id));
        statements.submit(statement);
        current.set(manager.user());
        assertThatThrownBy(() ->
                        statements.returnStatement(statement, List.of(TestUsers.nextId()), new Reason("Đối chiếu lại")))
                .isInstanceOf(BusinessException.class);
        assertThat(status("cost_statement", statement)).isEqualTo("AWAITING_FINALIZATION");
        assertThat(status("cost_item", id)).isEqualTo("CHECKED");
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void FR_033_aConcurrentDraftCannotBeHiddenByAnOlderTransactionSnapshot() throws Exception {
        long id = item("EVENT_BASED", 300L, "");
        approve(id);
        statements.submit(statement);
        current.set(manager.user());
        int version = statements.read(statement).version();
        var inserted = new java.util.concurrent.CountDownLatch(1);
        var oldSnapshot = new java.util.concurrent.CountDownLatch(1);
        var committed = new java.util.concurrent.CountDownLatch(1);
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var insertion = pool.submit(() -> {
                new org.springframework.transaction.support.TransactionTemplate(transactions)
                        .executeWithoutResult(tx -> {
                            costLock.statement(resident, statement);
                            item("EVENT_BASED", 100L, "");
                            inserted.countDown();
                            await(oldSnapshot);
                        });
                committed.countDown();
            });
            assertThat(inserted.await(15, java.util.concurrent.TimeUnit.SECONDS))
                    .isTrue();
            var finalization = pool.submit(() -> new org.springframework.transaction.support.TransactionTemplate(
                            transactions)
                    .execute(tx -> {
                        jdbc.queryForObject("SELECT version FROM cost_statement WHERE id=?", Integer.class, statement);
                        oldSnapshot.countDown();
                        await(committed);
                        statements.finalizeStatement(statement, "\"" + version + "\"");
                        return true;
                    }));
            insertion.get(20, java.util.concurrent.TimeUnit.SECONDS);
            assertThatThrownBy(() -> finalization.get(20, java.util.concurrent.TimeUnit.SECONDS))
                    .rootCause()
                    .isInstanceOf(VersionMismatchException.class);
        }
        assertThat(status("cost_statement", statement)).isEqualTo("OPEN");
        assertThat(status("cost_item", id)).isEqualTo("APPROVED");
    }

    // Demo 2026-11-25 (E, chờ chủ dự án duyệt): bảng kỳ cuối do Hành chính chốt luôn khi gửi chốt; khoản nhập tay, mua
    // hộ, điều chỉnh vẫn phải được Quản lý viện duyệt riêng trước (BR-M11-04 giữ nguyên).
    void finalPeriod() {
        jdbc.update("UPDATE cost_statement SET final_period=1 WHERE id=?", statement);
    }

    @Test
    void finalPeriod_submitByOfficeFinalizesDirectly_whenOnlyEventItemsChecked() {
        finalPeriod();
        long id = item("EVENT_BASED", 300L, "");
        items.check(selection(id));
        statements.submit(statement);
        assertThat(status("cost_statement", statement)).isEqualTo("FINALIZED");
        assertThat(status("cost_item", id)).isEqualTo("FINALIZED");
        assertThat(jdbc.queryForObject("SELECT total FROM cost_statement WHERE id=?", Long.class, statement))
                .isEqualTo(300L);
    }

    // Demo 2026-11-25: thông báo chốt bảng tới người thân nêu kỳ, tổng tiền và việc đã trừ vào số dư (017 US4)
    @Test
    void finalize_familyNotificationStatesPeriodTotalAndBalanceDeduction() {
        finalPeriod();
        long a = item("EVENT_BASED", 1_250_000L, "");
        long b = item("EVENT_BASED", 300_000L, "");
        items.check(selection(a, b));
        statements.submit(statement);
        var event = events.stream(com.eldercare.backend.billing.api.events.CostStatementFinalized.class)
                .filter(e -> e.statementId() == statement)
                .findFirst()
                .orElseThrow();
        var content =
                event.notificationRequests().getFirst().content().getFirst().content();
        assertThat(content)
                .isEqualTo("Bảng chi phí kỳ cuối tháng %02d/%d đã chốt: tổng 1.550.000 đ, đã trừ vào số dư."
                        .formatted(billedMonth.getMonthValue(), billedMonth.getYear()));
    }

    @Test
    void finalPeriod_manualItemNotApproved_staysAwaitingManager() {
        finalPeriod();
        long event = item("EVENT_BASED", 300L, "");
        long manual = item("MANUAL", 50L, "");
        items.check(selection(event, manual));
        statements.submit(statement);
        assertThat(status("cost_statement", statement)).isEqualTo("AWAITING_FINALIZATION");
        assertThat(status("cost_item", event)).isEqualTo("CHECKED");
        assertThat(status("cost_item", manual)).isEqualTo("CHECKED");
    }

    @Test
    void finalPeriod_manualItemApprovedByManager_thenOfficeSubmitFinalizes() {
        finalPeriod();
        long event = item("EVENT_BASED", 300L, "");
        long manual = item("MANUAL", 50L, "");
        items.check(selection(event));
        approve(manual);
        statements.submit(statement);
        assertThat(status("cost_statement", statement)).isEqualTo("FINALIZED");
        assertThat(status("cost_item", event)).isEqualTo("FINALIZED");
        assertThat(status("cost_item", manual)).isEqualTo("FINALIZED");
    }

    // Demo 2026-11-26 (E, chờ chủ dự án duyệt): đổi ngày kết thúc sau khi đã chốt bảng kỳ cuối thì ngày lưu trú gom
    // thêm
    // thành khoản bổ sung muộn ở bảng bổ sung; bảng này cũng do Hành chính chốt luôn như bảng kỳ cuối, để không chặn
    // quyết
    // toán. Điều chỉnh tay (trỏ về khoản gốc) vẫn chờ Quản lý viện.
    long supplementary() {
        long id = TestUsers.nextId();
        jdbc.update(
                """
                INSERT INTO cost_statement(id,kind,period_id,resident_id,statement_type,unique_key,created_at)
                VALUES (?,'STATEMENT',?,?,'SUPPLEMENTARY',?,?)
                """,
                id,
                period,
                resident,
                "STATEMENT:" + period + ":" + resident + ":SUPPLEMENTARY",
                Timestamp.from(time.now()));
        return id;
    }

    long lateAddition(long statementId, boolean fromSource) {
        long id = TestUsers.nextId();
        String source = "source-" + id;
        jdbc.update(
                """
                INSERT INTO cost_item(id,resident_id,statement_id,period_id,cost_type,origin,
                  source_type,source_id,billing_unit,description,incurred_on,quantity,unit,unit_price,
                  amount,flags,original_item_id,original_period_id,adjustment_type,reason,created_at,created_by)
                VALUES (?,?,?,?,'RESIDENCY_FEE','ADJUSTMENT',?,?,?,'Phí lưu trú',?,1,'ngày',83333,83333,
                  'AFTER_FINALIZED_PERIOD,AFFECTS_TERMINATION',?,?,'LATE_ADDITION','Kiểm thử',?,?)
                """,
                id,
                resident,
                statementId,
                period,
                fromSource ? "RESIDENCY_DAY" : null,
                fromSource ? source : null,
                fromSource ? "DAY" : null,
                billedMonth.atDay(2),
                fromSource ? null : item("EVENT_BASED", 83333L, ""),
                period,
                Timestamp.from(time.now()),
                office.accountId());
        return id;
    }

    @Test
    void terminationSupplement_lateAdditionsFromSources_officeSubmitFinalizesDirectly() {
        long supp = supplementary();
        long a = lateAddition(supp, true);
        long b = lateAddition(supp, true);
        items.check(selection(a, b));
        statements.submit(supp);
        assertThat(status("cost_statement", supp)).isEqualTo("FINALIZED");
        assertThat(status("cost_item", a)).isEqualTo("FINALIZED");
        assertThat(status("cost_item", b)).isEqualTo("FINALIZED");
    }

    @Test
    void terminationSupplement_manualAdjustment_staysAwaitingManager() {
        long supp = supplementary();
        long manual = lateAddition(supp, false);
        items.check(selection(manual));
        statements.submit(supp);
        assertThat(status("cost_statement", supp)).isEqualTo("AWAITING_FINALIZATION");
    }

    // Demo 2026-11-26 (E): bảng kỳ cuối chốt thì Kế toán được báo việc cần làm (hoàn tiền hoặc ghi nộp, rồi xác nhận
    // quyết
    // toán); nguồn là số dư của người cao tuổi để nút Xử lý ở web mở thẳng số dư
    @Test
    void finalPeriod_finalized_accountantToldWhatToDoNext_sourceIsTheBalance() {
        org.mockito.Mockito.doReturn(new com.eldercare.backend.resident.api.dto.IdentityView(
                        resident, "NCT-T", "Lê Văn Thử", java.util.Optional.empty()))
                .when(residentApi)
                .identity(resident);
        finalPeriod();
        long id = item("EVENT_BASED", 2_083_325L, "");
        items.check(selection(id));
        statements.submit(statement);

        var request = events.stream(com.eldercare.backend.common.notification.HasNotificationRequests.class)
                .flatMap(e -> e.notificationRequests().stream())
                .filter(r -> r.eventType().equals("COST_FINAL_STATEMENT_FOR_ACCOUNTANT"))
                .filter(r -> r.residentId().orElse(0L) == resident)
                .findFirst()
                .orElseThrow();
        assertThat(request.recipientGroups())
                .containsExactly(
                        new com.eldercare.backend.common.notification.RecipientGroup.FacilityRole(Role.ACCOUNTANT));
        assertThat(request.sourceRecord().type()).isEqualTo("M11.BALANCE");
        assertThat(request.sourceRecord().id()).isEqualTo(Long.toString(resident));
        assertThat(request.content().getFirst().content())
                .contains("Lê Văn Thử")
                .contains("2.083.325 đ")
                .contains("xác nhận quyết toán");
    }

    @Test
    void regularStatement_submitStillWaitsForManager() {
        long id = item("EVENT_BASED", 300L, "");
        items.check(selection(id));
        statements.submit(statement);
        assertThat(status("cost_statement", statement)).isEqualTo("AWAITING_FINALIZATION");
    }

    private void await(java.util.concurrent.CountDownLatch latch) {
        try {
            if (!latch.await(15, java.util.concurrent.TimeUnit.SECONDS))
                throw new IllegalStateException("Hết thời gian chờ transaction");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
