package com.eldercare.backend.medication.internal.service.response;

import com.eldercare.backend.medication.internal.service.PrescriptionCheckResult;
import java.util.List;

/**
 * Pre-validation check response for new prescription (POST /nguoi-cao-tuoi/{nctId}/don-thuoc/kiem-tra; api-006.yaml).
 */
public record PrescriptionCheckResponse(
        List<PrescriptionIssueResponse> issues, List<PrescriptionIssueResponse> reviewItems) {

    public static PrescriptionCheckResponse from(PrescriptionCheckResult result) {
        List<PrescriptionIssueResponse> issues =
                result.issues().stream().map(PrescriptionIssueResponse::from).toList();
        List<PrescriptionIssueResponse> review = result.reviewItems().stream()
                .map(PrescriptionIssueResponse::from)
                .toList();
        return new PrescriptionCheckResponse(issues, review);
    }

    // Compatibility accessors
    public List<PrescriptionIssueResponse> vanDe() {
        return issues;
    }

    public List<PrescriptionIssueResponse> canXemXet() {
        return reviewItems;
    }
}
