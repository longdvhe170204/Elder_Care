package com.eldercare.backend.care.internal.careplan.domain;

import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.common.Role;
import java.time.LocalTime;
import java.util.*;

/** Nội dung mục TASK; khung null ở Nháp được cố định bằng giá trị hiện hành khi gửi duyệt. */
public record CarePlanItemData(
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
    public CarePlanItemData {
        times = times == null ? List.of() : List.copyOf(times);
        weekdays = weekdays == null ? List.of() : List.copyOf(weekdays);
    }

    public CarePlanItemData windows(int before, int after) {
        return new CarePlanItemData(
                taskTypeId,
                content,
                frequencyType,
                intervalHours,
                timesPerDay,
                startsAt,
                times,
                weekdays,
                before,
                after,
                performerRole,
                resultType,
                importance,
                billable,
                waterTargetMl);
    }

    public Map<String, Object> fields() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskTypeId", taskTypeId);
        result.put("content", content);
        result.put("frequencyType", frequencyType);
        result.put("intervalHours", intervalHours);
        result.put("timesPerDay", timesPerDay);
        result.put("startsAt", startsAt);
        result.put("times", times);
        result.put("weekdays", weekdays);
        result.put("windowBeforeMinutes", windowBeforeMinutes);
        result.put("windowAfterMinutes", windowAfterMinutes);
        result.put("performerRole", performerRole);
        result.put("resultType", resultType);
        result.put("importance", importance);
        result.put("billable", billable);
        result.put("waterTargetMl", waterTargetMl);
        return result;
    }

    public CarePlanItemView view(long id) {
        if (windowBeforeMinutes == null || windowAfterMinutes == null)
            throw new IllegalStateException("Mục chưa cố định khung");
        int days = weekdays.stream().reduce(0, (mask, day) -> mask | (1 << (day - 1)));
        return new CarePlanItemView(
                id,
                taskTypeId,
                content,
                frequencyType,
                Optional.ofNullable(intervalHours),
                Optional.ofNullable(timesPerDay),
                Optional.ofNullable(startsAt),
                times,
                weekdays.isEmpty() ? Optional.empty() : Optional.of(days),
                windowBeforeMinutes,
                windowAfterMinutes,
                performerRole,
                resultType,
                importance,
                billable,
                Optional.ofNullable(waterTargetMl));
    }
}
