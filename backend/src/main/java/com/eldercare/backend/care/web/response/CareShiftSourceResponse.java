package com.eldercare.backend.care.web.response;

import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.spi.ShiftStaffPort.CareAssignmentView;
import com.eldercare.backend.care.internal.task.domain.CareShiftSources;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.*;

@Schema(name = "CareShiftSourceData")
public record CareShiftSourceResponse(Shift shift, List<Resident> residents) {
    public record Shift(long id, Instant startsAt, Instant endsAt, long floorAreaId) {}

    public record Resident(
            long residentId,
            String attendance,
            List<CareAssignmentView> assignments,
            List<CarePlanItemResponse> planItems,
            List<OtherSource> otherSources) {}

    public record OtherSource(
            com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType type,
            long id,
            Instant at,
            String description) {}

    public static CareShiftSourceResponse of(CareShiftSources source) {
        var s = source.shift();
        return new CareShiftSourceResponse(
                new Shift(s.id(), s.startsAt(), s.endsAt(), s.floorAreaId()),
                source.residents().stream()
                        .map(r -> new Resident(
                                r.residentId(),
                                r.attendance(),
                                r.assignments(),
                                r.planItems().stream()
                                        .map(CareShiftSourceResponse::item)
                                        .toList(),
                                r.otherSources().stream()
                                        .map(v -> new OtherSource(v.type(), v.id(), v.at(), v.description()))
                                        .toList()))
                        .toList());
    }

    private static CarePlanItemResponse item(CarePlanItemView i) {
        var weekdays = new ArrayList<Integer>();
        for (int day = 1; day <= 7; day++) if ((i.weekdays().orElse(0) & (1 << (day - 1))) != 0) weekdays.add(day);
        return new CarePlanItemResponse(
                i.id(),
                null,
                i.taskTypeId(),
                i.content(),
                i.frequencyType(),
                i.intervalHours().orElse(null),
                i.timesPerDay().orElse(null),
                i.startsAt().orElse(null),
                i.times(),
                weekdays,
                i.windowBeforeMinutes(),
                i.windowAfterMinutes(),
                i.performerRole(),
                i.resultType(),
                i.importance(),
                i.billable(),
                i.waterTargetMl().orElse(null));
    }
}
