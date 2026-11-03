package com.eldercare.backend.incident.api.spi;

import com.eldercare.backend.incident.api.dto.ActivityAttendanceTraceView;
import java.time.Instant;
import java.util.List;

/** DiemDanhTruyVetPort (007 mục 2.1), do 014 cài; chỉ đọc những người cùng Có mặt. */
public interface AttendanceTracePort {
    /** Các buổi có thời gian chồng khoảng [from, to), kể cả chuyến đi. */
    List<ActivityAttendanceTraceView> coAttendance(long residentId, Instant from, Instant to);
}
