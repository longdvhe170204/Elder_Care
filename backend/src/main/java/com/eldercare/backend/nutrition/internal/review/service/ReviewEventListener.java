package com.eldercare.backend.nutrition.internal.review.service;

import com.eldercare.backend.care.api.events.ProlongedPoorEating;
import com.eldercare.backend.incident.api.events.WeightLossDetected;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Nghe ăn kém kéo dài (005) và sụt cân (007) sau commit của lệnh nguồn, mỗi sự kiện một transaction mới (FR-058). Idempotent
 * theo khóa sự kiện nhờ mốc kỹ thuật của {@link ReviewNoticeService}; sự kiện giao lại không báo lần hai.
 */
@Component
class ReviewEventListener {

    private final ReviewNoticeService review;

    ReviewEventListener(ReviewNoticeService review) {
        this.review = review;
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(ProlongedPoorEating event) {
        review.onProlongedPoorEating(event);
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(WeightLossDetected event) {
        review.onWeightLossDetected(event);
    }
}
