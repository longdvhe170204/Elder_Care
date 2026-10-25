package com.cambae.backend.m02.web.response;

import com.cambae.backend.m15.approval.api.ApprovalRequestView;
import io.swagger.v3.oas.annotations.media.Schema;

/** Tham chiếu yêu cầu phê duyệt vừa lập (contracts/api-004.yaml, schema {@code ApprovalRequestRef}). */
@Schema(name = "ApprovalRequestRef")
public record ApprovalRequestRefBody(long approvalRequestId, String type, String status) {

    public static ApprovalRequestRefBody of(ApprovalRequestView request) {
        return new ApprovalRequestRefBody(request.id(), request.type(), request.status());
    }
}
