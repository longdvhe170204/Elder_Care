package com.eldercare.backend.care.internal.attendance.domain;

import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import java.util.List;

public record AttendanceCheckoutResult(SimpleRecord<AttendanceData> attendance, List<Long> cancelledTaskIds) {}
