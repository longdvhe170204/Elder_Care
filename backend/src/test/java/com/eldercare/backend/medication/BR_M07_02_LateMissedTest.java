package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.api.events.MedicationAlertRequestedEvent;
import com.eldercare.backend.medication.internal.catalog.persistence.MedicationRepository;
import com.eldercare.backend.medication.internal.dose.domain.DoseAuthorityBasis;
import com.eldercare.backend.medication.internal.dose.domain.DoseResult;
import com.eldercare.backend.medication.internal.dose.domain.DoseResultType;
import com.eldercare.backend.medication.internal.dose.domain.DoseStatus;
import com.eldercare.backend.medication.internal.dose.domain.MedicationDose;
import com.eldercare.backend.medication.internal.dose.persistence.DoseResultRepository;
import com.eldercare.backend.medication.internal.dose.persistence.MedicationDoseRepository;
import com.eldercare.backend.medication.internal.dose.service.DoseAdministrationService;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.schedule.persistence.M07JobRunStore;
import com.eldercare.backend.medication.internal.schedule.service.M07JobService;
import com.eldercare.backend.medication.internal.schedule.service.MedicationScheduleCalculator;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class BR_M07_02_LateMissedTest {

    @Test
    void BR_M07_02_gracePeriodClosesLateDoseAndRecordsSystemMissedResult() {
        Instant now = Instant.parse("2026-10-28T02:00:00Z");
        MedicationDose dose = new MedicationDose(
                10L,
                201L,
                301L,
                Instant.parse("2026-10-28T01:00:00Z"),
                Instant.parse("2026-10-28T00:30:00Z"),
                Instant.parse("2026-10-28T01:30:00Z"),
                BigDecimal.ONE,
                "tablet",
                MedicationSource.FACILITY,
                false,
                DoseStatus.LATE,
                11L,
                Instant.parse("2026-10-28T00:00:00Z"));
        setId(dose, 101L);

        MedicationDoseRepository doses = mock(MedicationDoseRepository.class);
        DoseResultRepository results = mock(DoseResultRepository.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        BusinessTime time = mock(BusinessTime.class);
        when(time.now()).thenReturn(now);
        when(doses.findByStatusAndLateAtLessThanEqual(eq(DoseStatus.LATE), any()))
                .thenReturn(List.of(dose));

        M07JobService jobs = new M07JobService(
                mock(M07JobRunStore.class),
                mock(PrescriptionRepository.class),
                mock(MedicationRepository.class),
                doses,
                results,
                mock(MedicationScheduleCalculator.class),
                mock(DoseAdministrationService.class),
                events,
                time,
                M07TestParameters.defaults(),
                mock(com.eldercare.backend.roombedallocation.api.ResidentLocationApi.class),
                mock(com.eldercare.backend.medication.api.spi.NursingShiftPort.class));

        jobs.runMinuteTasks(now);

        assertThat(dose.getStatus()).isEqualTo(DoseStatus.MISSED);
        assertThat(dose.getClosedByType()).isEqualTo("SYSTEM");
        assertThat(dose.getClosureRule()).isEqualTo("BR_M07_02");
        ArgumentCaptor<DoseResult> result = ArgumentCaptor.forClass(DoseResult.class);
        verify(results).save(result.capture());
        assertThat(result.getValue().getResultType()).isEqualTo(DoseResultType.MISSED);
        assertThat(result.getValue().getAuthorityBasis()).isEqualTo(DoseAuthorityBasis.SYSTEM);
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue()).isInstanceOfSatisfying(MedicationAlertRequestedEvent.class, alert -> {
            assertThat(alert.alertType()).isEqualTo("BO_LO_LIEU");
            assertThat(alert.alertLevel()).isEqualTo("TRUNG_BINH");
        });
    }

    private static void setId(Object entity, long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
