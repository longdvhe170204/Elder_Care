package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.api.dto.NursingShiftView;
import com.eldercare.backend.medication.api.events.FloorWithoutNurse;
import com.eldercare.backend.medication.api.spi.NursingShiftPort;
import com.eldercare.backend.medication.internal.catalog.persistence.MedicationRepository;
import com.eldercare.backend.medication.internal.dose.domain.DoseStatus;
import com.eldercare.backend.medication.internal.dose.persistence.DoseResultRepository;
import com.eldercare.backend.medication.internal.dose.persistence.MedicationDoseRepository;
import com.eldercare.backend.medication.internal.dose.service.DoseAdministrationService;
import com.eldercare.backend.medication.internal.prescription.domain.Prescription;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionType;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.schedule.persistence.M07JobRunStore;
import com.eldercare.backend.medication.internal.schedule.service.CalculatedDoseSlot;
import com.eldercare.backend.medication.internal.schedule.service.M07JobService;
import com.eldercare.backend.medication.internal.schedule.service.MedicationScheduleCalculator;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class BR_M07_21_FloorWithoutNurseTest {

    @Test
    void BR_M07_21_notifiesFloorAndFacilityManagersOncePerFloorShift() {
        Instant now = Instant.parse("2026-10-28T00:00:00Z");
        Instant shiftEnd = Instant.parse("2026-10-28T08:00:00Z");
        Prescription prescription = mock(Prescription.class);
        when(prescription.getId()).thenReturn(10L);
        when(prescription.getResidentId()).thenReturn(201L);
        when(prescription.getMedicationId()).thenReturn(301L);
        when(prescription.getPrescriptionType()).thenReturn(PrescriptionType.PERIODIC);
        PrescriptionRepository prescriptions = mock(PrescriptionRepository.class);
        when(prescriptions.findByStatus(any())).thenReturn(List.of(prescription));
        ResidentLocationApi locations = mock(ResidentLocationApi.class);
        when(locations.at(201L, now))
                .thenReturn(Optional.of(new LocationView(11L, "Floor 1", 11L, "Floor 1", 21L, "Room", 31L, "B1")));
        NursingShiftPort shifts = mock(NursingShiftPort.class);
        when(shifts.shiftAt(11L, now)).thenReturn(Optional.of(new NursingShiftView(91L, 11L, now, shiftEnd)));
        when(shifts.nursesWithShift(11L, now)).thenReturn(List.of());
        MedicationScheduleCalculator schedules = mock(MedicationScheduleCalculator.class);
        when(schedules.calculate(eq(prescription), any(), any(), any(), eq(now), any()))
                .thenReturn(List.of(slot(1L, now.plusSeconds(3600)), slot(2L, now.plusSeconds(7200))));
        M07JobRunStore runs = mock(M07JobRunStore.class);
        when(runs.isCompleted("FLOOR_WITHOUT_NURSE", "11:91")).thenReturn(false, true);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        BusinessTime time = mock(BusinessTime.class);
        when(time.now()).thenReturn(now);
        M07JobService jobs = new M07JobService(
                runs,
                prescriptions,
                mock(MedicationRepository.class),
                mock(MedicationDoseRepository.class),
                mock(DoseResultRepository.class),
                schedules,
                mock(DoseAdministrationService.class),
                events,
                time,
                M07TestParameters.defaults(),
                locations,
                shifts,
                mock(com.eldercare.backend.medication.internal.schedule.service.AbsenceScheduleContexts.class),
                mock(com.eldercare.backend.medication.internal.reconciliation.service.ReconciliationDeadlines.class));

        jobs.runMinuteTasks(now);
        jobs.runMinuteTasks(now);

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(events, times(1)).publishEvent(event.capture());
        assertThat(event.getValue()).isInstanceOfSatisfying(FloorWithoutNurse.class, notice -> {
            assertThat(notice.floorAreaId()).isEqualTo(11L);
            assertThat(notice.shiftId()).isEqualTo(91L);
            assertThat(notice.upcomingDoseCount()).isEqualTo(2);
            assertThat(notice.notificationRequests()).singleElement().satisfies(request -> {
                assertThat(request.level()).contains(NotificationLevel.MEDIUM);
                assertThat(request.recipientGroups())
                        .contains(
                                new RecipientGroup.FloorManager(
                                        11L, new com.eldercare.backend.common.notification.Substitution.Default()),
                                new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
            });
        });
        verify(runs).recordCompletion("FLOOR_WITHOUT_NURSE", "11:91", "COMPLETED");
    }

    private static CalculatedDoseSlot slot(long id, Instant scheduled) {
        return new CalculatedDoseSlot(
                "10-" + id,
                10L,
                201L,
                301L,
                "Medication",
                BigDecimal.ONE,
                "tablet",
                "Oral",
                MedicationSource.FACILITY,
                false,
                scheduled,
                scheduled.minusSeconds(1800),
                scheduled.plusSeconds(1800),
                DoseStatus.UPCOMING,
                null,
                false,
                null);
    }
}
