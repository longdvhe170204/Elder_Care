package com.cambae.backend.m03.api.spi;

import java.time.Instant;
import java.util.Set;

/** 007 cài; dữ liệu khoanh vùng và nghi nhiễm/tiếp xúc tại thời điểm yêu cầu (003 FR-009, FR-045). */
public interface AllocationInfectionStatus {
    RestrictedZone restrictedZone(Instant at);

    boolean listed(long residentId, Instant at);

    /** Tầng và khu vực đang bị khoanh; không sao chép bảng của 007 (Q-69). */
    record RestrictedZone(Set<Long> floorIds, Set<Long> areaIds) {
        public RestrictedZone {
            floorIds = Set.copyOf(floorIds);
            areaIds = Set.copyOf(areaIds);
        }
    }
}
