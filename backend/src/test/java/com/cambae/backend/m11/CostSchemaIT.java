package com.cambae.backend.m11;

import static org.assertj.core.api.Assertions.*;

import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.TestUsers;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.*;

@Transactional
class CostSchemaIT extends MySqlIntegrationTestBase {
    private static final Timestamp AT = Timestamp.from(Instant.parse("2026-10-30T01:00:00Z"));

    @Autowired
    JdbcTemplate jdbc;

    record Fixture(long period, long statement, long resident, YearMonth month) {}

    @BeforeEach
    @AfterEach
    void clearRecalculationGate() {
        jdbc.execute("SET @m11_recalculating = NULL");
    }

    private long period(YearMonth month) {
        long id = TestUsers.nextId();
        jdbc.update("""
                INSERT INTO cost_statement
                  (id,kind,period_month,period_starts_on,period_ends_on,unique_key,created_at)
                VALUES (?,'PERIOD',?,?,?,?,?)
                """, id, month.toString(), month.atDay(1), month.atEndOfMonth(), "PERIOD:" + month, AT);
        return id;
    }

    private long statement(long period, long resident, String type) {
        long id = TestUsers.nextId();
        jdbc.update("""
                INSERT INTO cost_statement
                  (id,kind,period_id,resident_id,statement_type,unique_key,created_at,created_by)
                VALUES (?,'STATEMENT',?,?,?,?,?,?)
                """, id, period, resident, type, "STATEMENT:" + period + ":" + resident + ":" + type, AT, resident);
        return id;
    }

    private Fixture fixture() {
        long resident = TestUsers.nextId();
        long seed = Math.floorMod(resident, 72000);
        var month = YearMonth.of(2000 + (int) seed / 12, 1 + (int) seed % 12);
        long period = period(month);
        return new Fixture(period, statement(period, resident, "REGULAR"), resident, month);
    }

    private long item(Fixture f) {
        long id = TestUsers.nextId();
        jdbc.update("""
                INSERT INTO cost_item
                  (id,resident_id,statement_id,period_id,cost_type,origin,description,incurred_on,
                   quantity,unit,unit_price,amount,reason,created_at,created_by)
                VALUES (?,?,?,?,'OTHER','MANUAL','[Khoản kiểm ràng buộc]',?,1,'UNIT',300,300,
                        'Kiểm tra schema',?,?)
                """, id, f.resident(), f.statement(), f.period(), f.month().atDay(1), AT, f.resident());
        return id;
    }

    private long eventItem(Fixture f, String sourceId, String flags, Long price, Long amount) {
        long id = TestUsers.nextId();
        jdbc.update(
                """
                INSERT INTO cost_item
                  (id,resident_id,statement_id,period_id,cost_type,origin,source_type,source_id,billing_unit,
                   description,incurred_on,quantity,unit,unit_price,amount,flags,unique_key,created_at,created_by)
                VALUES (?,?,?,?,'RESIDENCY_FEE','EVENT_BASED','RESIDENCY_DAY',?,'DAY',
                        '[Phí lưu trú kiểm schema]',?,1,'DAY',?,?,?,?,?,?)
                """,
                id,
                f.resident(),
                f.statement(),
                f.period(),
                sourceId,
                f.month().atDay(1),
                price,
                amount,
                flags,
                "RESIDENCY_DAY:" + sourceId + ":DAY",
                AT,
                f.resident());
        return id;
    }

    private void finalizeStatement(long id) {
        jdbc.update("""
                UPDATE cost_statement SET status='FINALIZED', total=300,
                  totals_by_type=JSON_ARRAY(JSON_OBJECT('type','OTHER','total',300)),
                  finalized_at=?,locked_at=? WHERE id=?
                """, AT, AT, id);
    }

    private void blocked(ThrowingCallable operation, String cause) {
        assertThatThrownBy(operation)
                .isInstanceOf(DataAccessException.class)
                .rootCause()
                .isInstanceOf(SQLException.class)
                .hasMessageContaining(cause);
    }

    @Test
    void FR_001_onlyTheThreePrivateR15TablesAreCreated() {
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema=DATABASE() AND table_name IN ('cost_statement','cost_item','cost_source_index')
                """, Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()
                AND table_name IN ('resident_cost_guard','cost_period','cost_statement_type_total',
                  'cost_recalculation_history','cost_source_event_processed','cost_scheduler_run')
                """, Integer.class)).isZero();
    }

    @Test
    void FR_001_calendarMonthIsUniqueAndRequiresItsRealFirstAndLastDay() {
        var f = fixture();
        blocked(() -> period(f.month()), "uq_cost_statement_key");
        blocked(
                () -> jdbc.update("UPDATE cost_statement SET period_ends_on=period_starts_on WHERE id=?", f.period()),
                "chk_cost_statement_shape");
        blocked(
                () -> jdbc.update("UPDATE cost_statement SET resident_id=? WHERE id=?", f.resident(), f.period()),
                "chk_cost_statement_shape");
    }

    @Test
    void FR_002_oneRegularAndOneSupplementaryStatementPerResidentAndPeriod() {
        var f = fixture();
        blocked(() -> statement(f.period(), f.resident(), "REGULAR"), "uq_cost_statement_key");
        assertThat(statement(f.period(), f.resident(), "SUPPLEMENTARY")).isPositive();
        blocked(() -> statement(f.statement(), f.resident(), "REGULAR"), "M11_INVALID_PERIOD");
        blocked(
                () -> jdbc.update("UPDATE cost_statement SET unique_key='wrong' WHERE id=?", f.statement()),
                "chk_cost_statement_key");
    }

    @Test
    void DBR_15_aCancelledSourceCanBeRecreatedButAnEffectiveSourceCannotBeDuplicated() {
        var f = fixture();
        String source = "resident:" + f.resident();
        long id = eventItem(f, source, "", 300L, 300L);
        blocked(() -> eventItem(f, source, "", 300L, 300L), "uq_cost_item_key");
        jdbc.update("UPDATE cost_item SET status='CANCELLED',locked_at=?,unique_key=NULL WHERE id=?", AT, id);
        assertThat(eventItem(f, source, "", 300L, 300L)).isNotEqualTo(id);
        blocked(
                () -> jdbc.update("UPDATE cost_item SET status='DRAFT',locked_at=NULL WHERE id=?", id),
                "M11_ITEM_LOCKED");
    }

    @Test
    void DBR_15_sourceIdentityCannotBeHiddenBehindAnArbitraryUniqueKey() {
        var f = fixture();
        long id = eventItem(f, "source:" + f.resident(), "", 300L, 300L);
        blocked(() -> jdbc.update("UPDATE cost_item SET unique_key=NULL WHERE id=?", id), "chk_cost_item_key");
        blocked(
                () -> jdbc.update("UPDATE cost_item SET unique_key='different-key' WHERE id=?", id),
                "chk_cost_item_key");
    }

    @Test
    void DBR_15_manualItemsRequireAReasonAndCannotAlsoPointToAnEvent() {
        var f = fixture();
        long id = item(f);
        assertThat(item(f)).isNotEqualTo(id);
        blocked(() -> jdbc.update("UPDATE cost_item SET reason='' WHERE id=?", id), "chk_cost_item_origin");
        blocked(
                () -> jdbc.update(
                        "UPDATE cost_item SET source_type='DOSE',source_id='1',billing_unit='DOSE' WHERE id=?", id),
                "chk_cost_item_origin");
    }

    @Test
    void FR_012_missingPriceUsesNullAmountAndCannotBeConfusedWithAZeroPrice() {
        var f = fixture();
        long id = eventItem(f, "unpriced:" + f.resident(), "MISSING_UNIT_PRICE", null, null);
        blocked(() -> jdbc.update("UPDATE cost_item SET flags='' WHERE id=?", id), "chk_cost_item_unpriced");
        jdbc.execute("SET @m11_recalculating=1");
        jdbc.update("UPDATE cost_item SET flags='',unit_price=0,amount=0 WHERE id=?", id);
        assertThat(jdbc.queryForObject("SELECT amount FROM cost_item WHERE id=?", BigDecimal.class, id))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void FR_016_priceChangesRequireTheRecalculationGateOnTheSameConnection() {
        var f = fixture();
        long id = item(f);
        blocked(() -> jdbc.update("UPDATE cost_item SET amount=301 WHERE id=?", id), "M11_RECALCULATION_REQUIRED");
        jdbc.execute("SET @m11_recalculating=0");
        blocked(() -> jdbc.update("UPDATE cost_item SET quantity=2 WHERE id=?", id), "M11_RECALCULATION_REQUIRED");
        jdbc.execute("SET @m11_recalculating=1");
        jdbc.update("UPDATE cost_item SET quantity=2,unit_price=301,factor_percent=100,amount=602 WHERE id=?", id);
        assertThat(jdbc.queryForObject("SELECT amount FROM cost_item WHERE id=?", BigDecimal.class, id))
                .isEqualByComparingTo("602");
    }

    @Test
    void DBR_17_aFinalizedStatementKeepsEveryColumnExceptItsFirstExportReference() {
        var f = fixture();
        finalizeStatement(f.statement());
        blocked(
                () -> jdbc.update(
                        "UPDATE cost_statement SET status='OPEN',locked_at=NULL,finalized_at=NULL WHERE id=?",
                        f.statement()),
                "M11_STATEMENT_LOCKED");
        blocked(
                () -> jdbc.update("UPDATE cost_statement SET total=301 WHERE id=?", f.statement()),
                "M11_STATEMENT_LOCKED");
        blocked(
                () -> jdbc.update("UPDATE cost_statement SET totals_by_type=JSON_ARRAY() WHERE id=?", f.statement()),
                "M11_STATEMENT_LOCKED");
        jdbc.update("UPDATE cost_statement SET first_export_id=? WHERE id=?", TestUsers.nextId(), f.statement());
        blocked(
                () -> jdbc.update(
                        "UPDATE cost_statement SET first_export_id=? WHERE id=?", TestUsers.nextId(), f.statement()),
                "M11_STATEMENT_LOCKED");
    }

    @Test
    void DBR_17_theRecalculationGateCannotUnlockAFinalizedItem() {
        var f = fixture();
        long id = item(f);
        jdbc.update("UPDATE cost_item SET status='FINALIZED',locked_at=? WHERE id=?", AT, id);
        jdbc.execute("SET @m11_recalculating=1");
        blocked(() -> jdbc.update("UPDATE cost_item SET amount=301 WHERE id=?", id), "M11_ITEM_LOCKED");
        blocked(
                () -> jdbc.update("UPDATE cost_item SET locked_at=NULL,status='DRAFT' WHERE id=?", id),
                "M11_ITEM_LOCKED");
    }

    @Test
    void DBR_17_bothTheOldAndNewParentProtectTheirItemsAfterFinalization() {
        var frozen = fixture();
        long item = item(frozen);
        var open = fixture();
        long other = item(open);
        finalizeStatement(frozen.statement());
        blocked(
                () -> jdbc.update("UPDATE cost_item SET description='Đổi nội dung' WHERE id=?", item),
                "M11_PARENT_LOCKED");
        blocked(
                () -> jdbc.update(
                        "UPDATE cost_item SET statement_id=?,period_id=?,resident_id=? WHERE id=?",
                        open.statement(),
                        open.period(),
                        open.resident(),
                        item),
                "M11_PARENT_LOCKED");
        blocked(
                () -> jdbc.update(
                        "UPDATE cost_item SET statement_id=?,period_id=?,resident_id=? WHERE id=?",
                        frozen.statement(),
                        frozen.period(),
                        frozen.resident(),
                        other),
                "M11_PARENT_LOCKED");
        blocked(() -> item(frozen), "M11_PARENT_LOCKED");
    }

    @Test
    void DBR_17_theItemMustBelongToTheStatementsResidentAndPeriod() {
        var f = fixture();
        long id = item(f);
        blocked(
                () -> jdbc.update("UPDATE cost_item SET resident_id=? WHERE id=?", TestUsers.nextId(), id),
                "M11_INVALID_STATEMENT");
    }

    @Test
    void Q_395_theEightFlagsExcludeBothRemovedAbsencePolicyFlags() {
        String type = jdbc.queryForObject("""
                SELECT column_type FROM information_schema.columns WHERE table_schema=DATABASE()
                  AND table_name='cost_item' AND column_name='flags'
                """, String.class);
        assertThat(type.split(",")).hasSize(8);
        assertThat(type)
                .contains("MISSING_UNIT_PRICE", "INCLUDED_IN_PACKAGE")
                .doesNotContain("AWAITING_DECISION", "NO_POLICY_ROW");
        for (String column : List.of("unit_price", "amount")) {
            assertThat(jdbc.queryForObject("""
                    SELECT numeric_precision FROM information_schema.columns WHERE table_schema=DATABASE()
                      AND table_name='cost_item' AND column_name=?
                    """, Integer.class, column)).isEqualTo(15);
            assertThat(jdbc.queryForObject("""
                    SELECT numeric_scale FROM information_schema.columns WHERE table_schema=DATABASE()
                      AND table_name='cost_item' AND column_name=?
                    """, Integer.class, column)).isZero();
        }
    }

    @Test
    void FR_005_theTechnicalIndexKeepsOneRowPerSourceUnitAndDoesNotStoreResidencyDays() {
        var f = fixture();
        String source = "meal:" + f.resident();
        jdbc.update("""
                INSERT INTO cost_source_index(source_type,source_id,billing_unit,resident_id,incurred_on,
                  occurred_at,current_quantity,updated_at) VALUES ('FAMILY_MEAL',?,'MEAL',?,?,?,1,?)
                """, source, f.resident(), f.month().atDay(1), AT, AT);
        blocked(() -> jdbc.update("""
                INSERT INTO cost_source_index(source_type,source_id,billing_unit,resident_id,incurred_on,
                  occurred_at,current_quantity,updated_at) VALUES ('FAMILY_MEAL',?,'MEAL',?,?,?,1,?)
                """, source, f.resident(), f.month().atDay(1), AT, AT), "PRIMARY");
        assertThatThrownBy(
                        () -> jdbc.update("""
                INSERT INTO cost_source_index(source_type,source_id,billing_unit,resident_id,incurred_on,
                  occurred_at,current_quantity,updated_at) VALUES ('RESIDENCY_DAY',?,'DAY',?,?,?,1,?)
                """, source, f.resident(), f.month().atDay(1), AT, AT))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void BR_M15_03_adminCannotRewriteALockedStatementAndOnlyRetentionCanDeleteIt() throws Exception {
        var f = fixture();
        long id = item(f);
        try {
            finalizeStatement(f.statement());
            blocked(() -> jdbc.update("DELETE FROM cost_item WHERE id=?", id), "DELETE command denied");
            try (var admin = adminConnection();
                    var command = admin.prepareStatement("UPDATE cost_statement SET total=301 WHERE id=?")) {
                command.setLong(1, f.statement());
                assertThatThrownBy(command::executeUpdate)
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining("M11_STATEMENT_LOCKED");
            }
        } finally {
            try (var connection = retentionConnection()) {
                for (var row : List.of(
                        Map.entry("cost_item", id),
                        Map.entry("cost_statement", f.statement()),
                        Map.entry("cost_statement", f.period()))) {
                    try (var command = connection.prepareStatement("DELETE FROM " + row.getKey() + " WHERE id=?")) {
                        command.setLong(1, row.getValue());
                        command.executeUpdate();
                    }
                }
            }
        }
    }
}
