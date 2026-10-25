package com.cambae.backend.m03.api.spi;

import java.time.Instant;

/** Nguồn khoanh vùng và danh sách nghi nhiễm/tiếp xúc do 007 cài (FR-009, FR-045). */
public interface AllocationInfectionStatus {
    CordonedArea cordonedAt(Instant at);

    boolean listedAt(long residentId, Instant at);
}
