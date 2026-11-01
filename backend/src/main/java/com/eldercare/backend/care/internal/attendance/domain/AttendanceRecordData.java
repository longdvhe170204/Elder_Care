package com.eldercare.backend.care.internal.attendance.domain;

import com.eldercare.backend.care.api.dto.CheckoutType;
import java.time.Instant;

/** Bản gốc nhóm3; đính chính không sửa nội dung hoặc thời điểm gốc này. */
public record AttendanceRecordData(
        long attendanceId, String direction, Instant at, CheckoutType checkoutType, Long pickupRecordId) {}
