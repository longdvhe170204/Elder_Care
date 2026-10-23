package com.cambae.backend.m07.api.dto;

import java.time.LocalTime;
import java.util.List;

/**
 * Thuốc đang dùng cho thẻ khẩn cấp và tóm tắt chuyển viện (ThuocDangDungView, FR-050).
 */
public record ActiveMedicationView(
        long prescriptionId,
        MedicationSummary medication,
        String dosage,
        String routeOfAdministration,
        List<LocalTime> scheduledTimes,
        String prnLimits,
        MedicationSource source,
        String status) {}
