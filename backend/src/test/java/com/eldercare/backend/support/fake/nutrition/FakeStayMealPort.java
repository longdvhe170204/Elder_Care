package com.eldercare.backend.support.fake.nutrition;

import com.eldercare.backend.nutrition.api.dto.StayMealView;
import com.eldercare.backend.nutrition.api.spi.StayMealPort;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Bản giả {@link StayMealPort} (012 cài): trả lượt ở lại đã đặt, đang diễn ra tại thời điểm hỏi và có đăng ký ăn. */
public class FakeStayMealPort implements StayMealPort {

    private final List<StayMealView> stays = new ArrayList<>();

    public FakeStayMealPort stay(StayMealView stay) {
        stays.add(stay);
        return this;
    }

    @Override
    public List<StayMealView> staysWithMealRegistration(Instant t) {
        return stays.stream()
                .filter(s -> s.mealRegistered() && !t.isBefore(s.from()) && t.isBefore(s.to()))
                .toList();
    }
}
