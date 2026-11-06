package com.eldercare.backend.nutrition.api.dto;

import java.time.Instant;
import java.time.LocalDate;

/** Bữa trong ngày: giờ dự kiến (chốt lúc tạo) và đã chốt suất chưa. */
public record DailyMealView(long dailyMealId, long mealId, LocalDate date, Instant expectedTime, boolean closed) {}
