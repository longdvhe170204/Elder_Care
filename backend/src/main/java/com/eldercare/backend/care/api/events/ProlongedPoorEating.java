package com.eldercare.backend.care.api.events;

import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.common.notification.*;
import java.time.*;
import java.util.*;

/** T004: payload liên module, chưa đăng ký producer của story. */
public record ProlongedPoorEating(
        long residentId, Instant firstMealAt, Instant lastMealAt, int mealCount, Instant at, CareEventContext context)
        implements CareEvent {
    /** Tương thích caller cũ; producer mới truyền thời điểm bữa thật. */
    public ProlongedPoorEating(long residentId, List<Long> mealIds, Instant at, CareEventContext context) {
        this(residentId, at, at, mealIds.size(), at, context);
    }

    /** Giữ khả năng giao lại outbox cũ, không ghi danh sách bữa vào payload mới. */
    @com.fasterxml.jackson.annotation.JsonCreator
    public static ProlongedPoorEating restore(
            @com.fasterxml.jackson.annotation.JsonProperty("residentId") long residentId,
            @com.fasterxml.jackson.annotation.JsonProperty("firstMealAt") Instant firstMealAt,
            @com.fasterxml.jackson.annotation.JsonProperty("lastMealAt") Instant lastMealAt,
            @com.fasterxml.jackson.annotation.JsonProperty("mealCount") Integer mealCount,
            @com.fasterxml.jackson.annotation.JsonProperty("at") Instant at,
            @com.fasterxml.jackson.annotation.JsonProperty("context") CareEventContext context,
            @com.fasterxml.jackson.annotation.JsonProperty("mealIds") List<Long> legacyIds) {
        return new ProlongedPoorEating(
                residentId,
                firstMealAt == null ? at : firstMealAt,
                lastMealAt == null ? at : lastMealAt,
                mealCount == null ? legacyIds.size() : mealCount,
                at,
                context);
    }
}
