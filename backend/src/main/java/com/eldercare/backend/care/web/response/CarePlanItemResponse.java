package com.eldercare.backend.care.web.response;

import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.internal.careplan.domain.CarePlanItem;
import com.eldercare.backend.common.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;
import java.util.List;

/** T016: không đưa kind/active hoặc id vật lý của danh mục vào HTTP. */
@Schema(name = "CarePlanItem")
public record CarePlanItemResponse(
        long id,
        Long sourceItemId,
        long taskTypeId,
        String content,
        String frequencyType,
        Integer intervalHours,
        Integer timesPerDay,
        LocalTime startsAt,
        List<LocalTime> times,
        List<Integer> weekdays,
        Integer windowBeforeMinutes,
        Integer windowAfterMinutes,
        Role performerRole,
        ResultType resultType,
        Importance importance,
        boolean billable,
        Integer waterTargetMl) {
    public static CarePlanItemResponse of(CarePlanItem row) {
        var d = row.data();
        return new CarePlanItemResponse(
                row.id(),
                row.sourceItemId(),
                d.taskTypeId(),
                d.content(),
                d.frequencyType(),
                d.intervalHours(),
                d.timesPerDay(),
                d.startsAt(),
                d.times(),
                d.weekdays(),
                d.windowBeforeMinutes(),
                d.windowAfterMinutes(),
                d.performerRole(),
                d.resultType(),
                d.importance(),
                d.billable(),
                d.waterTargetMl());
    }
}
