package com.eldercare.backend.care.internal.careplan.domain;

import java.util.*;

/** FR-008: so sánh được cả Nháp chưa cố định khung; không thay null bằng giá trị cấu hình mới. */
public record CarePlanDifference(
        List<CarePlanItem> added, List<CarePlanItem> removed, List<CarePlanDifferenceEntry> changed) {
    public static CarePlanDifference compare(List<CarePlanItem> before, List<CarePlanItem> after) {
        Map<Long, CarePlanItem> old = new LinkedHashMap<>();
        before.forEach(item -> old.put(item.lineage(), item));
        var added = new ArrayList<CarePlanItem>();
        var changed = new ArrayList<CarePlanDifferenceEntry>();
        for (var item : after) {
            var previous = old.remove(item.lineage());
            if (previous == null) added.add(item);
            else {
                var a = previous.data().fields();
                var b = item.data().fields();
                var fields = a.keySet().stream()
                        .filter(key -> !Objects.equals(a.get(key), b.get(key)))
                        .toList();
                if (!fields.isEmpty()) changed.add(new CarePlanDifferenceEntry(previous, item, fields));
            }
        }
        return new CarePlanDifference(List.copyOf(added), List.copyOf(old.values()), List.copyOf(changed));
    }
}
