package com.eldercare.backend.nutrition.api.dto;

import java.time.Instant;

/** Đồ ăn gia đình cho báo cáo (016). */
public record FamilyFoodReportView(long id, long residentId, FamilyFoodStatus status, Instant receivedAt) {}
