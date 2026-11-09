package com.eldercare.backend.incident.internal.infection.service;

import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.incident.api.IsolationApi;
import com.eldercare.backend.incident.api.dto.IsolationZoneView;
import com.eldercare.backend.incident.internal.infection.domain.ContainmentContent;
import com.eldercare.backend.incident.internal.infection.persistence.ContainmentStore;
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import java.time.Instant;
import java.util.List;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bản thật {@link IsolationApi} (module-interfaces 007 mục 1.8; 012 FR-030 (c)): vùng có hiệu lực tại thời điểm hỏi. Vùng
 * khu vực tính cho mọi tầng của khu ({@code floorsInArea}); phòng xét theo tầng của phòng; người cao tuổi xét theo vị trí
 * lúc đó (khu hoặc tầng).
 */
@Service
@Transactional(readOnly = true)
public class DefaultIsolationApi implements IsolationApi {
    private final ContainmentStore store;
    private final ResidentLocationApi residents;
    private final LocationStructureApi structure;

    public DefaultIsolationApi(ContainmentStore store, ResidentLocationApi residents, LocationStructureApi structure) {
        this.store = store;
        this.residents = residents;
        this.structure = structure;
    }

    @Override
    public List<IsolationZoneView> activeByRoom(long roomId, Instant at) {
        return structure
                .room(roomId)
                .map(room -> activeByFloorArea(room.floorId(), at))
                .orElse(List.of());
    }

    @Override
    public List<IsolationZoneView> activeByFloorArea(long floorAreaId, Instant at) {
        return activeAt(at, row -> coversFloor(row.content(), floorAreaId));
    }

    @Override
    public List<IsolationZoneView> activeByResident(long residentId, Instant at) {
        return residents
                .at(residentId, at)
                .map(location -> activeAt(at, row -> {
                    var scope = row.content().period().scope();
                    return (location.areaId() != null && scope.areaIds().contains(location.areaId()))
                            || (location.floorId() != null && coversFloor(row.content(), location.floorId()));
                }))
                .orElse(List.of());
    }

    @Override
    public List<IsolationZoneView> activeZones() {
        return store.list().stream()
                .filter(row -> row.content().period().releasedAt() == null)
                .map(DefaultIsolationApi::view)
                .toList();
    }

    private List<IsolationZoneView> activeAt(Instant at, Predicate<SimpleRecord<ContainmentContent>> matches) {
        return store.list().stream()
                .filter(row -> row.content().period().includes(at))
                .filter(matches)
                .map(DefaultIsolationApi::view)
                .toList();
    }

    private boolean coversFloor(ContainmentContent content, long floorId) {
        var scope = content.period().scope();
        return scope.floorIds().contains(floorId)
                || scope.areaIds().stream()
                        .anyMatch(area -> structure.floorsInArea(area).contains(floorId));
    }

    private static IsolationZoneView view(SimpleRecord<ContainmentContent> row) {
        var period = row.content().period();
        return new IsolationZoneView(
                row.id(), period.scope().floorIds(), period.scope().areaIds(), period.startedAt(), period.releasedAt());
    }
}
