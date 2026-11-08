package com.eldercare.backend.nutrition.api.dto;

import java.time.Instant;
import java.time.LocalDate;

/** Phiếu bữa ăn cho báo cáo (016): trạng thái, giờ giao, trễ, sai lệch (FR-047, FR-048). */
public record MealSlipReportView(
        long slipId,
        LocalDate date,
        long mealId,
        long floorAreaId,
        MealSlipStatus status,
        Instant deliveredAt,
        boolean late,
        boolean hasDiscrepancy) {}
