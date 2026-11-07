package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.shared.api.ResidentModuleStateApi;
import com.eldercare.backend.health.internal.governance.domain.ResidentVitalSignState;
import com.eldercare.backend.health.internal.threshold.domain.ThresholdBand;
import com.eldercare.backend.health.internal.threshold.domain.ThresholdVersion;
import com.eldercare.backend.health.internal.threshold.persistence.ThresholdVersionStore;
import com.eldercare.backend.health.internal.threshold.service.MissingThresholdReminders;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.ResidentStatusApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusPeriod;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhắc thiếu ngưỡng cá nhân một lần, nhắc lại sau khi Ngừng (FR-025, BR-M06-02, Q-330) trên MySQL; trạng thái người cao tuổi của 001 dùng bean giả (tasks T019).
 */
@Transactional
class BR_M06_02_MissingPersonalThresholdReminderIT extends MySqlIntegrationTestBase {
    private static final Instant NOW = Instant.parse("2026-11-04T17:15:00Z");

    @MockitoBean
    ResidentApi residents;

    @MockitoBean
    ResidentStatusApi statuses;

    @Autowired
    MissingThresholdReminders reminders;

    @Autowired
    ThresholdVersionStore store;

    @Autowired
    ResidentModuleStateApi states;

    private long admittedResident(int daysAgo) {
        long resident = TestUsers.nextId();
        when(residents.byStatus(Set.of(ResidentStatus.IN_RESIDENCE))).thenReturn(List.of(resident));
        when(statuses.statusPeriods(eq(resident), eq(Set.of(ResidentStatus.IN_RESIDENCE)), any(), any()))
                .thenReturn(List.of(new StatusPeriod(
                        ResidentStatus.IN_RESIDENCE, NOW.minus(Duration.ofDays(daysAgo)), Optional.empty())));
        return resident;
    }

    private long personal(long resident, String status, Instant from) {
        return store.insert(
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
                from);
    }

    @Test
    void BR_M06_02_remindedOnceThenAgainAfterLastPersonalThresholdIsStopped() {
        long resident = admittedResident(8);

        assertThat(reminders.remind(NOW)).isEqualTo(1);
        assertThat(states.get(ResidentVitalSignState.KIND, resident)
                        .orElseThrow()
                        .missingThresholdRemindedAt())
                .isEqualTo(NOW);
        assertThat(reminders.remind(NOW.plus(Duration.ofDays(1)))).isZero();

        long id = personal(resident, "EFFECTIVE", NOW);
        assertThat(reminders.missing(NOW)).isEmpty();
        store.end(id, "EXPIRED", NOW.plus(Duration.ofDays(2)), "Đã hồi phục", TestUsers.nextId(), 0);
        reminders.allowReminderAgain(resident);

        assertThat(reminders.missing(NOW)).containsExactly(resident);
        assertThat(reminders.remind(NOW.plus(Duration.ofDays(3)))).isEqualTo(1);
    }
}
