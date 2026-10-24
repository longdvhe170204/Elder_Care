package com.cambae.backend.m09.assignment.internal.domain;

import com.cambae.backend.common.Role;
import java.time.Instant;

/** FR-025/037a: phân công có trạng thái và khoảng hiệu lực; thay người phải kết thúc dòng cũ. */
public record StaffAssignment(
        long id,
        long shiftId,
        long staffId,
        String targetType,
        long targetId,
        Long taskTypeId,
        Role role,
        String assignmentKind,
        boolean assignedNurse,
        Instant startsAt,
        Instant endsAt,
        String status,
        Instant actualEndAt,
        String source,
        long version) {
    public Instant effectiveEnd() {
        return actualEndAt != null && actualEndAt.isBefore(endsAt) ? actualEndAt : endsAt;
    }

    public boolean activeAt(Instant at) {
        return !java.util.Set.of("DRAFT", "CANCELLED").contains(status)
                && !startsAt.isAfter(at)
                && effectiveEnd().isAfter(at);
    }
}
