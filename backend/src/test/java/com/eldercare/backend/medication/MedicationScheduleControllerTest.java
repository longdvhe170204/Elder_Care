package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.internal.dose.service.DoseAdministrationService;
import com.eldercare.backend.medication.internal.dose.service.DoseViewService;
import com.eldercare.backend.medication.internal.schedule.service.MedicationScheduleCalculator;
import com.eldercare.backend.medication.internal.schedule.service.MedicationScheduleService;
import com.eldercare.backend.medication.web.MedicationScheduleController;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MedicationScheduleControllerTest {

    @Test
    void CFG_M07_01_roundWithoutRangeUsesConfiguredWindowAroundNow() {
        Instant now = Instant.parse("2026-10-30T08:00:00Z");
        MedicationScheduleService schedules = mock(MedicationScheduleService.class);
        DoseAdministrationService administration = mock(DoseAdministrationService.class);
        DoseViewService doseViews = mock(DoseViewService.class);
        MedicationScheduleCalculator calculator = mock(MedicationScheduleCalculator.class);
        when(calculator.defaultWindowMinutes()).thenReturn(45);
        when(schedules.getMedicationRound(isNull(), any(), any(), any())).thenReturn(List.of());
        when(doseViews.views(List.of())).thenReturn(List.of());

        MedicationScheduleController controller = new MedicationScheduleController(
                schedules, administration, doseViews, new BusinessTime(Clock.fixed(now, ZoneOffset.UTC)), calculator);

        assertThat(controller.getMedicationRound(null, null, null)).isEmpty();

        ArgumentCaptor<Instant> from = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> to = ArgumentCaptor.forClass(Instant.class);
        verify(schedules).getMedicationRound(isNull(), from.capture(), to.capture(), any());
        assertThat(Duration.between(from.getValue(), now)).isEqualTo(Duration.ofMinutes(45));
        assertThat(Duration.between(now, to.getValue())).isEqualTo(Duration.ofMinutes(45));
    }
}
