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
 * khi phát lại), như listener đơn thuốc của 006. Đối chiếu thực đơn chưa hết tuần qua {@link AllergyMenuCheck} (Phase 5–6);
 * suất đã chốt tính lại ở listener chốt suất (cả dị ứng loại "khác", FR-036a, và dị ứng Đã loại trừ); đồ ăn gia đình ở
 * phase đồ ăn gia đình.
 */
@Component
class NutritionAllergyListener {

    private final M08EventListener listener;
    private final HealthItemApi healthItems;

    private final AllergyMenuCheck menuCheck;

    NutritionAllergyListener(M08EventListener listener, HealthItemApi healthItems, AllergyMenuCheck menuCheck) {
        this.listener = listener;
        this.healthItems = healthItems;
        this.menuCheck = menuCheck;
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAllergyRecorded(AllergyRecorded event) {
        listener.once("M08.ALLERGY_RECORDED:" + event.itemId(), () -> {
            menuCheck.onAllergyRecorded(event.itemId(), event.residentId());
            healthItems.markChecked(event.itemId(), HealthItemCheckSource.NUTRITION);
        });
    }
}
