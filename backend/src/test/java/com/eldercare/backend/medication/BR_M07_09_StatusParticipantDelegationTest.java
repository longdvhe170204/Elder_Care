package com.eldercare.backend.medication;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.internal.dose.persistence.DoseResultRepository;
import com.eldercare.backend.medication.internal.dose.persistence.MedicationDoseRepository;
import com.eldercare.backend.medication.internal.family.persistence.FamilyMedicationBatchRepository;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationAbsenceIntervalRepository;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationHandoverRepository;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationReason;
import com.eldercare.backend.medication.internal.reconciliation.persistence.MedicationReconciliationRepository;
import com.eldercare.backend.medication.internal.reconciliation.service.ReconciliationDeadlines;
import com.eldercare.backend.medication.internal.status.service.MedicationStatusParticipant;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * BR-M07-09, FR-034, FR-034a (research D-13, D-16): Hoàn tất tiếp nhận và Ghi nhận trở về từ Điều trị tại bệnh viện đi
 * vào hạn đối chiếu; trở về từ Tạm vắng thì không.
 */
class BR_M07_09_StatusParticipantDelegationTest {

    private static final long RESIDENT = 9L;
    private static final Instant AT = Instant.parse("2026-11-08T02:00:00Z");

    private final ReconciliationDeadlines deadlines = mock(ReconciliationDeadlines.class);
    private final MedicationAbsenceIntervalRepository absences = mock(MedicationAbsenceIntervalRepository.class);
    private final BusinessTime time = mock(BusinessTime.class);
    private final MedicationStatusParticipant participant = new MedicationStatusParticipant(
            absences,
            mock(MedicationDoseRepository.class),
            mock(DoseResultRepository.class),
            mock(PrescriptionRepository.class),
            mock(MedicationHandoverRepository.class),
            mock(MedicationReconciliationRepository.class),
            mock(FamilyMedicationBatchRepository.class),
            time,
            deadlines);

    private TransitionResult result(StatusCommand command, ResidentStatus from) {
        return new TransitionResult(
                RESIDENT,
                from,
                ResidentStatus.IN_RESIDENCE,
                command,
                1L,
                AT,
                JsonMapper.builder().build().createObjectNode());
    }

    @Test
    void FR_034a_completeAdmissionHandsOverToTheDeadlines() {
        when(time.now()).thenReturn(AT);

        participant.applyEffects(result(StatusCommand.COMPLETE_ADMISSION, ResidentStatus.ADMISSION_IN_PROGRESS));

        verify(deadlines).completeAdmission(RESIDENT, AT);
    }

    @Test
    void FR_034_returnFromHospitalStartsReconciliationButReturnFromLeaveDoesNot() {
        when(time.now()).thenReturn(AT);

        participant.applyEffects(result(StatusCommand.RECORD_RETURN, ResidentStatus.HOSPITALIZED));
        verify(deadlines).begin(RESIDENT, ReconciliationReason.HOSPITAL_RETURN, AT);

        org.mockito.Mockito.clearInvocations(deadlines);
        participant.applyEffects(result(StatusCommand.RECORD_RETURN, ResidentStatus.TEMPORARILY_ABSENT));
        verify(deadlines, never()).begin(any(Long.class), any(), any());
    }
}
