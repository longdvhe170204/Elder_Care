package com.eldercare.backend.staffing.api.dto;

import java.time.YearMonth;

/** Giờ làm trong tháng của một nhân viên (015 FR-042, FR-034 của 008): đã làm và đã xếp; không có lý do nghỉ. */
public record WorkingHoursView(long staffId, YearMonth month, long workedMinutes, long scheduledMinutes) {}
