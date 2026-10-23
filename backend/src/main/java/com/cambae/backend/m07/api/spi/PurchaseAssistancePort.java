package com.cambae.backend.m07.api.spi;

import java.util.Optional;

/**
 * SPI do M07 khai báo, Module M11 (010) cài đặt để đối soát mua thuốc hộ (DeNghiMuaHoPort).
 * Chỉ đọc.
 */
public interface PurchaseAssistancePort {
    /** Lấy thông tin đề nghị mua hộ ở trạng thái ĐÃ MUA. */
    Optional<PurchaseAssistanceView> purchasedDetails(long requestId, long residentId);
}
