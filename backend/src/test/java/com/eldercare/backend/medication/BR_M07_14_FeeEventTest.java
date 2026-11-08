package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.dto.AdministerDoseRequest;
import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.api.events.BilledSource;
import com.eldercare.backend.medication.api.events.MedicationBilledEvent;
import com.eldercare.backend.medication.internal.catalog.persistence.MedicationRepository;
import com.eldercare.backend.medication.internal.dose.domain.DoseAuthorityBasis;
import com.eldercare.backend.medication.internal.dose.domain.DoseResult;
import com.eldercare.backend.medication.internal.dose.domain.DoseStatus;
import com.eldercare.backend.medication.internal.dose.domain.MedicationDose;
import com.eldercare.backend.medication.internal.dose.persistence.DoseResultRepository;
import com.eldercare.backend.medication.internal.dose.persistence.DoseReviewRepository;
import com.eldercare.backend.medication.internal.dose.persistence.MedicationDoseRepository;
import com.eldercare.backend.medication.internal.dose.service.DoseAdministrationService;
import com.eldercare.backend.medication.internal.dose.service.DoseAssigneeResolver;
import com.eldercare.backend.medication.internal.family.service.FamilyMedicationService;
import com.eldercare.backend.medication.internal.prescription.domain.Prescription;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.reconciliation.persistence.ResidentMedicationLock;
import com.eldercare.backend.medication.internal.schedule.service.MedicationScheduleCalculator;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class BR_M07_14_FeeEventTest {

    @Test
    void BR_M07_14_facilityDoseBillsOnceButCarriedDoseDoesNotBillAgain() {
        Instant scheduled = Instant.parse("2026-10-28T01:00:00Z");
        Instant now = Instant.parse("2026-10-28T01:10:00Z");
        MedicationDose dose = mock(MedicationDose.class);
        when(dose.getId()).thenReturn(101L);
        when(dose.getPrescriptionId()).thenReturn(10L);
        when(dose.getResidentId()).thenReturn(201L);
        when(dose.getMedicationId()).thenReturn(301L);
        when(dose.getMedicationSource()).thenReturn(MedicationSource.FACILITY);
        when(dose.getDosage()).thenReturn(new BigDecimal("2.00"));
        when(dose.getDosageUnit()).thenReturn("tablet");
        when(dose.getScheduledTime()).thenReturn(scheduled);
        when(dose.getWindowFrom()).thenReturn(scheduled.minusSeconds(1800));
        when(dose.getWindowTo()).thenReturn(scheduled.plusSeconds(1800));
        when(dose.getStatus()).thenReturn(DoseStatus.ADMINISTERED);
        when(dose.getFloorAreaId()).thenReturn(11L);

        Prescription prescription = mock(Prescription.class);
        when(prescription.getResidentId()).thenReturn(201L);
        MedicationDoseRepository doses = mock(MedicationDoseRepository.class);
        when(doses.findByPrescriptionIdAndScheduledTime(10L, scheduled)).thenReturn(Optional.of(dose));
        PrescriptionRepository prescriptions = mock(PrescriptionRepository.class);
        when(prescriptions.findById(10L)).thenReturn(Optional.of(prescription));
        DoseResultRepository results = mock(DoseResultRepository.class);
        when(results.save(any(DoseResult.class))).thenAnswer(invocation -> {
            DoseResult result = invocation.getArgument(0);
            setId(result, 501L);
            return result;
        });
        DoseAssigneeResolver assignees = mock(DoseAssigneeResolver.class);
        when(assignees.resolve(any(), any(), any(), any()))
                .thenReturn(new DoseAssigneeResolver.DoseAssignee(
                        77L, false, DoseAuthorityBasis.ASSIGNED, java.util.List.of(77L)));
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        BusinessTime time = mock(BusinessTime.class);
        when(time.now()).thenReturn(now);

        DoseAdministrationService service = new DoseAdministrationService(
                doses,
                results,
                mock(DoseReviewRepository.class),
                prescriptions,
                mock(MedicationRepository.class),
                mock(MedicationScheduleCalculator.class),
                assignees,
                mock(ResidentMedicationLock.class),
                events,
                time,
                M07TestParameters.defaults(),
                mock(FamilyMedicationService.class),
                mock(com.eldercare.backend.medication.internal.schedule.service.AbsenceScheduleContexts.class));
        String doseKey = MedicationScheduleCalculator.doseKey(10L, scheduled);

        service.administerDose(doseKey, request(now, null), 77L, 11L, now);

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue()).isInstanceOfSatisfying(MedicationBilledEvent.class, billed -> {
            assertThat(billed.source()).isEqualTo(BilledSource.DOSE);
            assertThat(billed.residentId()).isEqualTo(201L);
            assertThat(billed.medicationId()).isEqualTo(301L);
            assertThat(billed.quantity()).isEqualByComparingTo("2.00");
            assertThat(billed.usageUnit()).isEqualTo("tablet");
        });

        clearInvocations(events);
        service.administerDose(doseKey, request(now, 901L), 77L, 11L, now);
        verifyNoInteractions(events);
    }

    private static AdministerDoseRequest request(Instant administeredAt, Long leaveHandoverId) {
        return new AdministerDoseRequest(
                administeredAt, null, null, null, null, null, null, null, false, leaveHandoverId);
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
