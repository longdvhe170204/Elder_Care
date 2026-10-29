package com.eldercare.backend.staffing.shift.internal.domain;

import java.time.Instant;

/** Khoảng phụ trách đã ghi, cắt theo thời gian ca và tư cách thành viên; không suy từ con trỏ hiện tại. */
public record ShiftLeadPeriod(long shiftId, long floorAreaId, Instant startsAt, Instant endsAt, Instant shiftEndsAt) {}
