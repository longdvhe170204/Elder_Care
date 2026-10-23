package com.cambae.backend.m07.api.dto;

import java.time.LocalDate;

/**
 * Đơn thuốc hiển thị trên cổng người thân (DonThuocNguoiThanView).
 */
public record FamilyPrescriptionView(
        long prescriptionId,
        long residentId,
        String medicationName,
        String dosageInstruction,
        String status,
        LocalDate startDate,
        LocalDate endDate) {}
