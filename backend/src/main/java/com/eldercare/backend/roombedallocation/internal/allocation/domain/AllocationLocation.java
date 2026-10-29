package com.eldercare.backend.roombedallocation.internal.allocation.domain;

import com.eldercare.backend.administration.account.api.spi.BedAllocationView;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;

/** Projection vị trí sau đính chính, chỉ dùng nội bộ M03. */
public record AllocationLocation(long allocationId, long residentId, AllocationStatus status, LocationView location) {
    public BedAllocationView scopeView() {
        return new BedAllocationView(allocationId, location.bedId(), location.roomId(), location.floorId());
    }
}
