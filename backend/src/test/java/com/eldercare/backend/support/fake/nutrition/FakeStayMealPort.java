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

    /** Kết thúc sớm hoặc hủy lượt ở lại: lượt còn tới {@code to} (bằng {@code from} là hủy). */
    public FakeStayMealPort end(long stayId, Instant to) {
        stays.replaceAll(s -> s.stayId() == stayId
                ? new StayMealView(s.stayId(), s.residentId(), s.familyMemberId(), s.from(), to, s.mealRegistered())
                : s);
        return this;
    }

    @Override
    public List<StayMealView> staysWithMealRegistration(Instant t) {
        return stays.stream()
                .filter(s -> s.mealRegistered() && !t.isBefore(s.from()) && t.isBefore(s.to()))
                .toList();
    }
}
