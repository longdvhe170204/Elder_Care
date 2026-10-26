package com.cambae.backend.m07.web.response;

import com.cambae.backend.m07.internal.service.request.CreatePrescriptionBody;
import com.cambae.backend.m07.internal.service.response.ReconciliationItemResponse;
import com.cambae.backend.m07.internal.service.response.ReconciliationResponse;
import com.cambae.backend.m07.web.request.NewPrescriptionRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

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
            List<String> allowedCommands) {

        public static ReconciliationBody of(ReconciliationResponse r) {
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
                    r.getPermittedActions());
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
