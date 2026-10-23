package com.cambae.backend.m07.api.spi;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * SPI do M07 khai báo, Module M09 (008) cài đặt để cung cấp thông tin ca trực và phân công điều dưỡng.
 * Chỉ đọc (DieuDuongCaPort).
 */
public interface ShiftNursePort {
    /** Ca của tầng chứa thời điểm. */
    Optional<ShiftView> shiftContaining(long floorAreaId, Instant time);

    /** Điều dưỡng phụ trách người cao tuổi trong ca. */
    Optional<Long> assignedNurse(long residentId, Instant time);

    /** Danh sách điều dưỡng có ca trực tại tầng trong mốc giờ. */
    List<Long> nursesOnShift(long floorAreaId, Instant time);

    /** Nhân viên có tên trong ca đang diễn ra không. */
    boolean isStaffOnActiveShift(long staffId, Instant time);

    /** Bàn giao ca trực đã được xác nhận chưa. */
    boolean isHandoverConfirmed(long shiftId);
}
