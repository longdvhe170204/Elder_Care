package com.cambae.backend.m03.internal.bed.domain;

import com.cambae.backend.m03.api.dto.*;
import com.cambae.backend.m03.api.spi.CareLevelCode;
import java.time.Instant;
import java.util.Set;

/** Các ảnh đọc nội bộ của T061; record không nằm trong tầng persistence. */
public final class BedReportingSnapshot {
    private BedReportingSnapshot() {}

    public record State(
            long bedId, long floorId, long roomId, String code, BedStatus state, String event, Instant at) {}

    public record Window(State state, Instant until, Long departedResidentId) {}

    public record Policy(
            long bedId,
            long roomId,
            long floorId,
            long areaId,
            boolean active,
            GenderPolicy gender,
            Set<CareLevelCode> levels,
            boolean longTerm,
            boolean shortTerm,
            boolean isolated,
            boolean reserved) {}

    public record Flags(
            long allocationId,
            long residentId,
            long bedId,
            long roomId,
            long floorId,
            Instant from,
            Instant to,
            boolean active,
            Set<String> flags) {}
}
