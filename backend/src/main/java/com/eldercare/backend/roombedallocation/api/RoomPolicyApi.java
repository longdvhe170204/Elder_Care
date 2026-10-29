package com.eldercare.backend.roombedallocation.api;

import com.eldercare.backend.roombedallocation.api.dto.RoomAllocationProperties;
import com.eldercare.backend.roombedallocation.api.spi.CareLevelCode;

/** Hợp đồng 003 RoomPolicyApi (T003); bản cài thuộc user story tương ứng. */
public interface RoomPolicyApi {
    boolean currentRoomAllows(long residentId, CareLevelCode level);

    RoomAllocationProperties allocationProperties(long bedId);
}
