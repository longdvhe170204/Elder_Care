package com.eldercare.backend.incident.api.dto;

import java.time.Instant;
import java.util.Set;

/** Phạm vi và thời gian khoanh vùng, không chứa danh sách người tiếp xúc (mục 1.8). */
public record IsolationZoneView(long id, Set<Long> floorAreaIds, Set<Long> areaIds, Instant startsAt, Instant endsAt) {
    public IsolationZoneView {
        floorAreaIds = Set.copyOf(floorAreaIds);
        areaIds = Set.copyOf(areaIds);
    }
}
