package com.eldercare.backend.care.web.request;

import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.internal.careplan.domain.*;
import com.eldercare.backend.common.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;
import java.util.List;

/** CarePlanItem của hợp đồng; id/gốc chỉ được tham chiếu mục đã có trong cùng bản Nháp. */
@Schema(name = "CarePlanItem")
public record CarePlanItemWrite(
        Long id,
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
    public CarePlanItem item(long planId) {
        if (times != null && times.stream().anyMatch(java.util.Objects::isNull)
                || weekdays != null && weekdays.stream().anyMatch(java.util.Objects::isNull))
            throw new com.eldercare.backend.common.error.InvalidValueException(
                    "Giờ và ngày trong tuần không được chứa giá trị null");
        return new CarePlanItem(
                id == null ? 0 : id,
                planId,
                sourceItemId,
                0,
                new CarePlanItemData(
                        taskTypeId,
                        content,
                        frequencyType,
                        intervalHours,
                        timesPerDay,
                        startsAt,
                        times,
                        weekdays,
                        windowBeforeMinutes,
                        windowAfterMinutes,
                        performerRole,
                        resultType,
                        importance,
                        billable,
                        waterTargetMl));
    }
}
