package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.billing.api.events.CostStatementFinalized;
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
    void FR_015_batchCheckingSkipsUnpricedItemsAndRecordsTheChecker() {
        long priced = item("EVENT_BASED", 300L, ""), missing = item("EVENT_BASED", null, "MISSING_UNIT_PRICE");
        var result = items.check(selection(priced, missing));
        assertThat(result.succeededIds()).containsExactly(priced);
        assertThat(result.skipped()).extracting(s -> s.itemId()).containsExactly(missing);
        assertThat(status("cost_item", priced)).isEqualTo("CHECKED");
        assertThat(status("cost_item", missing)).isEqualTo("DRAFT");
        assertThat(jdbc.queryForObject("SELECT checked_by FROM cost_item WHERE id=?", Long.class, priced))
                .isEqualTo(office.accountId());
    }

    @Test
    void FR_015_bulkApprovalLeavesManualItemsForIndividualReview() {
        long event = item("EVENT_BASED", 300L, ""), manual = item("MANUAL", 500L, "");
        items.check(selection(event, manual));
        current.set(manager.user());
        var result = items.approve(selection(event, manual), null);
        assertThat(result.succeededIds()).containsExactly(event);
        assertThat(result.skipped()).extracting(s -> s.itemId()).containsExactly(manual);
        assertThat(items.approve(selection(manual), null).succeededIds()).containsExactly(manual);
    }

    @Test
    void BR_M11_06_submissionListsDraftsAndFinalizationListsUncheckedApprovals() {
        long id = item("EVENT_BASED", 300L, "");
        assertThatThrownBy(() -> statements.submit(statement))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).errorCode())
                .isEqualTo("DRAFT_ITEMS_REMAIN");
        items.check(selection(id));
        statements.submit(statement);
        current.set(manager.user());
        var snapshot = statements.read(statement);
        assertThatThrownBy(() -> statements.finalizeStatement(statement, "\"" + snapshot.version() + "\""))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).errorCode())
                .isEqualTo("UNAPPROVED_ITEMS_REMAIN");
        assertThat(status("cost_statement", statement)).isEqualTo("AWAITING_FINALIZATION");
    }

    @Test
    void FR_033_finalizationStoresTotalsLocksItemsAndEmitsOneEvent() {
        long id = item("EVENT_BASED", 300L, "");
        approve(id);
        statements.submit(statement);
        current.set(manager.user());
        int version = statements.read(statement).version();
        statements.finalizeStatement(statement, "\"" + version + "\"");
        assertThat(status("cost_item", id)).isEqualTo("FINALIZED");
        assertThat(status("cost_statement", statement)).isEqualTo("FINALIZED");
        assertThat(jdbc.queryForObject("SELECT total FROM cost_statement WHERE id=?", Long.class, statement))
                .isEqualTo(300L);
        assertThat(events.stream(CostStatementFinalized.class)
                        .filter(e -> e.statementId() == statement)
                        .count())
                .isOne();
        assertThatThrownBy(() -> statements.finalizeStatement(statement, "\"" + version + "\""))
                .isInstanceOf(BusinessException.class);
        assertThat(events.stream(CostStatementFinalized.class)
                        .filter(e -> e.statementId() == statement)
                        .count())
                .isOne();
        assertThatThrownBy(() -> jdbc.update("UPDATE cost_item SET description='Thay đổi' WHERE id=?", id))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test
    void FR_033_anyItemChangeInvalidatesTheStatementEtag() {
        long id = item("EVENT_BASED", 300L, "");
        items.check(selection(id));
        statements.submit(statement);
        current.set(manager.user());
        int version = statements.read(statement).version();
        items.approve(selection(id), null);
        assertThatThrownBy(() -> statements.finalizeStatement(statement, "\"" + version + "\""))
                .isInstanceOf(VersionMismatchException.class);
        assertThat(status("cost_item", id)).isEqualTo("APPROVED");
    }

    @Test
    void FR_004_returningAnItemReopensTheStatementAndRequiresAReason() {
        long id = item("EVENT_BASED", 300L, "");
        items.check(selection(id));
        statements.submit(statement);
        current.set(manager.user());
        items.returnItem(id, new Reason("Cần đối chiếu lại nguồn"));
        assertThat(status("cost_statement", statement)).isEqualTo("OPEN");
        assertThat(status("cost_item", id)).isEqualTo("DRAFT");
    }

    @Test
    void FR_004_aNewDraftReopensTheStatementAndInvalidatesItsEtag() {
        long id = item("EVENT_BASED", 300L, "");
        approve(id);
        statements.submit(statement);
        int version = statements.read(statement).version();
        item("EVENT_BASED", 100L, "");
        assertThat(status("cost_statement", statement)).isEqualTo("OPEN");
        assertThat(statements.read(statement).version()).isGreaterThan(version);
    }

    @Test
    void FR_015_uncheckingIsBlockedWhileAwaitingFinalization() {
        long id = item("EVENT_BASED", 300L, "");
        items.check(selection(id));
        statements.submit(statement);
        assertThatThrownBy(() -> items.uncheck(id, new Reason("Đối chiếu lại"))).isInstanceOf(BusinessException.class);
        assertThat(status("cost_item", id)).isEqualTo("CHECKED");
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
    void FR_033_aFailureAfterUpdatingTotalsRollsBackEveryItemAndTheStatement() {
        long id = item("EVENT_BASED", 300L, "");
        approve(id);
        statements.submit(statement);
        current.set(manager.user());
        int version = statements.read(statement).version();
        AuditLogApi auditTarget = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(audit);
        doThrow(new IllegalStateException("Giả lập lỗi nhật ký trong transaction"))
                .when(auditTarget)
                .write(argThat(e -> e.action().code().equals("COST_STATEMENT_FINALIZED")));
        assertThatThrownBy(() -> statements.finalizeStatement(statement, "\"" + version + "\""))
                .isInstanceOf(IllegalStateException.class);
        assertThat(status("cost_item", id)).isEqualTo("APPROVED");
        assertThat(status("cost_statement", statement)).isEqualTo("AWAITING_FINALIZATION");
        assertThat(jdbc.queryForObject("SELECT total FROM cost_statement WHERE id=?", Long.class, statement))
                .isNull();
        assertThat(events.stream(CostStatementFinalized.class)
                        .filter(e -> e.statementId() == statement)
                        .count())
                .isZero();
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

    private void await(java.util.concurrent.CountDownLatch latch) {
        try {
            if (!latch.await(15, java.util.concurrent.TimeUnit.SECONDS))
                throw new IllegalStateException("Hết thời gian chờ transaction");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    @Test
    void FR_017_rolesCannotPerformEachOthersCommands() {
        long id = item("EVENT_BASED", 300L, "");
        assertThatThrownBy(() -> items.approve(selection(id), null)).isInstanceOf(PermissionDeniedException.class);
        current.set(manager.user());
        assertThatThrownBy(() -> items.check(selection(id))).isInstanceOf(PermissionDeniedException.class);
        assertThatThrownBy(() -> statements.submit(statement)).isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void FR_017_theRecordedCheckerCannotApproveEvenAfterReceivingAManagerRole() {
        long id = item("EVENT_BASED", 300L, "");
        var dualRole = new TestUsers(jdbc, time).staff(Role.OFFICE_STAFF, Role.NURSING_HOME_MANAGER);
        jdbc.update(
                "UPDATE cost_item SET status='CHECKED',checked_by=?,checked_at=? WHERE id=?",
                dualRole.accountId(),
                Timestamp.from(time.now()),
                id);
        current.set(dualRole.user());
        var result = items.approve(selection(id), null);
        assertThat(result.succeededIds()).isEmpty();
        assertThat(result.skipped()).extracting(s -> s.errorCode()).containsExactly("PERMISSION_DENIED");
        assertThat(status("cost_item", id)).isEqualTo("CHECKED");
    }

    @Test
    void FR_024_approvingAnExcessPurchaseRequiresAnExplicitReason() {
        long id = item("MANUAL", 600_000L, "");
        jdbc.update(
                "UPDATE cost_item SET origin='PURCHASE_ASSISTANCE',purchase_assistance_id=?,flags='URGENT_EXCESS' WHERE id=?",
                TestUsers.nextId(),
                id);
        items.check(selection(id));
        current.set(manager.user());
        var rejected = items.approve(selection(id), null);
        assertThat(rejected.succeededIds()).isEmpty();
        assertThat(rejected.skipped())
                .extracting(s -> s.errorCode())
                .containsExactly("EXCESS_APPROVAL_REASON_REQUIRED");
        assertThat(items.approve(selection(id), "Chấp nhận phần vượt có chứng từ")
                        .succeededIds())
                .containsExactly(id);
        assertThat(jdbc.queryForObject("SELECT excess_approval_reason FROM cost_item WHERE id=?", String.class, id))
                .isEqualTo("Chấp nhận phần vượt có chứng từ");
    }

    @Test
    void FR_016_onlyTheCreatorCanCancelADraftAndTheSourceCanBeUsedAgain() {
        long id = item("EVENT_BASED", 300L, "");
        items.cancel(id, new Reason("Chọn nhầm nguồn"));
        assertThat(status("cost_item", id)).isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject("SELECT unique_key FROM cost_item WHERE id=?", String.class, id))
                .isNull();
        assertThat(jdbc.queryForObject("SELECT locked_at FROM cost_item WHERE id=?", Timestamp.class, id))
                .isNotNull();
    }

    @Test
    void FR_015_httpCheckingAndReadingUseTheApprovedFieldsAndStatementEtag() throws Exception {
        long id = item("EVENT_BASED", 300L, "");
        String bearer = TestUsers.bearer(new TestUsers(jdbc, time).session(office.accountId()));
        mvc.perform(post("/api/v1/costs/items/check")
                        .header("Authorization", bearer)
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType("application/json")
                        .content("{\"itemIds\":[" + id + "]}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isOk())
                .andExpect(jsonPath("$.succeededIds[0]").value(id));
        mvc.perform(get("/api/v1/costs/statements/{id}", statement).header("Authorization", bearer))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.residentId").value(resident))
                .andExpect(jsonPath("$.items[0].status").value("CHECKED"));
    }

    @Test
    void FR_019_httpHidesMedicationNamesAndOmitsAbsentOptionalFields() throws Exception {
        long id = item("EVENT_BASED", null, "MISSING_UNIT_PRICE");
        jdbc.update("UPDATE cost_item SET cost_type='MEDICATION',description='Tên thuốc không được lộ' WHERE id=?", id);
        String bearer = TestUsers.bearer(new TestUsers(jdbc, time).session(office.accountId()));
        mvc.perform(get("/api/v1/costs/items/{id}", id).header("Authorization", bearer))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isOk())
                .andExpect(jsonPath("$.description").value("Thuốc"))
                .andExpect(jsonPath("$.medicationName").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.unitPrice").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.amount").doesNotHaveJsonPath());
    }

    @Test
    void FR_001_calendarClosesThePeriodAndReleasesAdvanceFlagsWithoutCreatingStatementsOrItems() {
        long id = item("EVENT_BASED", 300L, "CREATED_IN_ADVANCE");
        jdbc.update("UPDATE cost_statement SET status='OPEN' WHERE id=?", period);
        int statementsBefore =
                jdbc.queryForObject("SELECT COUNT(*) FROM cost_statement WHERE kind='STATEMENT'", Integer.class);
        int itemsBefore = jdbc.queryForObject("SELECT COUNT(*) FROM cost_item", Integer.class);
        calendar.run(time.now());
        assertThat(status("cost_statement", period)).isEqualTo("AWAITING_FINALIZATION");
        assertThat(jdbc.queryForObject(
                        "SELECT finalization_due_on FROM cost_statement WHERE id=?", LocalDate.class, period))
                .isNotNull();
        assertThat(jdbc.queryForObject("SELECT flags FROM cost_item WHERE id=?", String.class, id))
                .isEmpty();
        assertThat(jdbc.queryForObject("SELECT amount FROM cost_item WHERE id=?", Long.class, id))
                .isEqualTo(300L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cost_statement WHERE kind='STATEMENT'", Integer.class))
                .isEqualTo(statementsBefore);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cost_item", Integer.class))
                .isEqualTo(itemsBefore);
    }

    @Test
    void FR_001_periodsEndpointUsesThePublishedPeriodView() throws Exception {
        String bearer = TestUsers.bearer(new TestUsers(jdbc, time).session(office.accountId()));
        mvc.perform(get("/api/v1/costs/periods").header("Authorization", bearer))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isOk())
                .andExpect(jsonPath("$[*].month").value(org.hamcrest.Matchers.hasItem(billedMonth.toString())));
    }

    @Test
    void FR_018_itemHistoryRetainsEachCheckerApproverAndReturnReason() throws Exception {
        long id = item("MANUAL", 300L, "");
        items.check(selection(id));
        current.set(manager.user());
        items.approve(selection(id), null);
        items.returnItem(id, new Reason("Cần kiểm tra lại chứng từ"));
        current.set(office.user());
        String bearer = TestUsers.bearer(new TestUsers(jdbc, time).session(office.accountId()));
        mvc.perform(get("/api/v1/costs/items/{id}", id).header("Authorization", bearer))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isOk())
                .andExpect(jsonPath("$.commandHistory[0].operation").value("CHECK"))
                .andExpect(jsonPath("$.commandHistory[0].actorId").value(office.accountId()))
                .andExpect(jsonPath("$.commandHistory[1].operation").value("APPROVE"))
                .andExpect(jsonPath("$.commandHistory[2].operation").value("RETURN"))
                .andExpect(jsonPath("$.commandHistory[2].reason").value("Cần kiểm tra lại chứng từ"));
    }
}
