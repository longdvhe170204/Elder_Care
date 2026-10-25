package com.cambae.backend.m03.api;

import com.cambae.backend.m03.api.dto.RoomAllocationProperties;
import com.cambae.backend.m03.api.spi.CareLevelCode;

/** Hợp đồng 003 RoomPolicyApi (T003); bản cài thuộc user story tương ứng. */
public interface RoomPolicyApi {
    boolean currentRoomAllows(long residentId, CareLevelCode level);

    RoomAllocationProperties allocationProperties(long bedId);
}
