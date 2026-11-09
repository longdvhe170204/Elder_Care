package com.eldercare.backend.roombedallocation.internal.isolation.service;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.roombedallocation.api.InfectionControlCommandApi;
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import com.eldercare.backend.roombedallocation.api.dto.BedNotificationContext;
import com.eldercare.backend.roombedallocation.api.events.FormerOccupantListed;
import com.eldercare.backend.roombedallocation.api.spi.AllocationInfectionStatus;
import com.eldercare.backend.roombedallocation.internal.isolation.persistence.RoomIsolationStore;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * T053, T054 (FR-024a (c), FR-045; research G-08; chủ dự án duyệt 2026-11-09): bản cài {@link InfectionControlCommandApi}
 * 007 gọi trong lệnh gỡ khoanh vùng, thêm vào danh sách tiếp xúc. Không nghe sự kiện của 007 (tránh vòng module).
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class InfectionControlCommands implements InfectionControlCommandApi {
    private final RoomIsolationStore store;
    private final ObjectProvider<AllocationInfectionStatus> infection;
    private final LocationStructureApi locations;
    private final ApplicationEventPublisher events;

    public InfectionControlCommands(
            RoomIsolationStore store,
            ObjectProvider<AllocationInfectionStatus> infection,
            LocationStructureApi locations,
            ApplicationEventPublisher events) {
        this.store = store;
        this.infection = infection;
        this.locations = locations;
        this.events = events;
    }

    @Override
    public void cordonLifted(Set<Long> floorIds, Set<Long> areaIds, Instant at) {
        var source = infection.getIfAvailable();
        if (source == null)
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_UNAVAILABLE", "Chưa có nguồn khoanh vùng 007");
        var cordon = source.cordonedAt(at);
        var floors = new java.util.TreeSet<>(floorIds);
        areaIds.forEach(area -> floors.addAll(locations.floorsInArea(area)));
        for (var room : store.roomsOnFloors(floors)) {
            Long area = locations.floor(room.floorId()).map(f -> f.areaId()).orElse(null);
            boolean blocked = room.isolated()
                    || cordon.floorIds().contains(room.floorId())
                    || (area != null && cordon.areaIds().contains(area));
            if (blocked) continue;
            store.lock(room.getId());
            store.clearDoctorFlags(room.getId());
        }
    }

    @Override
    public void contactListed(long residentId, Instant at) {
        for (var bed : store.awaitingReadyBedsLeftBy(residentId))
            // Mốc là lúc giường bắt đầu Chờ sẵn sàng: khóa thông báo ổn định, 009 không báo lặp cho cùng lần rời
            // giường.
            events.publishEvent(new FormerOccupantListed(
                    bed.bedId(),
                    residentId,
                    new BedNotificationContext(
                            bed.awaitingSince(),
                            bed.floorId(),
                            List.of(new RecipientGroup.FloorManager(bed.floorId(), new Substitution.Default())))));
    }
}
