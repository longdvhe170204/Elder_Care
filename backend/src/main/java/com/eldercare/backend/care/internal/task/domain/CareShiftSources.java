package com.eldercare.backend.care.internal.task.domain;

import com.eldercare.backend.care.api.dto.CarePlanItemView;
import com.eldercare.backend.care.api.spi.ShiftStaffPort.*;
import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceView;
import java.util.List;

/** Dữ liệu đã tồn tại để người lập đối chiếu, không có công việc đề xuất. */
public record CareShiftSources(ShiftView shift, List<ResidentSources> residents) {
    public record ResidentSources(
            long residentId,
            String attendance,
            List<CareAssignmentView> assignments,
            List<CarePlanItemView> planItems,
            List<SourceView> otherSources) {}
}
