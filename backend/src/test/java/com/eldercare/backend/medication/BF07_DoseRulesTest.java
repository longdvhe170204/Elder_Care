package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.dto.AdministerDoseRequest;
import com.eldercare.backend.medication.api.dto.DoseMeasurement;
import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.api.events.CriticalDoseMissed;
import com.eldercare.backend.medication.internal.catalog.persistence.MedicationRepository;
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
import com.eldercare.backend.medication.internal.prescription.domain.TimeCriticality;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.reconciliation.persistence.ResidentMedicationLock;
import com.eldercare.backend.medication.internal.schedule.persistence.M07JobRunStore;
import com.eldercare.backend.medication.internal.schedule.service.AbsenceScheduleContexts;
import com.eldercare.backend.medication.internal.schedule.service.M07JobService;
import com.eldercare.backend.medication.internal.schedule.service.MedicationScheduleCalculator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

/**
 * Sửa BF-07 (Q-426 → Q-428): Bỏ lỡ theo mức quan trọng của đơn (BR-M07-27), Nhận liều chặn người khác và chỉ số đo
 * trước khi dùng bắt buộc khi đơn yêu cầu (BR-M07-26).
 */
class BF07_DoseRulesTest {

    private static final Instant NOW = Instant.parse("2026-10-28T02:00:00Z");
    private static final Instant SCHEDULED = Instant.parse("2026-10-28T01:30:00Z");

    private static MedicationDose lateDose(Instant lateAt) {
        MedicationDose dose = new MedicationDose(
                10L,
                201L,
                301L,
                SCHEDULED,
                SCHEDULED.minusSeconds(1800),
                SCHEDULED.plusSeconds(1800),
                BigDecimal.ONE,
                "tablet",
                MedicationSource.FACILITY,
                false,
                DoseStatus.DUE,
                11L,
                SCHEDULED.minusSeconds(3600));
        dose.markLate(lateAt);
        setId(dose, 101L);
        return dose;
    }

    private static void setId(Object entity, long id) {
        try {
            java.lang.reflect.Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static Prescription prescription(TimeCriticality criticality, String measurementsJson) {
        Prescription prescription = mock(Prescription.class);
        when(prescription.getResidentId()).thenReturn(201L);
        when(prescription.getTimeCriticality()).thenReturn(criticality);
        when(prescription.getPreDoseMeasurements()).thenReturn(measurementsJson);
        return prescription;
    }

    private record Jobs(M07JobService service, ApplicationEventPublisher events) {}

    private static Jobs jobs(MedicationDose dose, Prescription prescription) {
        MedicationDoseRepository doses = mock(MedicationDoseRepository.class);
        when(doses.findByStatusAndLateAtLessThanEqual(eq(DoseStatus.LATE), any()))
                .thenReturn(List.of(dose));
        PrescriptionRepository prescriptions = mock(PrescriptionRepository.class);
        when(prescriptions.findById(10L)).thenReturn(Optional.of(prescription));
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        BusinessTime time = mock(BusinessTime.class);
        when(time.now()).thenReturn(NOW);
        M07JobService service = new M07JobService(
                mock(M07JobRunStore.class),
                prescriptions,
                mock(MedicationRepository.class),
                doses,
                mock(DoseResultRepository.class),
                mock(MedicationScheduleCalculator.class),
                mock(DoseAdministrationService.class),
                events,
                time,
                M07TestParameters.defaults(),
                mock(com.eldercare.backend.roombedallocation.api.ResidentLocationApi.class),
                mock(com.eldercare.backend.medication.api.spi.NursingShiftPort.class),
                mock(AbsenceScheduleContexts.class),
                mock(com.eldercare.backend.medication.internal.reconciliation.service.ReconciliationDeadlines.class));
        return new Jobs(service, events);
    }

    @Test
    void BR_M07_27_criticalDose_missedAfter15MinutesAndNotifiesFloorAndDoctor() {
        MedicationDose dose = lateDose(NOW.minusSeconds(20 * 60));
        Jobs jobs = jobs(dose, prescription(TimeCriticality.CRITICAL, null));

        jobs.service().runMinuteTasks(NOW);

        assertThat(dose.getStatus()).isEqualTo(DoseStatus.MISSED);
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(jobs.events(), atLeastOnce()).publishEvent(event.capture());
        assertThat(event.getAllValues()).anyMatch(CriticalDoseMissed.class::isInstance);
    }

    @Test
    void BR_M07_02_normalDose_notMissedBefore30Minutes() {
        MedicationDose dose = lateDose(NOW.minusSeconds(20 * 60));
        Jobs jobs = jobs(dose, prescription(TimeCriticality.NORMAL, null));

        jobs.service().runMinuteTasks(NOW);

        assertThat(dose.getStatus()).isEqualTo(DoseStatus.LATE);
        verify(jobs.events(), never()).publishEvent(any(CriticalDoseMissed.class));
    }

    private final MedicationDoseRepository doseRepository = mock(MedicationDoseRepository.class);
    private final DoseResultRepository resultRepository = mock(DoseResultRepository.class);

    private DoseAdministrationService administration(MedicationDose dose, Prescription prescription) {
        when(doseRepository.findByPrescriptionIdAndScheduledTime(10L, SCHEDULED))
                .thenReturn(Optional.of(dose));
        PrescriptionRepository prescriptions = mock(PrescriptionRepository.class);
        when(prescriptions.findById(10L)).thenReturn(Optional.of(prescription));
        DoseAssigneeResolver resolver = mock(DoseAssigneeResolver.class);
        when(resolver.resolve(any(), any(), any(), any()))
                .thenReturn(new DoseAssigneeResolver.DoseAssignee(null, true, null, List.of()));
        BusinessTime time = mock(BusinessTime.class);
        when(time.now()).thenReturn(NOW);
        return new DoseAdministrationService(
                doseRepository,
                resultRepository,
                mock(DoseReviewRepository.class),
                prescriptions,
                mock(MedicationRepository.class),
                mock(MedicationScheduleCalculator.class),
                resolver,
                mock(ResidentMedicationLock.class),
                mock(ApplicationEventPublisher.class),
                time,
                M07TestParameters.defaults(),
                mock(FamilyMedicationService.class),
                mock(AbsenceScheduleContexts.class));
    }

    private static AdministerDoseRequest request(List<DoseMeasurement> measurements, boolean offline) {
        return new AdministerDoseRequest(
                NOW.minusSeconds(60), null, null, null, null, null, null, null, offline, 5L, measurements);
    }

    private final String doseKey = MedicationScheduleCalculator.doseKey(10L, SCHEDULED);

    @Test
    void BR_M07_26_claimedByOtherNurse_blocksAdministerButNotOfflineRecord() {
        MedicationDose dose = lateDose(NOW.minusSeconds(300));
        dose.claim(99L, NOW.plusSeconds(600), NOW.minusSeconds(60));
        DoseAdministrationService service = administration(dose, prescription(TimeCriticality.NORMAL, null));

        BusinessException error = catchThrowableOfType(
                BusinessException.class, () -> service.administerDose(doseKey, request(null, false), 88L, null, NOW));

        assertThat(error).isNotNull();
        assertThat(error.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(error.errorCode()).isEqualTo("DOSE_CLAIMED");
        verify(resultRepository, never()).save(any());

        // Bản ghi ngoại tuyến không biết lượt nhận nên không bị chặn (BR-M07-20)
        service.administerDose(doseKey, request(null, true), 88L, null, NOW);
        verify(resultRepository).save(any(DoseResult.class));
    }

    @Test
    void BR_M07_26_requiredMeasurement_blocksAdministerUntilGiven() {
        MedicationDose dose = lateDose(NOW.minusSeconds(300));
        DoseAdministrationService service =
                administration(dose, prescription(TimeCriticality.NORMAL, "[\"HEART_RATE\"]"));

        ConditionNotMetException error = catchThrowableOfType(
                ConditionNotMetException.class,
                () -> service.administerDose(doseKey, request(null, false), 88L, null, NOW));
        assertThat(error).isNotNull();
        verify(resultRepository, never()).save(any());

        service.administerDose(
                doseKey,
                request(List.of(new DoseMeasurement("HEART_RATE", new BigDecimal("72"))), false),
                88L,
                null,
                NOW);
        ArgumentCaptor<DoseResult> saved = ArgumentCaptor.forClass(DoseResult.class);
        verify(resultRepository).save(saved.capture());
        assertThat(saved.getValue().getPreDoseMeasurements())
                .contains("HEART_RATE")
                .contains("72");
    }
}
