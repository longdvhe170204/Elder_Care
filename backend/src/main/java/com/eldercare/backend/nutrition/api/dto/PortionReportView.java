package com.eldercare.backend.nutrition.api.dto;

import java.time.LocalDate;

/** Số suất cho báo cáo (016): theo tầng/khu, chế độ ăn; adjustment là suất phát sinh sau chốt. */
public record PortionReportView(
        LocalDate date, long mealId, long floorAreaId, long dietId, int portionCount, boolean adjustment) {}
