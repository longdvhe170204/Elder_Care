package com.eldercare.backend.admission.api.dto;

import java.time.LocalDate;

/**
 * Một ngày lưu trú trong thời gian tính phí (ResidencyDayApi.daysBetween; FR-052, FR-074; Q-395). {@code presence} là {@code
 * PRESENT} hoặc {@code ABSENT}; {@code absenceId} chỉ có khi vắng. {@code bedReleased}: từ ngày giải phóng giường, ngày vắng không
 * tính phí lưu trú. Dấu: chưa vào ở, quá hạn hợp đồng. {@code outsideBillingPeriod}: sau mốc dừng hoặc sau ngày qua đời (FR-070).
 */
public record ResidencyDayView(
        LocalDate date,
        Long contractId,
        Long contentVersionId,
        String presence,
        Long absenceId,
        boolean bedReleased,
        boolean notYetAdmitted,
        boolean contractOverdue,
        boolean outsideBillingPeriod) {}
