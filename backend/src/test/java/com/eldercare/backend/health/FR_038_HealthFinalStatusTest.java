package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.eldercare.backend.health.internal.governance.service.HealthStatusParticipant;
import com.eldercare.backend.health.internal.schedule.service.MeasurementScheduleService;
import com.eldercare.backend.health.internal.threshold.service.ThresholdFinalStatus;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.Order;

/**
 * Bean tham gia lệnh trạng thái của {@code health} (T060; research S-17; FR-006, FR-022): trạng thái cuối thì lịch đo
 * Đã ngừng, ngưỡng cá nhân hết hiệu lực hoặc bị hủy, trong cùng lệnh của 001.
 */
class FR_038_HealthFinalStatusTest {
    private static final long A = 101L;
    private static final Instant AT = Instant.parse("2026-11-06T03:00:00Z");

    private final MeasurementScheduleService schedules = mock(MeasurementScheduleService.class);
    private final ThresholdFinalStatus thresholds = mock(ThresholdFinalStatus.class);
    private final HealthStatusParticipant participant = new HealthStatusParticipant(schedules, thresholds);

    @Test
    void T060_finalCommands_noConditions_orderBeforeIncident() {
        assertThat(participant.interestedCommands())
                .containsExactlyInAnyOrder(
                        StatusCommand.END_RESIDENCY, StatusCommand.RECORD_DEATH, StatusCommand.CANCEL_ADMISSION);
        assertThat(participant.checkConditions(null)).isEmpty();
        assertThat(HealthStatusParticipant.class.getAnnotation(Order.class).value())
                .isEqualTo(50);
    }

    private static TransitionResult result(ResidentStatus to) {
        return new TransitionResult(A, ResidentStatus.IN_RESIDENCE, to, StatusCommand.RECORD_DEATH, 1L, AT, null);
    }
}
