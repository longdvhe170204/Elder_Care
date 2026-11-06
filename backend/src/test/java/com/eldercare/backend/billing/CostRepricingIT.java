package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.shared.api.StatusHistoryApi;
import com.eldercare.backend.admission.api.events.PriceVersionCreated;
import com.eldercare.backend.billing.internal.item.domain.*;
import com.eldercare.backend.billing.internal.item.persistence.CostRepricingStore;
import com.eldercare.backend.billing.internal.period.service.CostLock;
import com.eldercare.backend.billing.internal.pricing.service.CostPriceRecalculator;
import com.eldercare.backend.billing.internal.pricing.service.ExistingCostPricing;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

/** T013: dùng MySQL/khóa/lịch sử/nhật ký thật; chỉ mock bộ tính E để cô lập phần ghi và rollback. */
@Transactional
class CostRepricingIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    CostPriceRecalculator recalculator;

    @Autowired
    StatusHistoryApi history;

    @Autowired
    CostRepricingStore rows;

    @Autowired
    CostLock locks;

    @MockitoBean
    ExistingCostPricing pricing;

    @MockitoSpyBean
    AuditLogApi audit;

    record Fixture(long resident, long period, long statement, long item, long service, LocalDate day) {}

    private Fixture fixture() {
        long resident = TestUsers.nextId(), period = TestUsers.nextId(), statement = TestUsers.nextId();
        long item = TestUsers.nextId(), service = TestUsers.nextId();
        int seed = (int) Math.floorMod(period, 72000);
        var month = YearMonth.of(2000 + seed / 12, 1 + seed % 12);
        var at = Timestamp.from(Instant.parse("2026-11-06T02:00:00Z"));
        jdbc.update("""
                INSERT INTO cost_statement(id,kind,period_month,period_starts_on,period_ends_on,unique_key,created_at)
                VALUES (?,'PERIOD',?,?,?,?,?)
                """, period, month.toString(), month.atDay(1), month.atEndOfMonth(), "PERIOD:" + month, at);
        jdbc.update(
                """
                INSERT INTO cost_statement(id,kind,period_id,resident_id,statement_type,unique_key,created_at,created_by)
                VALUES (?,'STATEMENT',?,?,'REGULAR',?,?,?)
                """, statement, period, resident, "STATEMENT:" + period + ":" + resident + ":REGULAR", at, resident);
        String source = Long.toString(TestUsers.nextId());
        jdbc.update(
                """
                INSERT INTO cost_item(id,resident_id,statement_id,period_id,cost_type,origin,source_type,source_id,billing_unit,
                    service_id,description,incurred_on,quantity,unit,flags,unique_key,created_at,created_by)
                VALUES (?,?,?,?,'CONSUMABLE','EVENT_BASED','CARE_TASK_RESULT',?,'UNIT',?,
                    '[Khoản kiểm tính lại giá]',?,0.125,'gói','MISSING_UNIT_PRICE',?,?,?)
                """,
                item,
                resident,
                statement,
                period,
                source,
                service,
                month.atDay(1),
                "CARE_TASK_RESULT:" + source + ":UNIT",
                at,
                resident);
        var snapshot = new CostPricingSnapshot(
                new CostRecalculation.Calculation(
                        new BigDecimal("0.125"), new BigDecimal("256"), null, new BigDecimal("32")),
                Set.of(),
                Map.of("serviceId", service, "kind", "PRICE_VERSION", "priceVersionId", service + 1),
                Map.of("sourceId", source));
        when(pricing.calculate(argThat(row -> row != null && row.id() == item), anyMap()))
                .thenReturn(Optional.of(snapshot));
        return new Fixture(resident, period, statement, item, service, month.atDay(1));
    }

    private void apply(Fixture f) {
        recalculator.priceCreated(new PriceVersionCreated(f.service(), f.day(), f.day()));
    }

    @Test
    void FR_012_fillPriceStoresExactQuantityHistoryAndOnlyOneReceiptOnReplay() {
        var f = fixture();

        apply(f);
        apply(f);

        assertThat(jdbc.queryForObject("SELECT amount FROM cost_item WHERE id=?", BigDecimal.class, f.item()))
                .isEqualByComparingTo("32");
        assertThat(jdbc.queryForObject("SELECT quantity FROM cost_item WHERE id=?", BigDecimal.class, f.item()))
                .isEqualByComparingTo("0.125");
        assertThat(jdbc.queryForObject("SELECT flags FROM cost_item WHERE id=?", String.class, f.item()))
                .isEmpty();
        assertThat(history.list(CostRecalculation.TRACK, f.item()))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.details().before().amount()).isNull();
                    assertThat(row.details().after().amount()).isEqualByComparingTo("32");
                    assertThat(row.bySystem()).isTrue();
                    assertThat(((Number) row.details().basisAfter().get("priceVersionId")).longValue())
                            .isEqualTo(f.service() + 1);
                });
        assertThat(jdbc.queryForObject("SELECT @m11_recalculating", Long.class)).isNull();
    }

    @Test
    void DBR_17_cancelledItemsAreNotSelectedForPriceUpdates() {
        var f = fixture();
        jdbc.update("""
                UPDATE cost_item SET status='CANCELLED',locked_at=?,unique_key=NULL,cancelled_by=?,cancellation_reason='Kiểm bảo vệ'
                WHERE id=?
                """, Timestamp.from(Instant.parse("2026-11-06T02:00:00Z")), f.resident(), f.item());

        apply(f);

        assertThat(jdbc.queryForObject("SELECT amount FROM cost_item WHERE id=?", BigDecimal.class, f.item()))
                .isNull();
        assertThat(history.list(CostRecalculation.TRACK, f.item())).isEmpty();
        verifyNoInteractions(pricing);
    }

    @Test
    void DBR_17_evenALegacyFinalizedMissingPriceCannotBeModified() {
        var f = fixture();
        var at = Timestamp.from(Instant.parse("2026-11-06T02:00:00Z"));
        jdbc.update("UPDATE cost_item SET status='FINALIZED',locked_at=? WHERE id=?", at, f.item());
        jdbc.update("""
                UPDATE cost_statement SET status='FINALIZED',total=0,totals_by_type=JSON_ARRAY(),
                    finalized_at=?,locked_at=? WHERE id=?
                """, at, at, f.statement());

        apply(f);

        assertThat(jdbc.queryForObject("SELECT amount FROM cost_item WHERE id=?", BigDecimal.class, f.item()))
                .isNull();
        assertThat(history.list(CostRecalculation.TRACK, f.item())).isEmpty();
        verifyNoInteractions(pricing);
    }

    @Test
    void FR_004_repricingClearsApprovalAndReopensTheAwaitingStatement() {
        var f = fixture();
        var at = Timestamp.from(Instant.parse("2026-11-06T02:00:00Z"));
        jdbc.execute("SET @m11_recalculating = 1");
        try {
            jdbc.update("""
                    UPDATE cost_item SET unit_price=256,amount=32,flags='',status='APPROVED',
                        checked_by=?,checked_at=?,approved_by=?,approved_at=? WHERE id=?
                    """, f.resident(), at, f.resident() + 1, at, f.item());
        } finally {
            jdbc.execute("SET @m11_recalculating = NULL");
        }
        jdbc.update("UPDATE cost_statement SET status='AWAITING_FINALIZATION' WHERE id=?", f.statement());
        locks.statement(f.resident(), f.statement());
        var before = rows.lock(f.item(), f.resident()).orElseThrow();
        var next = new CostPricingSnapshot(
                new CostRecalculation.Calculation(
                        new BigDecimal("0.125"), new BigDecimal("512"), null, new BigDecimal("64")),
                Set.of(),
                Map.of("serviceId", f.service()),
                Map.of());

        rows.apply(before, next);

        assertThat(jdbc.queryForObject("SELECT status FROM cost_item WHERE id=?", String.class, f.item()))
                .isEqualTo("DRAFT");
        assertThat(jdbc.queryForObject("SELECT checked_by FROM cost_item WHERE id=?", Long.class, f.item()))
                .isNull();
        assertThat(jdbc.queryForObject("SELECT approved_by FROM cost_item WHERE id=?", Long.class, f.item()))
                .isNull();
        assertThat(jdbc.queryForObject("SELECT status FROM cost_statement WHERE id=?", String.class, f.statement()))
                .isEqualTo("OPEN");
        assertThat(jdbc.queryForObject("SELECT @m11_recalculating", Long.class)).isNull();
    }

    @Test
    void FR_047_auditFailureRollsBackThePriceHistoryAndReceiptTogether() {
        var f = fixture();
        doThrow(new IllegalStateException("Lỗi nhật ký dùng để kiểm rollback"))
                .when(audit)
                .write(argThat(entry -> entry != null && entry.entityType().equals("M11.COST_ITEM")));

        assertThatThrownBy(() -> apply(f)).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT @m11_recalculating", Long.class)).isNull();
        TestTransaction.flagForRollback();
        TestTransaction.end();

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cost_item WHERE id=?", Integer.class, f.item()))
                .isZero();
        assertThat(history.list(CostRecalculation.TRACK, f.item())).isEmpty();
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM reminder_marker WHERE kind='M11.SOURCE_PROCESSED'
                    AND entity_type='M11.PRICE_RECALCULATION' AND entity_id=?
                """, Integer.class, f.resident())).isZero();
    }
}
