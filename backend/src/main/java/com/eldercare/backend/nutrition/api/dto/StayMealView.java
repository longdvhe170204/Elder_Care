package com.eldercare.backend.nutrition.api.dto;

import java.time.Instant;

/** Lượt ở lại của người thân (012) kèm đăng ký ăn: người thân được tính suất ăn khi {@code mealRegistered} (FR-034 c). */
public record StayMealView(
        long stayId, long residentId, long familyMemberId, Instant from, Instant to, boolean mealRegistered) {}
