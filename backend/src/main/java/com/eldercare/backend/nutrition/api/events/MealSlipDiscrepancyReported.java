package com.eldercare.backend.nutrition.api.events;

import java.time.Instant;

/** Tầng/khu báo sai lệch phiếu (FR-048) cho báo cáo 016. */
public record MealSlipDiscrepancyReported(long slipId, long dailyMealId, long floorAreaId, Instant at) {}
