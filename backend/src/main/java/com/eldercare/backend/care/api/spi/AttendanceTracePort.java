package com.eldercare.backend.care.api.spi;

import com.eldercare.backend.care.api.dto.ActivityAttendanceTraceView;
import java.time.Instant;
import java.util.List;

/**
 * DiemDanhTruyVetPort (007 mục 2.1, FR-060 (b)), do 014 cài, 007 đọc; chỉ đọc những người cùng Có mặt. Khai ở care như
 * {@link ActivityHealthGatePort} để care không phụ thuộc incident (C-02; chuyển từ {@code incident.api.spi} 2026-11-08).
 */
public interface AttendanceTracePort {
    /** Các buổi có thời gian chồng khoảng [from, to), kể cả chuyến đi. */
    List<ActivityAttendanceTraceView> coAttendance(long residentId, Instant from, Instant to);
}
