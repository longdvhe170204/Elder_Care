package com.eldercare.backend.nutrition.internal.familyfood.service;

import com.eldercare.backend.nutrition.internal.schedule.service.M08MinuteTask;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Đồ ăn gia đình Được sử dụng tới hạn dùng thì chuyển Không sử dụng, báo Điều dưỡng phụ trách mức Nhẹ (FR-055). Xét mọi đồ
 * ăn đã quá hạn tại thời điểm chạy nên lần chạy bù vẫn làm; đồ ăn đã chuyển không còn Được sử dụng nên không báo lại.
 */
@Component
class FamilyFoodExpiryTask implements M08MinuteTask {

    private static final String KEY = "FAMILY_FOOD_EXPIRY";

    private final FamilyFoodService service;

    FamilyFoodExpiryTask(FamilyFoodService service) {
        this.service = service;
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public void run(Instant now) {
        service.expireDue(now);
    }
}
