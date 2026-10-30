package com.eldercare.backend.medication.api.spi;

import java.util.Optional;

/**
 * SPI declared by M07 and implemented by Module M11 (010) to verify assisted medication purchases.
 * Read-only.
 */
public interface PurchaseAssistancePort {
    /** Retrieve assisted purchase request details in PURCHASED status. */
    Optional<PurchaseAssistanceView> purchasedDetails(long requestId, long residentId);
}
