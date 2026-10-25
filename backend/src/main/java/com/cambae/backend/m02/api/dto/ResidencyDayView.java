package com.cambae.backend.m02.api.dto;

import java.time.LocalDate;

/**
 * Một ngày lưu trú trong thời gian tính phí (ResidencyDayApi.daysBetween; FR-052, FR-060, FR-074). {@code presence} là
 * {@code PRESENT} hoặc {@code ABSENT}; {@code absenceId}, {@code sequenceNo}, {@code coefficientPercent} chỉ có khi
 * vắng. Dấu: chờ quyết định, không có dòng chính sách, chưa vào ở, quá hạn hợp đồng. {@code outsideBillingPeriod}: sau
 * mốc dừng hoặc sau ngày qua đời (FR-070).
 */
public record ResidencyDayView(
        LocalDate date,
        Long contractId,
        Long contentVersionId,
        String presence,
        Long absenceId,
        Integer sequenceNo,
        Integer coefficientPercent,
        boolean pendingDecision,
        boolean noPolicyLine,
        boolean notYetAdmitted,
        boolean contractOverdue,
        boolean outsideBillingPeriod) {}
