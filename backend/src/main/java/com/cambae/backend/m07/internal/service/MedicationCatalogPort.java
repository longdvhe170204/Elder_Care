package com.cambae.backend.m07.internal.service;

/**
 * Port for looking up hospital medication procurement service/pricing from Module M02 (004) per FR-003.
 * When an item is not yet present in 004, returns false to flag {@code pendingProcurementItem = true}.
 */
public interface MedicationCatalogPort {

    /**
     * Checks if hospital medication has an associated catalog item (service THUOC_NGUON_VIEN) in 004.
     *
     * @param medicationId medication ID
     * @return true if catalog item exists, false otherwise
     */
    boolean hasHospitalProcurementItem(long medicationId);
}
