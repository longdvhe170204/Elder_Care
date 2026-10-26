package com.cambae.backend.m03.api.spi;

import java.util.Set;

/** Tầng và khu vực đang khoanh vùng; dữ liệu từ 007, FR-009. */
public record CordonedArea(Set<Long> floorIds, Set<Long> areaIds) {
    public CordonedArea {
        floorIds = Set.copyOf(floorIds);
        areaIds = Set.copyOf(areaIds);
    }
}
