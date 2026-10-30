package com.cambae.backend.m07.api.spi;

import java.math.BigDecimal;

/**
 * Purchased assisted medication request details from module M11.
 */
public record PurchaseAssistanceView(
        long requestId, long residentId, String medicationName, BigDecimal quantityPurchased, long purchaserStaffId) {}
