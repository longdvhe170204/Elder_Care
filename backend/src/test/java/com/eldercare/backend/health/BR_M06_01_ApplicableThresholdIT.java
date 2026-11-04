package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.health.internal.threshold.domain.ThresholdBand;
import com.eldercare.backend.health.internal.threshold.domain.ThresholdVersion;
import com.eldercare.backend.health.internal.threshold.persistence.ApplicableThresholdStore;
import com.eldercare.backend.health.internal.threshold.persistence.ThresholdVersionStore;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Ngưỡng áp dụng theo thời điểm đo và ràng buộc phiên bản (BR-M06-01, FR-022 → FR-024; tasks T019). */
@Transactional
class BR_M06_01_ApplicableThresholdIT extends MySqlIntegrationTestBase {
    private static final Instant TODAY = Instant.parse("2026-11-04T02:00:00Z");
    private static final Instant TOMORROW = TODAY.plus(Duration.ofDays(1));

    @Autowired
    ThresholdVersionStore store;

    @Autowired
    ApplicableThresholdStore applicable;

    @Autowired
    JdbcTemplate jdbc;

    private static Map<String, ThresholdBand> band(String warningAbove) {
        return Map.of("VALUE", new ThresholdBand(null, null, new BigDecimal(warningAbove), null));
    }

    private long insert(String scope, Long resident, long indicator, Instant from, String status, String warningAbove) {
        return store.insert(
                new ThresholdVersion(0, scope, resident, indicator, from, null, status, band(warningAbove), 0),
                null,
                TestUsers.nextId(),
                from);
    }

    @Test
    void BR_M06_01_personalThresholdFromTomorrowReplacesFacilityOnlyFromThen() {
        long indicator = TestUsers.nextId();
        long resident = TestUsers.nextId();
        long facility = insert("FACILITY", null, indicator, TODAY.minus(Duration.ofDays(30)), "EFFECTIVE", "37.5");
        long personal = insert("PERSONAL", resident, indicator, TOMORROW, "PENDING", "38");

        assertThat(applicable.resolve(resident, indicator, TODAY).thresholdVersionId())
                .isEqualTo(facility);
        store.activate(personal, 0);
        assertThat(applicable
                        .resolve(resident, indicator, TOMORROW.plusSeconds(60))
                        .thresholdVersionId())
                .isEqualTo(personal);
        assertThat(applicable.resolve(resident, indicator, TODAY).thresholdVersionId())
                .isEqualTo(facility);
    }

    @Test
    void FR_022_atMostOneEffectiveAndOnePendingPerScopeAndIndicator() {
        long indicator = TestUsers.nextId();
        insert("FACILITY", null, indicator, TODAY, "EFFECTIVE", "37.5");
        insert("FACILITY", null, indicator, TOMORROW, "PENDING", "37.8");

        assertThatThrownBy(() -> insert("FACILITY", null, indicator, TODAY, "EFFECTIVE", "37.6"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> insert("FACILITY", null, indicator, TOMORROW, "PENDING", "37.9"))
                .isInstanceOf(DataAccessException.class);
        assertThat(store.open("FACILITY", null, indicator)).hasSize(2);
    }

    @Test
    void FR_023_versionContentCannotChangeAndFinalStatusIsFinal() {
        long indicator = TestUsers.nextId();
        long id = insert("FACILITY", null, indicator, TODAY, "EFFECTIVE", "37.5");

        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE monitoring_setting SET components = JSON_OBJECT('VALUE', JSON_OBJECT()) WHERE id = ?",
                        id))
                .isInstanceOf(DataAccessException.class);
        store.end(id, "EXPIRED", TOMORROW, null, null, 0);
        assertThatThrownBy(() -> jdbc.update("UPDATE monitoring_setting SET status = 'EFFECTIVE' WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class);
        assertThat(store.lock(id).orElseThrow().effectiveUntil()).isEqualTo(TOMORROW);
    }

    @Test
    void FR_022_dueListsOnlyPendingVersionsWhoseTimeHasCome() {
        long indicator = TestUsers.nextId();
        long due = insert("FACILITY", null, indicator, TODAY, "PENDING", "37.5");
        insert("PERSONAL", TestUsers.nextId(), indicator, TOMORROW, "PENDING", "38");

        assertThat(store.due(TODAY.plusSeconds(1)).stream().map(ThresholdVersion::id))
                .contains(due);
        assertThat(store.due(TODAY.plusSeconds(1)).stream().filter(v -> v.indicatorId() == indicator))
                .hasSize(1);
    }
}
