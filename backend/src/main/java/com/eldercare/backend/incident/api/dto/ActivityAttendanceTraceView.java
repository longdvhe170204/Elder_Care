package com.eldercare.backend.incident.api.dto;

import java.time.Instant;
import java.util.List;

/** Buổi hoạt động và những người cao tuổi cùng điểm danh với người đang được truy vết (FR-060). */
public record ActivityAttendanceTraceView(long sessionId, Instant from, Instant to, List<Long> residentIds) {
    public ActivityAttendanceTraceView {
        residentIds = List.copyOf(residentIds);
    }
}
