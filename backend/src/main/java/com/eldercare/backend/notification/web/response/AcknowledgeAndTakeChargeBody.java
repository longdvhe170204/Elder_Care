package com.eldercare.backend.notification.web.response;

import com.eldercare.backend.notification.internal.urgent.service.UrgentTakeCharge;
import java.util.Map;

/** Phản hồi "Xác nhận và tiếp nhận" theo api-009 (FR-026a): xác nhận luôn được ghi; tiếp nhận thành công hoặc bị 007 từ chối. */
public record AcknowledgeAndTakeChargeBody(Acknowledgement acknowledgement, TakeCharge takeCharge) {

    public enum Acknowledgement {
        ACKNOWLEDGED
    }

    public enum TakeChargeResult {
        SUCCEEDED,
        REJECTED
    }

    public record TakeCharge(TakeChargeResult result, String errorCode, Map<String, Object> details) {}

    public static AcknowledgeAndTakeChargeBody of(UrgentTakeCharge.Result r) {
        return new AcknowledgeAndTakeChargeBody(
                Acknowledgement.ACKNOWLEDGED,
                r.succeeded()
                        ? new TakeCharge(TakeChargeResult.SUCCEEDED, null, null)
                        : new TakeCharge(TakeChargeResult.REJECTED, r.errorCode(), r.details()));
    }
}
