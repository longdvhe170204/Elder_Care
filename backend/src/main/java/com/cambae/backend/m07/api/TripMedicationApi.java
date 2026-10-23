package com.cambae.backend.m07.api;

import com.cambae.backend.m07.api.dto.TimeInterval;
import com.cambae.backend.m07.api.dto.TripChangeType;

/**
 * API quản lý thuốc mang theo khi người cao tuổi ra ngoài / chuyển viện (MangThuocChuyenDiApi).
 * Dùng bởi Module M04 (014 - Hoạt động và chuyến đi).
 */
public interface TripMedicationApi {
    /** Kiểm tra xem thuốc mang theo cho chuyến đi đã được chuẩn bị xong chưa (FR-048b). */
    boolean isPrepared(long residentId, long tripId);

    /** Thông báo chuyến đi có sự thay đổi (Hủy chuyến, Không đi, Đổi ngày) để nhắc nhận lại thuốc (Q-58). */
    void onTripChanged(long tripId, long residentId, TripChangeType changeType);

    /** Tính lại khoảng thời gian liều mang theo khi thời điểm rời/về được đính chính (FR-048b). */
    void recalculateInterval(long tripId, long residentId, TimeInterval newInterval);
}
