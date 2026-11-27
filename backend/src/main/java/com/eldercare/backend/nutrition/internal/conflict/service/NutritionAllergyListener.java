package com.eldercare.backend.nutrition.internal.conflict.service;

import com.eldercare.backend.resident.api.HealthItemApi;
import com.eldercare.backend.resident.api.dto.HealthItemCheckSource;
import com.eldercare.backend.resident.api.events.AllergyRecorded;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Dị ứng chọn từ danh mục mới ghi nhận (FR-021, BR-M01-12): gỡ cờ chờ kiểm tra dinh dưỡng của mục. Idempotent theo
 * {@code itemId} ({@link M08EventListener#once}); job 15 phút của 001 phát lại tới khi cờ gỡ. Từ Q-445 thực đơn, suất ăn
 * không đối chiếu dị ứng; đồ ăn gia đình đối chiếu ở listener đồ ăn gia đình (FR-056).
 */
@Component
class NutritionAllergyListener {

    private final M08EventListener listener;
    private final HealthItemApi healthItems;

    NutritionAllergyListener(M08EventListener listener, HealthItemApi healthItems) {
        this.listener = listener;
        this.healthItems = healthItems;
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAllergyRecorded(AllergyRecorded event) {
        listener.once(
                "M08.ALLERGY_RECORDED:" + event.itemId(),
                () -> healthItems.markChecked(event.itemId(), HealthItemCheckSource.NUTRITION));
    }
}
