package com.eldercare.backend.staffing.handover.internal.domain;

import com.eldercare.backend.staffing.shift.internal.domain.WorkShift;
import java.time.Duration;
import java.util.*;

/** FR-038: cùng loại phạm vi và cùng id; tiếp giáp được ưu tiên trước khoảng tìm dự phòng. */
public final class NextHandoverShift {
    private NextHandoverShift() {}

    public static Optional<WorkShift> find(WorkShift previous, Collection<WorkShift> candidates, Duration maximumGap) {
        return candidates.stream()
                .filter(s -> s.id() != previous.id() && s.handoverRequired())
                .filter(s -> !Set.of("DRAFT", "CANCELLED").contains(s.status()))
                .filter(s ->
                        s.scopeType().equals(previous.scopeType()) && Objects.equals(s.scopeId(), previous.scopeId()))
                .filter(s -> s.startsAt().isAfter(previous.startsAt()))
                .filter(s -> !s.startsAt().isAfter(previous.endsAt().plus(maximumGap)))
                .min(Comparator.comparing(WorkShift::startsAt).thenComparingLong(WorkShift::id));
    }
}
