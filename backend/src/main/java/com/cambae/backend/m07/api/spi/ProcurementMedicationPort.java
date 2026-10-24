package com.cambae.backend.m07.api.spi;

import java.util.Optional;

/**
 * SPI declared by M07 and implemented by Module M11/M03 (019) to reconcile supplied medication against procurement orders.
 * Read-only.
 */
public interface ProcurementMedicationPort {
    /** Retrieve medication request line in PURCHASED status for intake reconciliation. */
    Optional<ProcurementMedicationLineView> lineRecordedPurchased(long lineId);
}
