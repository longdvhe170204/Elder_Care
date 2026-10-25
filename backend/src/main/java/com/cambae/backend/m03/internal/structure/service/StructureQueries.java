package com.cambae.backend.m03.internal.structure.service;

import com.cambae.backend.common.data.CatalogBase;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m03.api.LocationStructureApi;
import com.cambae.backend.m03.api.RoomPolicyApi;
import com.cambae.backend.m03.api.dto.AreaView;
import com.cambae.backend.m03.api.dto.FloorView;
import com.cambae.backend.m03.api.dto.LocationRef;
import com.cambae.backend.m03.api.dto.LocationView;
import com.cambae.backend.m03.api.dto.RoomAllocationProperties;
import com.cambae.backend.m03.api.dto.RoomTypeRef;
import com.cambae.backend.m03.api.dto.RoomView;
import com.cambae.backend.m03.api.spi.AllocationInfectionStatus;
import com.cambae.backend.m03.api.spi.CareLevelCode;
import com.cambae.backend.m03.internal.bed.domain.BedAssetPort;
import com.cambae.backend.m03.internal.bed.persistence.BedStore;
import com.cambae.backend.m03.internal.structure.domain.Area;
import com.cambae.backend.m03.internal.structure.domain.Floor;
import com.cambae.backend.m03.internal.structure.domain.Room;
import com.cambae.backend.m03.internal.structure.domain.RoomType;
import com.cambae.backend.m03.internal.structure.domain.StructureKind;
import com.cambae.backend.m03.internal.structure.persistence.StructureStore;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T017: API đọc cơ cấu và chính sách, không đọc module khác ngoài cổng đã công bố. */
@Service
@Transactional(readOnly = true)
public class StructureQueries implements LocationStructureApi, RoomPolicyApi {
    private final StructureStore store;
    private final BedStore beds;
    private final ObjectProvider<AllocationInfectionStatus> infections;
    private final ObjectProvider<BedAssetPort> assets;
    private final BusinessTime time;

    public StructureQueries(
            StructureStore store,
            BedStore beds,
            ObjectProvider<AllocationInfectionStatus> infections,
            ObjectProvider<BedAssetPort> assets,
            BusinessTime time) {
        this.store = store;
        this.beds = beds;
        this.infections = infections;
        this.assets = assets;
        this.time = time;
    }

    private Optional<CatalogBase> find(StructureKind kind, long id) {
        try {
            return Optional.of(store.find(kind, id, false));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<LocationView> bed(long id) {
        return beds.details(id).map(d -> d.location());
    }

    @Override
    public Optional<RoomView> room(long id) {
        return find(StructureKind.ROOM, id).map(e -> {
            var r = (Room) e;
            return new RoomView(r.getId(), r.floorId(), r.roomTypeId(), r.code(), r.getName(), r.isActive());
        });
    }

    @Override
    public Optional<FloorView> floor(long id) {
        return find(StructureKind.FLOOR, id).map(e -> {
            var f = (Floor) e;
            return new FloorView(f.getId(), f.areaId(), f.code(), f.getName(), f.buildingName(), f.isActive());
        });
    }

    @Override
    public Optional<AreaView> area(long id) {
        return find(StructureKind.AREA, id).map(e -> {
            var a = (Area) e;
            return new AreaView(a.getId(), a.code(), a.getName(), a.isActive());
        });
    }

    @Override
    public boolean active(LocationRef location) {
        return store.activeLocation(location);
    }

    @Override
    public Set<Long> floorsInArea(long areaId) {
        return store.floorIds(areaId);
    }

    @Override
    public Optional<LocationRef> dayCareRestAreaLocation() {
        return store.dayCareLocation();
    }

    @Override
    public Optional<RoomTypeRef> roomTypeOfBed(long id) {
        return beds.details(id).map(d -> {
            var r = (Room) store.find(StructureKind.ROOM, d.roomId(), false);
            var t = (RoomType) store.find(StructureKind.ROOM_TYPE, r.roomTypeId(), false);
            return new RoomTypeRef(t.getId(), t.getName());
        });
    }

    @Override
    public boolean currentRoomAllows(long residentId, CareLevelCode level) {
        return store.currentRoomAllows(residentId, level);
    }

    public Boolean isolationBlocked(Room room) {
        if (room.isolated()) return true;
        var source = infections.getIfAvailable();
        if (source == null) return null;
        var floor = (Floor) store.find(StructureKind.FLOOR, room.floorId(), false);
        var zones = source.cordonedAt(time.now());
        return zones.floorIds().contains(floor.getId()) || zones.areaIds().contains(floor.areaId());
    }

    @Override
    public RoomAllocationProperties allocationProperties(long bedId) {
        var d = beds.details(bedId).orElseThrow(NotFoundException::new);
        var r = (Room) store.find(StructureKind.ROOM, d.roomId(), false);
        var t = (RoomType) store.find(StructureKind.ROOM_TYPE, r.roomTypeId(), false);
        var blocked = isolationBlocked(r);
        var source = assets.getIfAvailable();
        if (blocked == null || source == null)
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_UNAVAILABLE", "Chưa thể kiểm tra đủ điều kiện phân bổ");
        return new RoomAllocationProperties(
                r.genderPolicy(),
                r.careLevels(),
                t.servesLongTerm(),
                t.servesShortTerm(),
                blocked,
                store.hasFutureAllocation(bedId),
                source.awaitingResidentTransfer(bedId));
    }
}
