package com.eldercare.backend.incident.internal.infection.domain;

import java.util.Set;

/** Phạm vi khoanh vùng theo Q-69. */
public record ContainmentScope(Set<Long> floorIds, Set<Long> areaIds) {

    public ContainmentScope {
        floorIds = Set.copyOf(floorIds);
        areaIds = Set.copyOf(areaIds);
        if (floorIds.isEmpty() ? areaIds.size() != 1 : !areaIds.isEmpty()) {
            throw new IllegalArgumentException("Chọn một hoặc nhiều tầng, hoặc đúng một khu vực");
        }
    }
}
