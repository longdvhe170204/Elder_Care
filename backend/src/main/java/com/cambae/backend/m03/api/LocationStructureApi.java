package com.cambae.backend.m03.api;

import com.cambae.backend.m03.api.dto.AreaView;
import com.cambae.backend.m03.api.dto.FloorView;
import com.cambae.backend.m03.api.dto.LocationRef;
import com.cambae.backend.m03.api.dto.LocationView;
import com.cambae.backend.m03.api.dto.RoomTypeRef;
import com.cambae.backend.m03.api.dto.RoomView;
import java.util.Optional;
import java.util.Set;

/** Hợp đồng 003 LocationStructureApi (T003); bản cài thuộc user story tương ứng. */
public interface LocationStructureApi {
    Optional<LocationView> bed(long bedId);

    Optional<RoomView> room(long roomId);

    Optional<FloorView> floor(long floorId);

    Optional<AreaView> area(long areaId);

    boolean active(LocationRef location);

    Set<Long> floorsInArea(long areaId);

    Optional<LocationRef> dayCareRestAreaLocation();

    Optional<RoomTypeRef> roomTypeOfBed(long bedId);
}
