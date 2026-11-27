package com.eldercare.backend.care.web.response;

import com.eldercare.backend.care.internal.careplan.domain.CarePlanReviewRaised;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Yêu cầu xem xét kế hoạch Đang mở (FR-058, Q-448). */
@Schema(name = "CarePlanReview")
public record CarePlanReviewResponse(String source, long sourceId, String reference, Instant raisedAt) {
    public static CarePlanReviewResponse of(CarePlanReviewRaised raised) {
        return new CarePlanReviewResponse(raised.source(), raised.sourceId(), raised.reference(), raised.raisedAt());
    }
}
