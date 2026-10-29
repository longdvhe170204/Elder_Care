package com.eldercare.backend.roombedallocation.api;

import com.eldercare.backend.roombedallocation.api.dto.AreaView;
import com.eldercare.backend.roombedallocation.api.dto.FloorView;
import com.eldercare.backend.roombedallocation.api.dto.LocationRef;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import com.eldercare.backend.roombedallocation.api.dto.RoomTypeRef;
import com.eldercare.backend.roombedallocation.api.dto.RoomView;
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
