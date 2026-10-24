package com.cambae.backend.m07.api.events;

import java.math.BigDecimal;

/**
 * Event fired when medication billing is adjusted following record correction or return of carried medication.
 * Module M11 (010) listens to update billing entries.
 */
public record MedicationBillAdjustedEvent(
        BilledSource source, long sourceId, BigDecimal newQuantity, BillAdjustmentReason reason) {}
