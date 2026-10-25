package com.cambae.backend.m07.api.dto;

import java.time.LocalDate;

/**
 * Prescription summary view.
 */
public record PrescriptionView(
        long prescriptionId,
        long residentId,
        String prescriptionType, // DINH_KY | PRN
        String status, // CHO_HIEU_LUC | HIEU_LUC | TAM_DUNG | DA_NGUNG | HET_HAN
        MedicationSource source,
        LocalDate startDate,
        LocalDate endDate,
        MedicationSummary medication,
        String dosageInstruction,
        Long replacedById) {}
