package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.internal.dose.domain.DoseCancellationReason;
import com.eldercare.backend.medication.internal.dose.domain.DoseResult;
import com.eldercare.backend.medication.internal.dose.domain.DoseResultType;
import com.eldercare.backend.medication.internal.dose.domain.DoseStatus;
import com.eldercare.backend.medication.internal.dose.domain.MedicationDose;
import com.eldercare.backend.medication.internal.dose.persistence.DoseResultRepository;
import com.eldercare.backend.medication.internal.dose.persistence.MedicationDoseRepository;
import com.eldercare.backend.medication.internal.family.persistence.FamilyMedicationBatchRepository;
import com.eldercare.backend.medication.internal.handover.domain.AbsenceIntervalType;
import com.eldercare.backend.medication.internal.handover.domain.MedicationAbsenceInterval;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationAbsenceIntervalRepository;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationHandoverRepository;
import com.eldercare.backend.medication.internal.prescription.domain.FrequencyType;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriberType;
import com.eldercare.backend.medication.internal.prescription.domain.Prescription;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionStatus;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionType;
import com.eldercare.backend.medication.internal.prescription.domain.TerminationReason;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.reconciliation.domain.MedicationReconciliation;
import com.eldercare.backend.medication.internal.reconciliation.persistence.MedicationReconciliationRepository;
import com.eldercare.backend.medication.internal.status.service.MedicationStatusParticipant;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * T042 (research D-15, D-16; FR-021, FR-047, FR-048, FR-048b, BR-M07-03): tác động của 006 trong lệnh trạng thái của
 * 001: khoảng vắng mở theo lệnh, trở về khôi phục liều và chờ nhận lại thuốc mang theo, trạng thái cuối ngừng đơn và
 * đóng liều, Kết thúc lưu trú chờ thuốc gia đình gửi.
 */
class MedicationStatusParticipantTest {

    private static final long RESIDENT = 9L;
    private static final Instant AT = Instant.parse("2026-11-08T02:00:00Z");

    private final MedicationAbsenceIntervalRepository absences = mock(MedicationAbsenceIntervalRepository.class);
    private final MedicationDoseRepository doses = mock(MedicationDoseRepository.class);
    private final DoseResultRepository results = mock(DoseResultRepository.class);
    private final PrescriptionRepository prescriptions = mock(PrescriptionRepository.class);
    private final MedicationHandoverRepository handovers = mock(MedicationHandoverRepository.class);
    private final MedicationReconciliationRepository tickets = mock(MedicationReconciliationRepository.class);
    private final FamilyMedicationBatchRepository batches = mock(FamilyMedicationBatchRepository.class);
    private final BusinessTime time = mock(BusinessTime.class);
    private final MedicationStatusParticipant participant = new MedicationStatusParticipant(
            absences,
            doses,
            results,
            prescriptions,
            handovers,
            tickets,
            batches,
            time,
            mock(com.eldercare.backend.medication.internal.reconciliation.service.ReconciliationDeadlines.class));

    private static ObjectNode details() {
        return JsonMapper.builder().build().createObjectNode();
    }

    private static TransitionResult result(StatusCommand command, ResidentStatus from, ObjectNode details) {
        return new TransitionResult(RESIDENT, from, ResidentStatus.IN_RESIDENCE, command, 1L, AT, details);
    }

    private static Prescription prescription(long id, PrescriptionStatus status) {
        Prescription p = new Prescription(
                RESIDENT,
                100L,
                new BigDecimal("1.00"),
                "viên",
                "uống",
                PrescriptionType.PERIODIC,
                FrequencyType.TIMED_SLOTS,
                "[\"08:00\"]",
                null,
                null,
                null,
                null,
                null,
                AT.minus(10, ChronoUnit.DAYS),
                null,
                MedicationSource.FACILITY,
                PrescriberType.INTERNAL,
                null,
                null,
                10L,
                null,
                null,
                null,
                status,
                false,
                null,
                null,
                null,
                AT,
                Set.of(),
                AT);
        setField(p, Prescription.class, "id", id);
        return p;
    }

    private static MedicationDose dose(long prescriptionId, DoseStatus status, Instant windowTo) {
        MedicationDose d = new MedicationDose(
                prescriptionId,
                RESIDENT,
                100L,
                windowTo.minus(30, ChronoUnit.MINUTES),
                windowTo.minus(60, ChronoUnit.MINUTES),
                windowTo,
                new BigDecimal("1.00"),
                "viên",
                MedicationSource.FACILITY,
                false,
                status,
                3L,
                AT);
        setField(d, MedicationDose.class, "id", prescriptionId * 10);
        return d;
    }

    private static void setField(Object target, Class<?> type, String name, Object value) {
        try {
            var field = type.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void BR_28_temporaryAbsenceOpensIntervalByTakesMedication() {
        when(absences.findOpenByResidentId(RESIDENT)).thenReturn(Optional.empty());
        ObjectNode details = details()
                .put("takesMedication", false)
                .put("expectedReturnAt", AT.plus(6, ChronoUnit.HOURS).toString());

        participant.applyEffects(result(StatusCommand.GRANT_TEMPORARY_ABSENCE, ResidentStatus.IN_RESIDENCE, details));

        ArgumentCaptor<MedicationAbsenceInterval> saved = ArgumentCaptor.forClass(MedicationAbsenceInterval.class);
        verify(absences).save(saved.capture());
        assertThat(saved.getValue().getAbsenceType()).isEqualTo(AbsenceIntervalType.TEMPORARY_LEAVE_NO_MED);
        assertThat(saved.getValue().getFromTime()).isEqualTo(AT);
        assertThat(saved.getValue().getExpectedToTime()).isEqualTo(AT.plus(6, ChronoUnit.HOURS));
    }

    @Test
    void FR_047_finalStatusStopsPrescriptionsAndClosesOpenDoses() {
        when(time.now()).thenReturn(AT);
        Prescription active = prescription(1L, PrescriptionStatus.ACTIVE);
        Prescription pending = prescription(2L, PrescriptionStatus.PENDING_EFFECTIVE);
        when(prescriptions.findByResidentIdAndStatusIn(eq(RESIDENT), anyCollection()))
                .thenReturn(List.of(active, pending));
        MedicationDose suspended = dose(1L, DoseStatus.SUSPENDED, AT.plus(1, ChronoUnit.HOURS));
        MedicationDose due = dose(3L, DoseStatus.DUE, AT.plus(1, ChronoUnit.HOURS));
        when(doses.findByResidentIdAndStatusIn(eq(RESIDENT), anyCollection())).thenReturn(List.of(suspended, due));
        MedicationReconciliation open = mock(MedicationReconciliation.class);
        when(tickets.findByResidentIdAndStatusIn(eq(RESIDENT), anyCollection())).thenReturn(List.of(open));

        participant.applyEffects(result(StatusCommand.RECORD_DEATH, ResidentStatus.IN_RESIDENCE, details()));

        assertThat(active.getStatus()).isEqualTo(PrescriptionStatus.DISCONTINUED);
        assertThat(active.getTerminationReason()).isEqualTo(TerminationReason.FINAL_STATE);
        assertThat(pending.getStatus()).isEqualTo(PrescriptionStatus.DISCONTINUED);
        assertThat(suspended.getStatus()).isEqualTo(DoseStatus.CANCELLED);
        assertThat(suspended.getCancellationReason()).isEqualTo(DoseCancellationReason.FINAL_STATE);
        assertThat(due.getStatus()).isEqualTo(DoseStatus.NOT_ADMINISTERED);
        assertThat(due.getClosedByType()).isEqualTo("SYSTEM");
        ArgumentCaptor<DoseResult> recorded = ArgumentCaptor.forClass(DoseResult.class);
        verify(results).save(recorded.capture());
        assertThat(recorded.getValue().getResultType()).isEqualTo(DoseResultType.NOT_ADMINISTERED);
        verify(open).cancel(any());
    }

    @Test
    void FR_048b_departureOpensTripIntervalUnlessNoMedicationOrderExists() {
        when(absences.findByTripId(77L)).thenReturn(Optional.empty());
        ObjectNode details = details()
                .put("tripId", 77L)
                .put("at", AT.toString())
                .put("expectedReturnAt", AT.plus(4, ChronoUnit.HOURS).toString());

        participant.applyEffects(result(StatusCommand.ROLL_CALL_DEPARTURE, ResidentStatus.IN_RESIDENCE, details));

        ArgumentCaptor<MedicationAbsenceInterval> saved = ArgumentCaptor.forClass(MedicationAbsenceInterval.class);
        verify(absences).save(saved.capture());
        assertThat(saved.getValue().getAbsenceType()).isEqualTo(AbsenceIntervalType.TRIP_WITH_MED);
        assertThat(saved.getValue().getTripId()).isEqualTo(77L);
        assertThat(saved.getValue().getExpectedToTime()).isEqualTo(AT.plus(4, ChronoUnit.HOURS));

        MedicationAbsenceInterval noMedication = new MedicationAbsenceInterval(
                RESIDENT, AbsenceIntervalType.TRIP_NO_MED, AT, AT.plus(4, ChronoUnit.HOURS), null, 78L, AT);
        when(absences.findByTripId(78L)).thenReturn(Optional.of(noMedication));
        participant.applyEffects(
                result(StatusCommand.ROLL_CALL_DEPARTURE, ResidentStatus.IN_RESIDENCE, details.put("tripId", 78L)));
        verify(absences, never()).save(noMedication);
    }
}
