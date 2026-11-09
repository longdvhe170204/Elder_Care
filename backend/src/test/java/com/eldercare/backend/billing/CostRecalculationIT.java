package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.shared.api.*;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.billing.internal.adjustment.domain.PostFinalizationChange;
import com.eldercare.backend.billing.internal.adjustment.service.CostAdjustmentService;
import com.eldercare.backend.billing.internal.item.domain.CostRecalculation;
import com.eldercare.backend.billing.internal.item.persistence.CostRecalculationStore;
import com.eldercare.backend.billing.internal.item.service.CostRecalculationService;
import com.eldercare.backend.billing.internal.pricing.domain.CalculatedCost;
import com.eldercare.backend.billing.internal.pricing.service.CostPricingService;
import com.eldercare.backend.common.*;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.YearMonth;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

/** T020: ghi khoản, lịch sử, biến động và trigger thật; giá sự kiện được cô lập ở tầng billing. */
@Transactional
@Import(CostWorkflowIT.Users.class)
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
class CostRecalculationIT extends MySqlIntegrationTestBase {
    @Autowired
    org.springframework.test.web.servlet.MockMvc mvc;

    @Autowired
    org.springframework.context.ApplicationEventPublisher publisher;

    @MockitoSpyBean
    com.eldercare.backend.resident.api.ResidentApi residents;

    @Autowired
    com.eldercare.backend.billing.internal.item.service.CostDestination destination;

    @Autowired
    com.eldercare.backend.billing.internal.period.service.CostLock locks;

    TestUsers.TestUser office;

    @MockitoSpyBean
    com.eldercare.backend.billing.internal.item.persistence.CostItemStore itemStore;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser current;

    @Autowired
    CostRecalculationService recalculation;

    @Autowired
    CostRecalculationStore store;

    @Autowired
    CostAdjustmentService adjustments;

    @Autowired
    StatusHistoryApi history;

    @Autowired
    RecordEventApi changes;

    @Autowired
    org.springframework.transaction.PlatformTransactionManager transactions;

    @MockitoSpyBean
    CostPricingService pricing;

    @MockitoSpyBean
    com.eldercare.backend.administration.auditlog.api.AuditLogApi audit;

    long resident, period, statement, item;
    YearMonth month;

    @BeforeEach
    void fixture() {
        office = new TestUsers(jdbc, time).staff(Role.OFFICE_STAFF);
        current.set(office.user());
        resident = TestUsers.nextId();
        period = TestUsers.nextId();
        statement = TestUsers.nextId();
        item = TestUsers.nextId();
        long seed = Math.floorMod(resident, 12000);
        month = YearMonth.of(1000 + (int) seed / 12, 1 + (int) seed % 12);
        var at = Timestamp.from(time.now());
        jdbc.update("""
                INSERT INTO cost_statement(id,kind,period_month,period_starts_on,period_ends_on,status,unique_key,created_at)
                VALUES (?,'PERIOD',?,?,?,'AWAITING_FINALIZATION',?,?)
                """, period, month.toString(), month.atDay(1), month.atEndOfMonth(), "PERIOD:" + month, at);
        jdbc.update("""
                INSERT INTO cost_statement(id,kind,period_id,resident_id,statement_type,unique_key,created_at)
                VALUES (?,'STATEMENT',?,?,'REGULAR',?,?)
                """, statement, period, resident, "STATEMENT:" + period + ":" + resident + ":REGULAR", at);
        jdbc.update(
                """
                INSERT INTO cost_item(id,resident_id,statement_id,period_id,cost_type,origin,source_type,source_id,
                  billing_unit,service_id,description,incurred_on,quantity,unit,unit_price,amount,source_values,
                  unique_key,created_at,created_by)
                VALUES (?,?,?,?,'SERVICE','EVENT_BASED','CARE_TASK_RESULT',?,'UNIT',9,'Dịch vụ kiểm thử',?,
                  1,'UNIT',300,300,'{"quantity":1}',?,?,?)
                """,
                item,
                resident,
                statement,
                period,
                "source-" + item,
                month.atDay(1),
                "CARE_TASK_RESULT:source-" + item + ":UNIT",
                at,
                office.accountId());
    }

    void change(BigDecimal quantity, boolean cancelled, String key) {
        recalculation.sourceChanged(
                "CARE_TASK_RESULT", "source-" + item, "UNIT", quantity, cancelled, "SOURCE_CORRECTED", key);
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void FR_020_publishedSourceCorrectionUpdatesIndexAndCostThenRecordsOnlyAfterFinalization() {
        var transaction = new org.springframework.transaction.support.TransactionTemplate(transactions);
        var sourceId = "source-" + item;
        transaction.executeWithoutResult(
                ignored -> publisher.publishEvent(new com.eldercare.backend.billing.api.events.CostSourceBilled(
                        "CARE_TASK_RESULT",
                        sourceId,
                        "UNIT",
                        resident,
                        Optional.of(9L),
                        BigDecimal.ONE,
                        month.atDay(1),
                        time.now(),
                        "created-" + item)));
        org.awaitility.Awaitility.await()
                .atMost(java.time.Duration.ofSeconds(15))
                .untilAsserted(() -> assertThat(jdbc.queryForObject(
                                "SELECT COUNT(*) FROM cost_source_index WHERE source_type='CARE_TASK_RESULT' AND source_id=? AND billing_unit='UNIT'",
                                Long.class,
                                sourceId))
                        .isEqualTo(1));
        transaction.executeWithoutResult(ignored -> doReturn(
                        new CostPricingService.PriceQuote(new BigDecimal("300"), new BigDecimal("600"), false, null))
                .when(pricing)
                .event(eq(resident), eq(9L), eq(month.atDay(1)), argThat(q -> q.compareTo(new BigDecimal("2")) == 0)));
        transaction.executeWithoutResult(
                ignored -> publisher.publishEvent(new com.eldercare.backend.billing.api.events.CostSourceChanged(
                        "CARE_TASK_RESULT",
                        sourceId,
                        "UNIT",
                        Optional.of(new BigDecimal("2")),
                        false,
                        "Đính chính nguồn",
                        "change-1-" + item)));
        org.awaitility.Awaitility.await()
                .atMost(java.time.Duration.ofSeconds(15))
                .untilAsserted(() -> {
                    assertThat(jdbc.queryForObject("SELECT amount FROM cost_item WHERE id=?", Long.class, item))
                            .isEqualTo(600);
                    assertThat(history.list(CostRecalculation.TRACK, item)).hasSize(1);
                });
        transaction.executeWithoutResult(ignored -> {
            locks.resident(resident);
            jdbc.update(
                    "UPDATE cost_item SET status='FINALIZED',locked_at=? WHERE id=?", Timestamp.from(time.now()), item);
        });
        transaction.executeWithoutResult(
                ignored -> publisher.publishEvent(new com.eldercare.backend.billing.api.events.CostSourceChanged(
                        "CARE_TASK_RESULT",
                        sourceId,
                        "UNIT",
                        Optional.of(new BigDecimal("3")),
                        false,
                        "Đính chính tiếp",
                        "change-2-" + item)));
        org.awaitility.Awaitility.await()
                .atMost(java.time.Duration.ofSeconds(15))
                .untilAsserted(() -> assertThat(
                                changes.list(PostFinalizationChange.KIND, new EntityRef("M11.COST_ITEM", item)))
                        .hasSize(1));
        assertThat(jdbc.queryForObject("SELECT amount FROM cost_item WHERE id=?", Long.class, item))
                .isEqualTo(600);
        assertThat(jdbc.queryForObject(
                        "SELECT current_quantity FROM cost_source_index WHERE source_type='CARE_TASK_RESULT' AND source_id=? AND billing_unit='UNIT'",
                        BigDecimal.class,
                        sourceId))
                .isEqualByComparingTo("3");
        transaction.executeWithoutResult(ignored -> verify(pricing, never())
                .event(eq(resident), anyLong(), any(), argThat(q -> q.compareTo(new BigDecimal("3")) == 0)));
    }

    @Test
    void FR_043_managerCannotCreateAdjustmentThroughHttp() throws Exception {
        var manager = new TestUsers(jdbc, time).staff(Role.NURSING_HOME_MANAGER);
        current.set(manager.user());
        String bearer = TestUsers.bearer(new TestUsers(jdbc, time).session(manager.accountId()));
        mvc.perform(post("/api/v1/costs/items/{id}/adjust", item)
                        .header("Authorization", bearer)
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType("application/json")
                        .content("{\"amount\":100,\"adjustmentType\":\"WAIVER\",\"reason\":\"Điều chỉnh\"}"))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cost_item WHERE original_item_id=?", Long.class, item))
                .isZero();
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void FR_003_adjustmentWaitingForFinalizationUsesCurrentLockedState() throws Exception {
        doReturn(false).when(residents).isFinalStatus(resident);
        var read = new java.util.concurrent.CountDownLatch(1);
        doAnswer(invocation -> {
                    var result = invocation.callRealMethod();
                    read.countDown();
                    return result;
                })
                .when(itemStore)
                .item(item);
        var transaction = new org.springframework.transaction.support.TransactionTemplate(transactions);
        try (var executor = java.util.concurrent.Executors.newSingleThreadExecutor()) {
            var pending = transaction.execute(ignored -> {
                locks.resident(resident);
                var future = executor.submit(
                        () -> adjustments.adjust(item, 100, "SOURCE_CHANGE", Reason.of("Bổ sung"), null));
                try {
                    assertThat(read.await(10, java.util.concurrent.TimeUnit.SECONDS))
                            .isTrue();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ex);
                }
                jdbc.update(
                        "UPDATE cost_item SET status='FINALIZED',locked_at=? WHERE id=?",
                        Timestamp.from(time.now()),
                        item);
                jdbc.update(
                        "UPDATE cost_statement SET status='FINALIZED',total=300,totals_by_type='[]',finalized_at=created_at,locked_at=? WHERE id=?",
                        Timestamp.from(time.now()),
                        statement);
                return future;
            });
            long created = pending.get(20, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(jdbc.queryForObject("SELECT statement_id FROM cost_item WHERE id=?", Long.class, created))
                    .isNotEqualTo(statement);
            assertThat(jdbc.queryForObject("SELECT original_period_id FROM cost_item WHERE id=?", Long.class, created))
                    .isEqualTo(period);
        }
    }

    @Test
    void FR_027_httpDeclaresSourceSnapshotsWithoutSuggestedMoney() throws Exception {
        String bearer = TestUsers.bearer(new TestUsers(jdbc, time).session(office.accountId()));
        mvc.perform(get("/v3/api-docs").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.PostFinalizationChange.properties.reference.type")
                        .value("string"))
                .andExpect(jsonPath("$.components.schemas.PostFinalizationChange.properties.difference")
                        .doesNotExist());
    }

    @Test
    void FR_027_httpListsChangesAndAdjustmentReplayCreatesOneItem() throws Exception {
        jdbc.update("UPDATE cost_item SET status='FINALIZED',locked_at=? WHERE id=?", Timestamp.from(time.now()), item);
        change(new BigDecimal("2"), false, "correction-" + item);
        long changeId = changes.list(PostFinalizationChange.KIND, new EntityRef("M11.COST_ITEM", item))
                .getFirst()
                .id();
        doReturn(false).when(residents).isFinalStatus(resident);
        String bearer = TestUsers.bearer(new TestUsers(jdbc, time).session(office.accountId()));
        mvc.perform(get("/api/v1/costs/post-finalization-changes")
                        .param("residentId", Long.toString(resident))
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itemId").value(item))
                .andExpect(jsonPath("$[0].after.quantity").value(2))
                .andExpect(jsonPath("$[0].difference").doesNotExist());
        String key = UUID.randomUUID().toString();
        String body =
                "{\"amount\":100,\"adjustmentType\":\"SOURCE_CHANGE\",\"reason\":\"Đính chính nguồn\",\"sourceChangeId\":"
                        + changeId + "}";
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(post("/api/v1/costs/items/{id}/adjust", item)
                            .header("Authorization", bearer)
                            .header("Idempotency-Key", key)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isCreated());
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cost_item WHERE original_item_id=?", Long.class, item))
                .isEqualTo(1);
    }

    @Test
    void FR_003_finalizedRegularStatementReceivesAdjustmentInSupplementaryStatement() {
        doReturn(false).when(residents).isFinalStatus(resident);
        locks.resident(resident);
        var regular = destination.regular(resident, YearMonth.from(time.today()), office.accountId());
        jdbc.update(
                "UPDATE cost_statement SET status='FINALIZED',total=0,totals_by_type='[]',finalized_at=created_at,locked_at=? WHERE id=?",
                Timestamp.from(time.now()),
                regular.id());
        jdbc.update("UPDATE cost_item SET status='FINALIZED',locked_at=? WHERE id=?", Timestamp.from(time.now()), item);
        long first = adjustments.adjust(item, 100, "SOURCE_CHANGE", Reason.of("Bổ sung"), null);
        long second = adjustments.adjust(item, 50, "SOURCE_CHANGE", Reason.of("Bổ sung tiếp"), null);
        var target = jdbc.queryForMap(
                "SELECT s.id,s.statement_type,s.period_id FROM cost_statement s JOIN cost_item i ON i.statement_id=s.id WHERE i.id=?",
                first);
        assertThat(target.get("statement_type")).isEqualTo("SUPPLEMENTARY");
        assertThat(((Number) target.get("period_id")).longValue()).isEqualTo(regular.periodId());
        assertThat(jdbc.queryForObject("SELECT statement_id FROM cost_item WHERE id=?", Long.class, second))
                .isEqualTo(((Number) target.get("id")).longValue());
        assertThat(jdbc.queryForObject("SELECT original_period_id FROM cost_item WHERE id=?", Long.class, first))
                .isEqualTo(period);
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void FR_026_concurrentReductionsCannotMakeTotalNegative() throws Exception {
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Boolean> reduce = () -> {
                if (!start.await(10, java.util.concurrent.TimeUnit.SECONDS))
                    throw new IllegalStateException("Không mở cổng bắt đầu");
                try {
                    adjustments.adjust(item, -200, "WAIVER", Reason.of("Miễn giảm"), null);
                    return true;
                } catch (BusinessException ex) {
                    assertThat(ex.errorCode()).isEqualTo("CONDITION_NOT_MET");
                    return false;
                }
            };
            var first = executor.submit(reduce);
            var second = executor.submit(reduce);
            start.countDown();
            assertThat(List.of(
                            first.get(20, java.util.concurrent.TimeUnit.SECONDS),
                            second.get(20, java.util.concurrent.TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }
        assertThat(jdbc.queryForObject(
                        "SELECT 300 + COALESCE(SUM(amount),0) FROM cost_item WHERE original_item_id=?",
                        Long.class,
                        item))
                .isEqualTo(100);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CHECKED", "APPROVED"})
    void FR_020_recalculationResetsApprovalAndReopensStatementWithHistory(String status) {
        jdbc.update(
                "UPDATE cost_item SET status=?,checked_by=created_by,checked_at=?,approved_by=created_by,approved_at=? WHERE id=?",
                status,
                Timestamp.from(time.now()),
                Timestamp.from(time.now()),
                item);
        jdbc.update("UPDATE cost_statement SET status='AWAITING_FINALIZATION' WHERE id=?", statement);
        doReturn(new CostPricingService.PriceQuote(new BigDecimal("300"), new BigDecimal("600"), false, null))
                .when(pricing)
                .event(resident, 9, month.atDay(1), new BigDecimal("2"));
        change(new BigDecimal("2"), false, "correction-" + item);
        var result = jdbc.queryForMap("SELECT status,amount,checked_by,approved_by FROM cost_item WHERE id=?", item);
        assertThat(result.get("status")).isEqualTo("DRAFT");
        assertThat(((Number) result.get("amount")).longValue()).isEqualTo(600);
        assertThat(result.get("checked_by")).isNull();
        assertThat(result.get("approved_by")).isNull();
        assertThat(jdbc.queryForObject("SELECT status FROM cost_statement WHERE id=?", String.class, statement))
                .isEqualTo("OPEN");
        assertThat(history.list(CostRecalculation.TRACK, item)).singleElement().satisfies(row -> {
            assertThat(row.details().before().amount()).isEqualByComparingTo("300");
            assertThat(row.details().after().amount()).isEqualByComparingTo("600");
            assertThat(row.details().resetToDraft()).isTrue();
        });
        assertThat(jdbc.queryForObject("SELECT @m11_recalculating", Integer.class))
                .isZero();
    }

    @Test
    void FR_020_cancelledSourceLocksItemAndRecordsHistoryWithoutPricing() {
        change(null, true, "void-" + item);
        assertThat(jdbc.queryForObject("SELECT status FROM cost_item WHERE id=?", String.class, item))
                .isEqualTo("CANCELLED");
        assertThat(history.list(CostRecalculation.TRACK, item)).hasSize(1);
        verify(pricing, never()).event(anyLong(), anyLong(), any(), any());
        assertThatThrownBy(() -> jdbc.update("UPDATE cost_item SET description='Sửa' WHERE id=?", item))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test
    void FR_027_finalizedSourceOnlyRecordsOneChangeOnReplayWithoutPricingOrAdjustment() {
        jdbc.update("UPDATE cost_item SET status='FINALIZED',locked_at=? WHERE id=?", Timestamp.from(time.now()), item);
        change(new BigDecimal("2"), false, "correction-" + item);
        change(new BigDecimal("2"), false, "correction-" + item);
        assertThat(changes.list(PostFinalizationChange.KIND, new EntityRef("M11.COST_ITEM", item)))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.content().before()).containsKey("quantity");
                    assertThat(row.content().after())
                            .containsKey("quantity")
                            .doesNotContainKeys("amount", "difference");
                });
        assertThat(jdbc.queryForObject("SELECT amount FROM cost_item WHERE id=?", Long.class, item))
                .isEqualTo(300);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cost_item WHERE original_item_id=?", Long.class, item))
                .isZero();
        verify(pricing, never()).event(anyLong(), anyLong(), any(), any());
    }

    @Test
    void FR_026_pendingReductionCountsAndAdjustmentRemainsInOriginalStatement() {
        long adjustment = adjustments.adjust(item, -300, "WAIVER", Reason.of("Miễn giảm"), null);
        assertThat(jdbc.queryForObject("SELECT statement_id FROM cost_item WHERE id=?", Long.class, adjustment))
                .isEqualTo(statement);
        assertThat(jdbc.queryForObject("SELECT status FROM cost_item WHERE id=?", String.class, adjustment))
                .isEqualTo("DRAFT");
        assertThatThrownBy(() -> adjustments.adjust(item, -1, "WAIVER", Reason.of("Giảm thêm"), null))
                .isInstanceOf(BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cost_item WHERE original_item_id=?", Long.class, item))
                .isEqualTo(1);
    }

    @Test
    void FR_026_auditFailureRollsBackAdjustmentAndStatementMutation() {
        var transaction = new org.springframework.transaction.support.TransactionTemplate(transactions);
        transaction.setPropagationBehavior(
                org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        doThrow(new IllegalStateException("Nhật ký không ghi được"))
                .when(audit)
                .write(argThat(entry -> entry.action().code().equals("COST_ADJUSTMENT_CREATE")));
        assertThatThrownBy(() -> transaction.executeWithoutResult(ignored -> {
                    fixture();
                    adjustments.adjust(item, -100, "WAIVER", Reason.of("Miễn giảm"), null);
                }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Nhật ký không ghi được");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cost_item WHERE resident_id=?", Long.class, resident))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cost_statement WHERE resident_id=?", Long.class, resident))
                .isZero();
    }

    @Test
    void FR_016_recalculationRequiresOneTransactionBoundConnection() {
        var outside = new org.springframework.transaction.support.TransactionTemplate(transactions);
        outside.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
        var cost = new CalculatedCost(
                BigDecimal.ONE, new BigDecimal("300"), null, new BigDecimal("300"), null, Map.of(), Set.of());
        assertThatThrownBy(() -> outside.executeWithoutResult(ignored -> store.recalculate(-1, cost)))
                .isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
    }

    @Test
    void FR_016_sqlFailureStillClearsRecalculationConnectionVariable() {
        jdbc.update("UPDATE cost_item SET status='FINALIZED',locked_at=? WHERE id=?", Timestamp.from(time.now()), item);
        var cost = new CalculatedCost(
                BigDecimal.ONE, new BigDecimal("300"), null, new BigDecimal("300"), null, Map.of(), Set.of());
        assertThatThrownBy(() -> store.recalculate(item, cost))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThat(jdbc.queryForObject("SELECT @m11_recalculating", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM cost_item WHERE id=?", String.class, item))
                .isEqualTo("FINALIZED");
    }
}
