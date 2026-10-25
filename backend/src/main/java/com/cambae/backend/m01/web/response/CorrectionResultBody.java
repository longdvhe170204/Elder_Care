package com.cambae.backend.m01.web.response;

import com.cambae.backend.m15.correction.api.CorrectionResult;
import com.cambae.backend.m15.correction.api.CreationMethod;
import io.swagger.v3.oas.annotations.media.Schema;

/** Kết quả đính chính (contracts/api-001.yaml, schema {@code CorrectionResult}): đã lưu hoặc chờ duyệt (cách C). */
@Schema(name = "CorrectionResult")
public record CorrectionResultBody(Method creationMethod, Long correctionId, Long approvalRequestId) {

    public enum Method {
        A_ORIGINAL_AUTHOR,
        B_FLOOR_LEAD,
        C_APPROVAL_REQUEST
    }

    public static CorrectionResultBody of(CorrectionResult result) {
        return switch (result) {
            case CorrectionResult.Saved saved ->
                new CorrectionResultBody(
                        method(saved.correction().creationMethod()),
                        saved.correction().id(),
                        saved.correction().approvalRequestId());
            case CorrectionResult.PendingApproval pending ->
                new CorrectionResultBody(
                        Method.C_APPROVAL_REQUEST, null, pending.request().id());
        };
    }

    private static Method method(CreationMethod method) {
        return switch (method) {
            case A -> Method.A_ORIGINAL_AUTHOR;
            case B -> Method.B_FLOOR_LEAD;
            case C -> Method.C_APPROVAL_REQUEST;
        };
    }
}
