package com.cambae.backend.m07.api.spi;

import java.math.BigDecimal;

/**
 * Thông tin đề nghị mua hộ đã mua từ module M11 (DeNghiMuaHoView).
 */
public record PurchaseAssistanceView(
        long requestId, long residentId, String medicationName, BigDecimal quantityPurchased, long purchaserStaffId) {}
