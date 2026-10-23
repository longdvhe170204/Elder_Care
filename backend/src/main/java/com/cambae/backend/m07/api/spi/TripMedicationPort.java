package com.cambae.backend.m07.api.spi;

import com.cambae.backend.m07.api.dto.TimeInterval;
import java.util.Optional;

/**
 * SPI do M07 khai báo, Module M04 (014) cài đặt để cung cấp thông tin chuyến đi ra ngoài / chuyển viện.
 * Chỉ đọc (ChuyenDiThuocPort).
 */
public interface TripMedicationPort {
    /** Khoảng thời gian đi dự kiến khi người cao tuổi được đánh giá Đạt. */
    Optional<TimeInterval> approvedTripInterval(long residentId, long tripId);

    /** Khoảng thời gian đi của chuyến. */
    Optional<TimeInterval> tripInterval(long tripId);
}
