package com.eldercare.backend.support.fake.nutrition;

import com.eldercare.backend.nutrition.api.events.StayMealChanged;
import com.eldercare.backend.nutrition.api.events.TripMealChanged;
import java.time.Instant;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Cách phát sự kiện của 012, 014 trong test của 011: gọi {@link #stayChanged}, {@link #tripChanged} thay cho lệnh thật.
 * Mỗi lần gọi một khóa sự kiện riêng; gọi trong transaction của test nếu listener chạy sau commit.
 */
public class FakeNutritionEvents {

    private final ApplicationEventPublisher publisher;
    private long sequence = 1;

    public FakeNutritionEvents(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public StayMealChanged stayChanged(
            long stayId,
            long residentId,
            long familyMemberId,
            String status,
            boolean mealRegistered,
            Instant from,
            Instant to) {
        StayMealChanged event = new StayMealChanged(
                stayId, residentId, familyMemberId, status, mealRegistered, from, to, "FAKE-STAY-" + sequence++);
        publisher.publishEvent(event);
        return event;
    }

    public TripMealChanged tripChanged(
            long tripId, Set<Long> residentIds, Instant from, Instant expectedUntil, String changeType) {
        TripMealChanged event =
                new TripMealChanged(tripId, residentIds, from, expectedUntil, changeType, "FAKE-TRIP-" + sequence++);
        publisher.publishEvent(event);
        return event;
    }
}
