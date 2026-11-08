package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;

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
}
