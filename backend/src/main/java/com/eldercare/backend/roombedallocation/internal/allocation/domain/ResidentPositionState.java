package com.eldercare.backend.roombedallocation.internal.allocation.domain;

import java.time.Instant;
import java.util.List;

/**
 * T066 (R-15): vị trí theo người cao tuổi của M03 trên {@code resident_module_state} (thay {@code resident_position}):
 * dấu "chưa có giường" từ lúc nào, giường đang giữ chỗ hồ sơ chờ của người đó.
 */
public record ResidentPositionState(Instant withoutBedSince, List<Long> waitingHoldBedIds) {
    public static final ResidentPositionState EMPTY = new ResidentPositionState(null, List.of());

    public ResidentPositionState {
        waitingHoldBedIds = waitingHoldBedIds == null ? List.of() : List.copyOf(waitingHoldBedIds);
    }
}
