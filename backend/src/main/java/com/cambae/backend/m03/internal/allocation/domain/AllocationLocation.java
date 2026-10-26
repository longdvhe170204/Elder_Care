package com.cambae.backend.m03.internal.allocation.domain;

import com.cambae.backend.m03.api.dto.LocationView;
import com.cambae.backend.m15.account.api.spi.BedAllocationView;

/** Projection vị trí sau đính chính, chỉ dùng nội bộ M03. */
public record AllocationLocation(long allocationId, long residentId, AllocationStatus status, LocationView location) {
    public BedAllocationView scopeView() {
        return new BedAllocationView(allocationId, location.bedId(), location.roomId(), location.floorId());
    }
}
