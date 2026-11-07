package com.eldercare.backend.nutrition.web.response;

import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.RetainedSample;
import com.eldercare.backend.nutrition.internal.sample.service.RetainedSampleService.DishLine;
import com.eldercare.backend.nutrition.internal.sample.service.RetainedSampleService.View;
import java.time.Instant;
import java.util.List;

/**
 * Món cần lưu mẫu và bản ghi lưu mẫu của một bữa (FR-060, FR-061): {@code dishes} gồm món cần lưu mà bếp chưa ghi
 * ({@code recorded = false}); {@code sample} vắng khi bữa chưa có bản ghi. Không có thông tin cá nhân.
 */
public record RetainedSampleResponse(long dailyMealId, List<Dish> dishes, Sample sample) {

    public record Dish(
            long dishId,
            String dishName,
            boolean recorded,
            boolean retained,
            String notRetainedReason,
            boolean addedLater) {

        static Dish of(DishLine l) {
            return new Dish(
                    l.dishId(), l.dishName(), l.recorded(), l.retained(), l.notRetainedReason(), l.addedLater());
        }
    }

    /** {@code status}: {@code RECORDED}, {@code DISCARDED}. */
    public record Sample(
            long id,
            String status,
            String storageLocation,
            long retainedBy,
            Instant retainedAt,
            Instant expiresAt,
            Long discardedBy,
            Instant discardedAt) {

        static Sample of(SimpleRecord<RetainedSample> row) {
            RetainedSample s = row.content();
            return new Sample(
                    row.id(),
                    row.status(),
                    s.storageLocation(),
                    s.retainedBy(),
                    s.retainedAt(),
                    s.expiresAt(),
                    s.discardedBy(),
                    s.discardedAt());
        }
    }

    public static RetainedSampleResponse of(View view) {
        return new RetainedSampleResponse(
                view.dailyMealId(),
                view.dishes().stream().map(Dish::of).toList(),
                view.sample() == null ? null : Sample.of(view.sample()));
    }
}
