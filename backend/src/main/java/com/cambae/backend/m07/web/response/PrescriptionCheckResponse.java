package com.cambae.backend.m07.web.response;

import com.cambae.backend.m07.internal.service.PrescriptionCheckResult;
import java.util.List;

/**
 * Pre-validation check response for new prescription (POST /nguoi-cao-tuoi/{nctId}/don-thuoc/kiem-tra; api-006.yaml).
 */
public record PrescriptionCheckResponse(
        List<PrescriptionIssueResponse> vanDe, List<PrescriptionIssueResponse> canXemXet) {

    public static PrescriptionCheckResponse from(PrescriptionCheckResult result) {
        List<PrescriptionIssueResponse> issues =
                result.issues().stream().map(PrescriptionIssueResponse::from).toList();
        List<PrescriptionIssueResponse> review = result.reviewItems().stream()
                .map(PrescriptionIssueResponse::from)
                .toList();
        return new PrescriptionCheckResponse(issues, review);
    }
}
