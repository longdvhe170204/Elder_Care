package com.eldercare.backend.medication.internal.prescription.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;
import java.util.Map;

/** Nội dung dòng {@code record_event} loại {@code M07.PRESCRIPTION_ISSUE} (data-model 006 mục 0.1). */
public record PrescriptionIssueContent(
        Long prescriptionId,
        Long reconciliationLineId,
        Integer lineNumber,
        PrescriptionIssueType issueType,
        String issueCode,
        Long duplicatePrescriptionId,
        Long allergyItemId,
        String severity,
        String activeIngredient,
        boolean manualReviewRequired,
        String confirmationReason,
        Long confirmedById,
        Instant occurredAt,
        Map<String, Object> legacy) {

    public static final SharedKind<PrescriptionIssueContent> KIND = SharedKind.of(
                    "M07.PRESCRIPTION_ISSUE", PrescriptionIssueContent.class)
            .notDeletable();

    public static PrescriptionIssueContent of(PrescriptionIssue i) {
        return new PrescriptionIssueContent(
                i.getPrescriptionId(),
                i.getReconciliationLineId(),
                i.getLineNumber(),
                i.getIssueType(),
                i.getIssueCode(),
                i.getDuplicatePrescriptionId(),
                i.getAllergyItemId(),
                i.getSeverity(),
                i.getActiveIngredient(),
                i.isManualReviewRequired(),
                i.getConfirmationReason(),
                i.getConfirmedById(),
                i.getOccurredAt(),
                null);
    }

    public PrescriptionIssue toDomain(long id) {
        PrescriptionIssue issue = new PrescriptionIssue(
                prescriptionId,
                reconciliationLineId,
                lineNumber,
                issueType,
                issueCode,
                duplicatePrescriptionId,
                allergyItemId,
                severity,
                activeIngredient,
                manualReviewRequired,
                confirmationReason,
                confirmedById,
                occurredAt,
                occurredAt);
        issue.assignId(id);
        return issue;
    }
}
