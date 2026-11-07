package com.eldercare.backend.nutrition.api.events;

import java.time.Instant;

/** Phiếu bữa ăn đã giao (FR-047) cho báo cáo 016. */
public record MealSlipDelivered(long slipId, long dailyMealId, long floorAreaId, Instant at) {}
