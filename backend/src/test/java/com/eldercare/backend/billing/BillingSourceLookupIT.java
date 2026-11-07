package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.internal.source.domain.IndexedBillingSource;
import com.eldercare.backend.billing.internal.source.persistence.BillingSourceIndexStore;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** T011: phạm vi nguồn, đơn vị và khoản hiện có; không triển khai endpoint bằng dữ liệu giả. */
@Transactional
class BillingSourceLookupIT extends MySqlIntegrationTestBase {
    private static final LocalDate DAY = LocalDate.of(2026, 11, 4);
    private static final Instant AT = Instant.parse("2026-11-04T01:00:00Z");

    @Autowired
    BillingSourceIndexStore store;

    @Autowired
    JdbcTemplate jdbc;

    private IndexedBillingSource source(long resident, String type, String id, String unit, LocalDate day) {
        var source = new IndexedBillingSource(type, id, unit, resident, day, AT, null, null, new BigDecimal("0.125"));
        store.insertIfAbsent(source, AT);
        return source;
    }

    private IndexedBillingSource source(long resident, String id, String unit) {
        return source(resident, "OVERNIGHT_STAY", id, unit, DAY);
    }

    private long item(IndexedBillingSource source, boolean cancelled) {
        long period = TestUsers.nextId();
        long statement = TestUsers.nextId();
        long item = TestUsers.nextId();
        // Mỗi ca một kỳ riêng để schema test có dữ liệu cũ vẫn không đụng unique PERIOD.
        int seed = (int) Math.floorMod(period, 72000);
        var month = YearMonth.of(2000 + seed / 12, 1 + seed % 12);
        jdbc.update(
                """
                INSERT INTO cost_statement
                    (id,kind,period_month,period_starts_on,period_ends_on,unique_key,created_at)
                VALUES (?,'PERIOD',?,?,?,?,?)
                """,
                period,
                month.toString(),
                month.atDay(1),
                month.atEndOfMonth(),
                "PERIOD:" + month,
                Timestamp.from(AT));
        jdbc.update(
                """
                INSERT INTO cost_statement
                    (id,kind,period_id,resident_id,statement_type,unique_key,created_at,created_by)
                VALUES (?,'STATEMENT',?,?,'REGULAR',?,?,?)
                """,
                statement,
                period,
                source.residentId(),
                "STATEMENT:" + period + ":" + source.residentId() + ":REGULAR",
                Timestamp.from(AT),
                source.residentId());
        jdbc.update(
                """
                INSERT INTO cost_item
                    (id,resident_id,statement_id,period_id,cost_type,origin,source_type,source_id,billing_unit,
                     description,incurred_on,quantity,unit,unit_price,amount,status,unique_key,locked_at,created_at,created_by)
                VALUES (?,?,?,?,'FAMILY_OVERNIGHT_STAY','EVENT_BASED',?,?,?,
                        '[Khoản kiểm tra cứu nguồn]',?,1,'đêm',1,1,?,?,?,?,?)
                """,
                item,
                source.residentId(),
                statement,
                period,
                source.sourceType(),
                source.sourceId(),
                source.unit(),
                source.incurredOn(),
                cancelled ? "CANCELLED" : "DRAFT",
                cancelled ? null : source.sourceType() + ":" + source.sourceId() + ":" + source.unit(),
                cancelled ? Timestamp.from(AT) : null,
                Timestamp.from(AT),
                source.residentId());
        return item;
    }

    @Test
    void FR_005_idSelectionReturnsAllUnitsButOnlyForTheRequestedResidentTypeAndIds() {
        long resident = TestUsers.nextId();
        String id = "stay-" + TestUsers.nextId();
        source(resident, id, "first");
        source(resident, id, "second");
        source(resident, id + "-other", "night");
        source(TestUsers.nextId(), "OVERNIGHT_STAY", id, "other-resident", DAY);
        source(resident, "FAMILY_MEAL", id, "meal", DAY);

        var matches = store.findBySourceIds(resident, "OVERNIGHT_STAY", List.of(id, "missing"));

        assertThat(matches).extracting(row -> row.source().unit()).containsExactly("first", "second");
        assertThat(matches).allSatisfy(row -> {
            assertThat(row.source().sourceId()).isEqualTo(id);
            assertThat(row.source().residentId()).isEqualTo(resident);
            assertThat(row.source().sourceType()).isEqualTo("OVERNIGHT_STAY");
        });
    }

    @Test
    void FR_005_emptyIdSelectionNeverExpandsToAllSources() {
        long resident = TestUsers.nextId();
        source(resident, "stay-" + TestUsers.nextId(), "night");

        assertThat(store.findBySourceIds(resident, "OVERNIGHT_STAY", List.of())).isEmpty();
    }

    @Test
    void FR_005_lookupIsScopedToResidentTypeAndInclusiveDateRange() {
        long resident = TestUsers.nextId();
        String id = "stay-" + TestUsers.nextId();
        source(resident, "OVERNIGHT_STAY", id, "before", DAY.minusDays(1));
        source(resident, "OVERNIGHT_STAY", id, "first", DAY);
        source(resident, "OVERNIGHT_STAY", id, "last", DAY.plusDays(1));
        source(resident, "OVERNIGHT_STAY", id, "after", DAY.plusDays(2));
        source(TestUsers.nextId(), "OVERNIGHT_STAY", id + "-other", "first", DAY);
        source(resident, "FAMILY_MEAL", id, "meal", DAY);

        var matches = store.find(resident, "OVERNIGHT_STAY", DAY, DAY.plusDays(1));

        assertThat(matches).extracting(row -> row.source().unit()).containsExactly("first", "last");
        assertThat(matches).allSatisfy(row -> {
            assertThat(row.source().residentId()).isEqualTo(resident);
            assertThat(row.source().sourceType()).isEqualTo("OVERNIGHT_STAY");
        });
    }

    @Test
    void FR_005_sourcesWithAndWithoutCostsAreReturnedTogether() {
        long resident = TestUsers.nextId();
        var withItem = source(resident, "stay-" + TestUsers.nextId(), "night");
        var withoutItem = source(resident, "stay-" + TestUsers.nextId(), "night");
        long itemId = item(withItem, false);

        var matches = store.find(resident, "OVERNIGHT_STAY", DAY, DAY);

        assertThat(matches).hasSize(2);
        assertThat(matches)
                .filteredOn(row -> row.source().sourceId().equals(withItem.sourceId()))
                .singleElement()
                .satisfies(row -> assertThat(row.existingItemId()).isEqualTo(itemId));
        assertThat(matches)
                .filteredOn(row -> row.source().sourceId().equals(withoutItem.sourceId()))
                .singleElement()
                .satisfies(row -> assertThat(row.existingItemId()).isNull());
    }

    @Test
    void DBR_15_existingCostMatchesTheExactBillingUnit() {
        long resident = TestUsers.nextId();
        String id = "stay-" + TestUsers.nextId();
        var first = source(resident, id, "2026-11-03");
        source(resident, id, "2026-11-04");
        long itemId = item(first, false);

        var matches = store.find(resident, "OVERNIGHT_STAY", DAY, DAY);

        assertThat(matches).hasSize(2);
        assertThat(matches.getFirst().existingItemId()).isEqualTo(itemId);
        assertThat(matches.getLast().existingItemId()).isNull();
    }

    @Test
    void FR_006_cancelledCostDoesNotHideTheNewEffectiveCostOrDuplicateTheSource() {
        long resident = TestUsers.nextId();
        var source = source(resident, "stay-" + TestUsers.nextId(), "night");
        item(source, true);
        long active = item(source, false);

        assertThat(store.find(resident, "OVERNIGHT_STAY", DAY, DAY))
                .singleElement()
                .satisfies(row -> assertThat(row.existingItemId()).isEqualTo(active));
    }

    @Test
    void FR_020_lookupReadsCurrentQuantityCancellationAndVersionWithoutRestoringTheOriginal() {
        long resident = TestUsers.nextId();
        var source = source(resident, "stay-" + TestUsers.nextId(), "night");
        jdbc.update("""
                UPDATE cost_source_index SET current_quantity=2.125,cancelled=TRUE,version=3
                WHERE source_type=? AND source_id=? AND billing_unit=?
                """, source.sourceType(), source.sourceId(), source.unit());

        assertThat(store.find(resident, "OVERNIGHT_STAY", DAY, DAY))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.source().quantity()).isEqualByComparingTo("2.125");
                    assertThat(row.cancelled()).isTrue();
                    assertThat(row.version()).isEqualTo(3);
                    assertThat(row.source().occurredAt()).isEqualTo(AT);
                });
    }

    @Test
    void FR_005_emptyLookupDoesNotCreateAnyBusinessRecord() {
        long resident = TestUsers.nextId();

        assertThat(store.find(resident, "OVERNIGHT_STAY", DAY, DAY)).isEmpty();

        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM cost_statement WHERE resident_id=?", Integer.class, resident))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cost_item WHERE resident_id=?", Integer.class, resident))
                .isZero();
    }
}
