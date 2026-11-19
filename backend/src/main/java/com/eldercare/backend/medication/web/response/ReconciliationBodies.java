package com.eldercare.backend.medication.web.response;

import com.eldercare.backend.medication.internal.prescription.domain.CreatePrescriptionBody;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationCorrectionResponse;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationItemResponse;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationResponse;
import com.eldercare.backend.medication.internal.reconciliation.service.ReconciliationWorklist;
import com.eldercare.backend.medication.web.request.NewPrescriptionRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Phản hồi đối chiếu thuốc theo api-006 ({@code Reconciliation}, {@code ReconciliationItem}; FR-034 → FR-038). */
public final class ReconciliationBodies {

    private ReconciliationBodies() {}

    public record ReconciliationItemBody(
            Long id,

            @Schema(
                    requiredMode = Schema.RequiredMode.REQUIRED,
                    allowableValues = {"CURRENT_PRESCRIPTION", "DISCHARGE_PRESCRIPTION", "ADMISSION_CURRENT_MEDICATION"
                    })
            String source,

            Long prescriptionId,
            Long admissionMedicationItemId,
            String medicationDescription,

            @Schema(allowableValues = {"CONTINUE", "DISCONTINUE", "CHANGE_DOSE", "ADD_NEW"})
            String decision,

            String reason,
            NewPrescriptionRequest newPrescription,
            Boolean discontinuedOutsideTicket,

            @Schema(description = "Tính lúc đọc (BR-M07-22)")
            Boolean medicationRemovedFromProfile,

            Long createdPrescriptionId) {

        static ReconciliationItemBody of(ReconciliationItemResponse i) {
            return new ReconciliationItemBody(
                    i.getId(),
                    i.getSource(),
                    i.getPrescriptionId(),
                    i.getAdmissionMedicationItemId(),
                    i.getMedicationDescription(),
                    i.getDecision(),
                    i.getReason(),
                    ReconciliationBodies.newPrescription(i.getNewPrescription()),
                    i.getDiscontinuedOutsideTicket(),
                    i.getMedicationRemovedFromProfile(),
                    i.getCreatedPrescriptionId());
        }
    }

    /** api-006 {@code Reconciliation}. */
    public record ReconciliationBody(
            Long id,
            Long residentId,

            @Schema(allowableValues = {"ADMISSION", "HOSPITAL_RETURN", "PROFESSIONAL_REQUEST"})
            String reason,

            String reasonDetail,

            @Schema(allowableValues = {"OPEN", "PENDING_CONFIRMATION", "CONFIRMED", "CANCELLED"})
            String status,

            Instant deadlineAt,
            Boolean overdue,
            String createdBy,
            String submittedBy,
            String confirmedBy,
            List<ReconciliationItemBody> items,
            List<String> allowedCommands,

            @Schema(description = "Đề nghị đính chính của phiếu, mới nhất trước (FR-039; bổ sung 2026-11-19, 006 T076)")
            List<ReconciliationCorrectionBody> corrections) {

        public static ReconciliationBody of(ReconciliationResponse r) {
            return of(r, null);
        }

        /** viewerId: người xem, để tính lệnh của từng đề nghị (người lập không tự xác nhận, BR-M07-17). */
        public static ReconciliationBody of(ReconciliationResponse r, Long viewerId) {
            return new ReconciliationBody(
                    r.getId(),
                    r.getResidentId(),
                    r.getReason(),
                    r.getReasonDetail(),
                    r.getStatus(),
                    r.getDeadlineAt(),
                    r.getOverdue(),
                    r.getCreatedBy(),
                    r.getSubmittedBy(),
                    r.getConfirmedBy(),
                    r.getItems() == null
                            ? List.of()
                            : r.getItems().stream()
                                    .map(ReconciliationItemBody::of)
                                    .toList(),
                    r.getPermittedActions(),
                    r.getCorrections().stream()
                            .map(c -> ReconciliationCorrectionBody.of(c, viewerId))
                            .toList());
        }
    }

    /** api-006 {@code ReconciliationCorrection} (FR-039; bổ sung 2026-11-19, 006 T076). */
    public record ReconciliationCorrectionBody(
            Long id,
            Long reconciliationId,
            Map<String, Object> correctedContent,
            String reason,

            @Schema(allowableValues = {"PENDING_CONFIRMATION", "CONFIRMED", "REJECTED"})
            String status,

            String createdBy,
            Instant createdAt,
            String confirmedBy,
            Instant confirmedAt,
            String returnReason,

            @Schema(allowableValues = {"CONFIRM", "RETURN"}) List<String> allowedCommands) {

        public static ReconciliationCorrectionBody of(ReconciliationCorrectionResponse c, Long viewerId) {
            boolean decidable = "PENDING_CONFIRMATION".equals(c.getStatus())
                    && viewerId != null
                    && !viewerId.equals(c.getCreatedById());
            return new ReconciliationCorrectionBody(
                    c.getId(),
                    c.getReconciliationId(),
                    c.getCorrectedContent(),
                    c.getReason(),
                    c.getStatus(),
                    c.getCreatedById() == null ? null : String.valueOf(c.getCreatedById()),
                    c.getCreatedAt(),
                    c.getConfirmedById() == null ? null : String.valueOf(c.getConfirmedById()),
                    c.getConfirmedAt(),
                    c.getReturnReason(),
                    decidable ? List.of("CONFIRM", "RETURN") : List.of());
        }
    }

    /** api-006 {@code ReconciliationWorklistEntry} (bổ sung 2026-11-19, 006 T075, T076). */
    public record ReconciliationWorklistEntryBody(
            Long residentId,
            Long reconciliationId,

            @Schema(allowableValues = {"OPEN", "PENDING_CONFIRMATION"})
            String status,

            @Schema(allowableValues = {"ADMISSION", "HOSPITAL_RETURN", "PROFESSIONAL_REQUEST"})
            String reason,

            Instant deadlineAt,
            Boolean overdue,
            Integer awaitingPrescriptionCount) {

        public static ReconciliationWorklistEntryBody of(ReconciliationWorklist.Entry e) {
            return new ReconciliationWorklistEntryBody(
                    e.residentId(),
                    e.reconciliationId(),
                    e.status(),
                    e.reason(),
                    e.deadlineAt(),
                    e.overdue(),
                    e.awaitingPrescriptionCount());
        }
    }

    /** Đơn mới ghi trên dòng (dạng {@code NewPrescription}). */
    static NewPrescriptionRequest newPrescription(CreatePrescriptionBody b) {
        if (b == null) {
            return null;
        }
        var f = b.getFrequency();
        var p = b.getPrescriber();
        return new NewPrescriptionRequest(
                b.getMedicationId(),
                b.getDosage(),
                b.getRoute(),
                b.getPrescriptionType() == null
                        ? null
                        : NewPrescriptionRequest.PrescriptionType.valueOf(b.getPrescriptionType()),
                f == null || f.getType() == null
                        ? null
                        : new NewPrescriptionRequest.PrescriptionFrequency(
                                NewPrescriptionRequest.FrequencyType.valueOf(f.getType()),
                                f.getTimedSlots(),
                                f.getEveryNHours(),
                                f.getFirstSlot(),
                                f.getDaysOfWeek()),
                b.getMinIntervalMinutes(),
                b.getMaxDosesPer24h(),
                b.getStartedAt(),
                b.getEndedAt(),
                b.getMedicationSource() == null
                        ? null
                        : NewPrescriptionRequest.MedicationSource.valueOf(b.getMedicationSource()),
                p == null || p.getType() == null
                        ? null
                        : new NewPrescriptionRequest.PrescriberInput(
                                NewPrescriptionRequest.PrescriberType.valueOf(p.getType()),
                                p.getFacilityName(),
                                p.getPrescriberName(),
                                p.getPrescribedAt(),
                                p.getPrescriptionFileId()),
                b.getInstructionNote(),
                b.getConfirmedIssues() == null
                        ? null
                        : b.getConfirmedIssues().stream()
                                .map(c -> new NewPrescriptionRequest.IssueConfirmationInput(
                                        c.getIssueCode(), c.getReason()))
                                .toList());
    }
}
