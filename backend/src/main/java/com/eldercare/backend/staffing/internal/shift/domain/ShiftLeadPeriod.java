package com.eldercare.backend.staffing.internal.shift.domain;

import java.time.Instant;

/** Khoảng phụ trách đã ghi, cắt theo thời gian ca và tư cách thành viên; không suy từ con trỏ hiện tại. */
public record ShiftLeadPeriod(long shiftId, long floorAreaId, Instant startsAt, Instant endsAt, Instant shiftEndsAt) {}
