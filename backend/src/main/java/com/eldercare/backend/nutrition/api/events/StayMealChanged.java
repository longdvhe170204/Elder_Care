package com.eldercare.backend.nutrition.api.events;

import java.time.Instant;

/** 012 phát: bắt đầu, kết thúc sớm, hủy, đổi đăng ký ăn của lượt ở lại. Listener 011 idempotent theo {@code eventKey}. */
public record StayMealChanged(
        long stayId,
        long residentId,
        long familyMemberId,
        String status,
        boolean mealRegistered,
        Instant from,
        Instant to,
        String eventKey) {}
