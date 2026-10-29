package com.eldercare.backend.medication.web.response;

import com.eldercare.backend.medication.internal.service.response.PrescriptionCheckResponse;
import com.eldercare.backend.medication.internal.service.response.PrescriptionIssueResponse;
import com.eldercare.backend.medication.internal.service.response.PrescriptionResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Phản hồi đơn thuốc theo api-006 ({@code Prescription}, {@code Issue}; FR-004 → FR-017). */
public final class PrescriptionBodies {

    private PrescriptionBodies() {}

    /** api-006 {@code Issue}. */
    public record IssueBody(
            String issueCode,

            @Schema(allowableValues = {"DUPLICATE_ACTIVE_INGREDIENT", "ALLERGY_CONFLICT", "OTHER_ALLERGY_REVIEW"})
            String issueType,

            String activeIngredient,
            Long duplicatePrescriptionId,
            Long allergyItemId,
            String severity,
            Boolean notAutoChecked,
            Integer lineNumber) {

        public static IssueBody of(PrescriptionIssueResponse i) {
            return new IssueBody(
                    i.issueCode(),
                    i.issueType(),
                    i.activeIngredient(),
                    i.duplicatePrescriptionId(),
                    i.allergyItemId(),
                    i.severity(),
                    i.manualReviewRequired(),
                    i.lineNumber());
        }

        static List<IssueBody> all(List<PrescriptionIssueResponse> issues) {
            return issues == null
                    ? List.of()
                    : issues.stream().map(IssueBody::of).toList();
        }
    }

    /** api-006 {@code Frequency} trong phản hồi. */
    public record FrequencyBody(
            @Schema(
                    requiredMode = RequiredMode.REQUIRED,
                    allowableValues = {"TIMED_SLOTS", "EVERY_N_HOURS", "DAYS_OF_WEEK"})
            String type,

            List<String> timedSlots,
            Integer everyNHours,
            Instant firstSlot,
            List<Integer> daysOfWeek) {}

    /** api-006 {@code Prescriber} trong phản hồi. */
    public record PrescriberBody(
            @Schema(
                    requiredMode = RequiredMode.REQUIRED,
                    allowableValues = {"INTERNAL", "EXTERNAL"})
            String type,

            String facilityName,
            String prescriberName,
            LocalDate prescribedAt,
            Long prescriptionFileId) {}

    /** Lý do đã xác nhận một vấn đề, như {@code issueConfirmations} của {@code NewPrescription}. */
    public record IssueConfirmationBody(
            @Schema(requiredMode = RequiredMode.REQUIRED) String issueCode,
            @Schema(requiredMode = RequiredMode.REQUIRED) String reason) {}

    /** api-006 {@code Prescription} ({@code NewPrescription} cùng các trường của đơn đã lưu). */
    public record PrescriptionBody(
            Long id,
            Long residentId,
            @Schema(requiredMode = RequiredMode.REQUIRED) Long medicationId,
            @Schema(requiredMode = RequiredMode.REQUIRED) BigDecimal dosage,
            @Schema(requiredMode = RequiredMode.REQUIRED) String route,

            @Schema(
                    requiredMode = RequiredMode.REQUIRED,
                    allowableValues = {"PERIODIC", "PRN"})
            String prescriptionType,

            FrequencyBody frequency,
            Integer minIntervalMinutes,
            Integer maxDosesPer24h,
            @Schema(requiredMode = RequiredMode.REQUIRED) Instant startedAt,
            LocalDate endedAt,

            @Schema(
                    requiredMode = RequiredMode.REQUIRED,
                    allowableValues = {"FACILITY", "FAMILY_SENT"})
            String medicationSource,

            @Schema(requiredMode = RequiredMode.REQUIRED) PrescriberBody prescriber,
            String instructionNote,
            List<IssueConfirmationBody> issueConfirmations,

            @Schema(allowableValues = {"PENDING_EFFECTIVE", "ACTIVE", "SUSPENDED", "DISCONTINUED", "EXPIRED"})
            String status,

            @Schema(allowableValues = {"ORDER", "RECONCILIATION_PENDING"})
            String suspensionReason,

            Boolean pendingAdmission,
            Instant suspendedFrom,
            Long replacesId,
            Long replacedById,
            Boolean noMedicationAvailable,
            List<IssueBody> confirmedIssues,
            List<Long> boundBatchIds,
            List<String> allowedCommands,
            List<String> hiddenFields) {

        public static PrescriptionBody of(PrescriptionResponse p) {
            PrescriptionResponse.FrequencyResponse f = p.frequency();
            PrescriptionResponse.PrescriberResponse pr = p.prescriber();
            return new PrescriptionBody(
                    p.id(),
                    p.residentId(),
                    p.medicationId(),
                    p.dosage(),
                    p.route(),
                    p.prescriptionType(),
                    f == null
                            ? null
                            : new FrequencyBody(
                                    f.type(), f.timedSlots(), f.everyNHours(), f.firstSlot(), f.daysOfWeek()),
                    p.minIntervalMinutes(),
                    p.maxDosesPer24h(),
                    p.startedAt(),
                    p.endedAt(),
                    p.medicationSource(),
                    pr == null
                            ? null
                            : new PrescriberBody(
                                    pr.type(),
                                    pr.facilityName(),
                                    pr.prescriberName(),
                                    pr.prescribedAt(),
                                    pr.prescriptionFileId()),
                    p.instructionNote(),
                    // lý do đã xác nhận nằm ở confirmedIssues; thân ghi mới dùng issueConfirmations
                    List.of(),
                    p.status(),
                    p.suspensionReason(),
                    p.pendingAdmission(),
                    p.suspendedFrom(),
                    p.replacesId(),
                    p.replacedById(),
                    p.noMedicationAvailable(),
                    IssueBody.all(p.confirmedIssues()),
                    p.boundBatchIds(),
                    p.allowedCommands(),
                    p.hiddenFields());
        }
    }

    /** Kết quả xem trước kiểm tra trùng hoạt chất, dị ứng (FR-007, FR-008). */
    public record CheckBody(List<IssueBody> issues, List<IssueBody> reviewItems) {

        public static CheckBody of(PrescriptionCheckResponse r) {
            return new CheckBody(IssueBody.all(r.issues()), IssueBody.all(r.reviewItems()));
        }
    }

    /** Phản hồi Ngừng đơn: cảnh báo khoảng không có liều (BR-M07-15). */
    public record DiscontinueBody(List<Warning> warnings) {}

    /** Một cảnh báo không chặn, ví dụ {@code NO_DOSE_GAP}. */
    public record Warning(String code) {}
}
