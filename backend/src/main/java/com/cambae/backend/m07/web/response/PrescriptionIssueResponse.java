package com.cambae.backend.m07.web.response;

import com.cambae.backend.m07.internal.service.PrescriptionIssueView;

/**
 * Prescription issue response (schema VanDe in api-006.yaml).
 */
public record PrescriptionIssueResponse(
        String issueCode,
        String issueType,
        String activeIngredient,
        Long duplicatePrescriptionId,
        Long allergyItemId,
        String severity,
        boolean manualReviewRequired,
        Integer lineNumber) {

    public static PrescriptionIssueResponse from(PrescriptionIssueView view) {
        return new PrescriptionIssueResponse(
                view.issueCode(),
                view.issueType().name(),
                view.activeIngredient(),
                view.duplicatePrescriptionId(),
                view.allergyItemId(),
                view.severity(),
                view.manualReviewRequired(),
                view.lineNumber());
    }
}
