package com.eldercare.backend.nutrition.api.events;

import java.time.Instant;

/** Phiếu giao trễ quá ngưỡng CFG-M08-04 (FR-048a) cho báo cáo 016. */
public record MealSlipDeliveredLate(
        long slipId, long dailyMealId, long floorAreaId, Instant expectedAt, Instant deliveredAt) {}
