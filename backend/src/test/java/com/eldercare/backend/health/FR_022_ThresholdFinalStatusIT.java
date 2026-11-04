package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.eldercare.backend.health.internal.threshold.domain.ThresholdBand;
import com.eldercare.backend.health.internal.threshold.domain.ThresholdVersion;
import com.eldercare.backend.health.internal.threshold.persistence.ThresholdVersionStore;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.events.ResidentStatusChanged;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Dòng cuối bảng FR-022: người cao tuổi chuyển trạng thái cuối thì ngưỡng cá nhân Hiệu lực hết hiệu lực, Chờ hiệu lực
 * bị hủy. Listener chạy sau commit nên test ghi dữ liệu trong transaction riêng và chờ kết quả (tasks T019).
 */
class FR_022_ThresholdFinalStatusIT extends MySqlIntegrationTestBase {
    private static final Instant NOW = Instant.parse("2026-11-04T17:15:00Z");

    @Autowired
    ThresholdVersionStore store;

    @Autowired
    ApplicationEventPublisher events;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    JdbcTemplate jdbc;

    private long personal(long resident, String status, Instant from) {
        return tx.execute(s -> store.insert(
                new ThresholdVersion(
                        0,
                        "PERSONAL",
                        resident,
                        TestUsers.nextId(),
                        from,
                        null,
                        status,
                        Map.of("VALUE", new ThresholdBand(null, null, new BigDecimal("37.5"), null)),
                        0),
                null,
                TestUsers.nextId(),
                from));
    }

    private String status(long id) {
        return jdbc.queryForObject("SELECT status FROM monitoring_setting WHERE id = ?", String.class, id);
    }

    @Test
    void FR_022_finalStatusExpiresEffectiveAndCancelsPendingPersonalThresholds() {
        long resident = TestUsers.nextId();
        long effective = personal(resident, "EFFECTIVE", NOW.minus(Duration.ofDays(5)));
        long pending = personal(resident, "PENDING", NOW.plus(Duration.ofDays(1)));

        tx.executeWithoutResult(s -> events.publishEvent(new ResidentStatusChanged(
                resident, ResidentStatus.IN_RESIDENCE, ResidentStatus.DECEASED, null, TestUsers.nextId(), NOW)));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(status(effective)).isEqualTo("EXPIRED");
            assertThat(status(pending)).isEqualTo("CANCELLED");
        });
        assertThat(jdbc.queryForObject(
                                "SELECT effective_until FROM monitoring_setting WHERE id = ?",
                                java.sql.Timestamp.class,
                                effective)
                        .toInstant())
                .isEqualTo(NOW);
        assertThat(store.hasPersonalOpen(resident)).isFalse();
    }

    @Test
    void FR_022_nonFinalStatusKeepsPersonalThresholds() {
        long resident = TestUsers.nextId();
        long effective = personal(resident, "EFFECTIVE", NOW.minus(Duration.ofDays(5)));

        tx.executeWithoutResult(s -> events.publishEvent(new ResidentStatusChanged(
                resident, ResidentStatus.IN_RESIDENCE, ResidentStatus.HOSPITALIZED, null, TestUsers.nextId(), NOW)));

        await().during(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(status(effective)).isEqualTo("EFFECTIVE"));
    }
}
