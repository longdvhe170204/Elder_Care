package com.eldercare.backend.care.internal.task.domain;

import com.eldercare.backend.care.api.dto.CarePlanItemView;
import com.eldercare.backend.care.api.spi.ShiftStaffPort.*;
import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceView;
import java.time.Instant;
import java.util.List;

/** Nguồn để Trưởng tầng đối chiếu và chọn mốc từ kế hoạch trước khi lập công việc. */
public record CareShiftSources(ShiftView shift, List<ResidentSources> residents) {
    public record ResidentSources(
            long residentId,
            String attendance,
            List<CareAssignmentView> assignments,
            List<CarePlanItemView> planItems,
            List<PlanOccurrence> planOccurrences,
            List<SourceView> otherSources) {}

    public record PlanOccurrence(long itemId, Instant plannedAt) {}
}
