package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.shared.api.*;
import com.eldercare.backend.billing.internal.adjustment.service.CostAdjustmentService;
import com.eldercare.backend.billing.internal.item.domain.CostRecalculation;
import com.eldercare.backend.billing.internal.item.persistence.CostRecalculationStore;
import com.eldercare.backend.billing.internal.item.service.CostRecalculationService;
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
@org.springframework.test.context.event.RecordApplicationEvents
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

    @Autowired
    org.springframework.test.context.event.ApplicationEvents recorded;
}
