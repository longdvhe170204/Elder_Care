package com.eldercare.backend.care.internal.careplan.domain;

import com.eldercare.backend.care.api.dto.CarePlanItemView;
import java.util.*;

/** FR-008:map khóa theo lineage/sourceItemId do persistence của phiên bản cung cấp. */
public record CarePlanComparison(
        List<CarePlanItemView> added, List<CarePlanItemView> removed, List<CarePlanItemChange> changed) {
    public CarePlanComparison {
        added = List.copyOf(added);
        removed = List.copyOf(removed);
        changed = List.copyOf(changed);
    }

    public static CarePlanComparison compare(Map<Long, CarePlanItemView> before, Map<Long, CarePlanItemView> after) {
        var added = new ArrayList<CarePlanItemView>();
        var removed = new ArrayList<CarePlanItemView>();
        var changed = new ArrayList<CarePlanItemChange>();
        for (var entry : after.entrySet()) {
            var old = before.get(entry.getKey());
            if (old == null) added.add(entry.getValue());
            else {
                var a = fields(old);
                var b = fields(entry.getValue());
                var names = a.keySet().stream()
                        .filter(k -> !Objects.equals(a.get(k), b.get(k)))
                        .toList();
                if (!names.isEmpty()) changed.add(new CarePlanItemChange(old, entry.getValue(), names));
            }
        }
        before.forEach((key, value) -> {
            if (!after.containsKey(key)) removed.add(value);
        });
        return new CarePlanComparison(added, removed, changed);
    }

    private static Map<String, Object> fields(CarePlanItemView row) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taskTypeId", row.taskTypeId());
        data.put("content", row.content());
        data.put("frequencyType", row.frequencyType());
        data.put("intervalHours", row.intervalHours());
        data.put("timesPerDay", row.timesPerDay());
        data.put("startsAt", row.startsAt());
        data.put("times", row.times());
        data.put("weekdays", row.weekdays());
        data.put("windowBeforeMinutes", row.windowBeforeMinutes());
        data.put("windowAfterMinutes", row.windowAfterMinutes());
        data.put("performerRole", row.performerRole());
        data.put("resultType", row.resultType());
        data.put("importance", row.importance());
        data.put("billable", row.billable());
        data.put("waterTargetMl", row.waterTargetMl());
        return data;
    }
}
