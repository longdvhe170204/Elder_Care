package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.health.internal.governance.service.HealthStatusParticipant;
import com.eldercare.backend.health.internal.threshold.domain.ThresholdBand;
import com.eldercare.backend.health.internal.threshold.domain.ThresholdVersion;
import com.eldercare.backend.health.internal.threshold.persistence.ThresholdVersionStore;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Dòng cuối bảng FR-022: người cao tuổi chuyển trạng thái cuối thì ngưỡng cá nhân Hiệu lực hết hiệu lực, Chờ hiệu lực
 * bị hủy, trong cùng transaction của lệnh trạng thái qua bean tham gia của {@code health} (tasks T019, T060; research
 * S-17).
 */
class FR_022_ThresholdFinalStatusIT extends MySqlIntegrationTestBase {
    private static final Instant NOW = Instant.parse("2026-11-04T17:15:00Z");

    @Autowired
    ThresholdVersionStore store;

    @Autowired
    HealthStatusParticipant participant;

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

        tx.executeWithoutResult(
                s -> participant.applyEffects(result(resident, StatusCommand.RECORD_DEATH, ResidentStatus.DECEASED)));

        assertThat(status(effective)).isEqualTo("EXPIRED");
        assertThat(status(pending)).isEqualTo("CANCELLED");
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

        tx.executeWithoutResult(s ->
                participant.applyEffects(result(resident, StatusCommand.TRANSFER_OUT, ResidentStatus.HOSPITALIZED)));

        assertThat(status(effective)).isEqualTo("EFFECTIVE");
    }

    private static TransitionResult result(long resident, StatusCommand command, ResidentStatus to) {
        return new TransitionResult(resident, ResidentStatus.IN_RESIDENCE, to, command, TestUsers.nextId(), NOW, null);
    }
}
