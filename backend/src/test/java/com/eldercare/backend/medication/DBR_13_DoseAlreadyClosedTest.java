package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.dto.AdministerDoseRequest;
import com.eldercare.backend.medication.api.dto.RefuseDoseRequest;
import com.eldercare.backend.medication.api.dto.SkipDoseRequest;
import com.eldercare.backend.medication.internal.catalog.persistence.MedicationRepository;
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
import com.eldercare.backend.medication.internal.schedule.service.AbsenceScheduleContexts;
import com.eldercare.backend.medication.internal.schedule.service.MedicationScheduleCalculator;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

/**
 * DBR-13, quickstart Q8 (006 T070): lệnh thứ hai trên liều đã đóng trả 409 {@code DOSE_ALREADY_CLOSED} kèm người đóng,
 * thời điểm, trạng thái hiện hành ({@code currentStatus}) và nội dung bị từ chối để chuyển thành đính chính.
 */
class DBR_13_DoseAlreadyClosedTest {

    private static final Instant SCHEDULED = Instant.parse("2026-10-28T01:00:00Z");
    private static final Instant CLOSED_AT = Instant.parse("2026-10-28T01:10:00Z");
    private static final Instant NOW = Instant.parse("2026-10-28T01:12:00Z");

    private final MedicationDoseRepository doses = mock(MedicationDoseRepository.class);
    private final DoseResultRepository results = mock(DoseResultRepository.class);
    private final DoseAdministrationService service = service();
    private final String doseKey = MedicationScheduleCalculator.doseKey(10L, SCHEDULED);

    @Test
    void DBR_13_administerOnAdministeredDose_returnsClosedByClosedAtCurrentStatusAndRejectedContent() {
        BusinessException error = catchThrowableOfType(
                BusinessException.class,
                () -> service.administerDose(
                        doseKey,
                        new AdministerDoseRequest(
                                CLOSED_AT.plusSeconds(60),
                                null,
                                null,
                                null,
                                null,
                                null,
                                "nổi mẩn",
                                "lần 2",
                                false,
                                null),
                        88L,
                        null,
                        NOW));

        assertClosed(error);
        assertThat(rejected(error))
                .containsEntry("administeredAt", CLOSED_AT.plusSeconds(60))
                .containsEntry("adverseReaction", "nổi mẩn")
                .containsEntry("note", "lần 2")
                .containsEntry("recordedAfterDisconnect", false);
    }

    @Test
    void DBR_13_refuseAndSkipOnClosedDose_keepReasonAndNote() {
        BusinessException refused = catchThrowableOfType(
                BusinessException.class,
                () -> service.refuseDose(
                        doseKey, new RefuseDoseRequest("không chịu uống", "ghi chú", null, false), 88L, null, NOW));
        assertClosed(refused);
        assertThat(rejected(refused)).containsEntry("reason", "không chịu uống").containsEntry("note", "ghi chú");

        BusinessException skipped = catchThrowableOfType(
                BusinessException.class,
                () -> service.skipDose(doseKey, new SkipDoseRequest("đang ngủ", null, null, false), 88L, null, NOW));
        assertClosed(skipped);
        assertThat(rejected(skipped)).containsEntry("reason", "đang ngủ").doesNotContainKey("note");
    }

    private void assertClosed(BusinessException error) {
        assertThat(error).isNotNull();
        assertThat(error.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(error.errorCode()).isEqualTo("DOSE_ALREADY_CLOSED");
        assertThat(error.properties())
                .containsEntry("closedBy", "77")
                .containsEntry("closedAt", CLOSED_AT)
                .containsEntry("currentStatus", "ADMINISTERED")
                .doesNotContainKey("status");
        verify(doses, never()).save(any());
        verify(results, never()).save(any());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> rejected(BusinessException error) {
        return (Map<String, Object>) error.properties().get("rejectedContent");
    }

    private DoseAdministrationService service() {
        MedicationDose dose = mock(MedicationDose.class);
        when(dose.getId()).thenReturn(101L);
        when(dose.getPrescriptionId()).thenReturn(10L);
        when(dose.getResidentId()).thenReturn(201L);
        when(dose.getScheduledTime()).thenReturn(SCHEDULED);
        when(dose.getStatus()).thenReturn(DoseStatus.ADMINISTERED);
        when(dose.getClosedById()).thenReturn(77L);
        when(dose.getClosedByType()).thenReturn("EMPLOYEE");
        when(dose.getClosedAt()).thenReturn(CLOSED_AT);
        when(doses.findByPrescriptionIdAndScheduledTime(10L, SCHEDULED)).thenReturn(Optional.of(dose));
        Prescription prescription = mock(Prescription.class);
        when(prescription.getResidentId()).thenReturn(201L);
        PrescriptionRepository prescriptions = mock(PrescriptionRepository.class);
        when(prescriptions.findById(10L)).thenReturn(Optional.of(prescription));
        BusinessTime time = mock(BusinessTime.class);
        when(time.now()).thenReturn(NOW);
        return new DoseAdministrationService(
                doses,
                results,
                mock(DoseReviewRepository.class),
                prescriptions,
                mock(MedicationRepository.class),
                mock(MedicationScheduleCalculator.class),
                mock(DoseAssigneeResolver.class),
                mock(ResidentMedicationLock.class),
                mock(ApplicationEventPublisher.class),
                time,
                M07TestParameters.defaults(),
                mock(FamilyMedicationService.class),
                mock(AbsenceScheduleContexts.class));
    }
}
