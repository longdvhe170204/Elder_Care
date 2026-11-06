package com.eldercare.backend.nutrition.internal.conflict.service;

import com.eldercare.backend.resident.api.HealthItemApi;
import com.eldercare.backend.resident.api.dto.HealthItemCheckSource;
import com.eldercare.backend.resident.api.events.AllergyRecorded;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Dị ứng chọn từ danh mục mới ghi nhận (FR-021, BR-M01-12; team-plan mục 8 dòng "D (006, 011)"): đối chiếu với phần của
 * 011 rồi gỡ cờ chờ kiểm tra dinh dưỡng của mục. Idempotent theo {@code itemId} ({@link M08EventListener#once}); job 15
 * phút của 001 phát lại tới khi cờ gỡ. Chạy sau commit trên luồng của lệnh 001 (người thực hiện lệnh, hoặc Bộ lập lịch
 * khi phát lại), như listener đơn thuốc của 006. Thực đơn, đồ ăn gia đình, suất đã chốt chưa có ở phase này nên chưa có
 * gì để đối chiếu; phase thực đơn, chốt suất, đồ ăn gia đình thêm đối chiếu vào {@code work}. Dị ứng loại "khác"
 * (FR-036a) và dị ứng Đã loại trừ (FR-021) chỉ tác động lên thực đơn, suất đã chốt, đồ ăn gia đình nên làm ở các phase đó.
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
