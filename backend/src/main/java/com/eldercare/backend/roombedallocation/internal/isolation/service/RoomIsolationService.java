package com.eldercare.backend.roombedallocation.internal.isolation.service;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import com.eldercare.backend.roombedallocation.api.events.*;
import com.eldercare.backend.roombedallocation.api.spi.AllocationInfectionStatus;
import com.eldercare.backend.roombedallocation.internal.isolation.domain.RoomIsolationFact;
import com.eldercare.backend.roombedallocation.internal.isolation.persistence.RoomIsolationStore;
import com.eldercare.backend.roombedallocation.internal.structure.domain.Room;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** T054/FR-008: lệnh phòng không thay trạng thái giường hoặc sinh phiếu nghiệp vụ. */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class RoomIsolationService {
    private final RoomIsolationStore store;
    private final ObjectProvider<AllocationInfectionStatus> infection;
    private final LocationStructureApi locations;
    private final BusinessTime time;
    private final CurrentUser user;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;

    public RoomIsolationService(
            RoomIsolationStore store,
            ObjectProvider<AllocationInfectionStatus> infection,
            LocationStructureApi locations,
            BusinessTime time,
            CurrentUser user,
            AuditLogApi audit,
            ApplicationEventPublisher events) {
        this.store = store;
        this.infection = infection;
        this.locations = locations;
        this.time = time;
        this.user = user;
        this.audit = audit;
        this.events = events;
    }

    @RequiresPermission(function = Function.ROOM_ISOLATION, operation = Operation.CREATE)
    @BusinessCommand
    public Room isolate(long roomId, Reason reason, Long incidentId) {
        return change(roomId, true, reason, incidentId);
    }

    @RequiresPermission(function = Function.ROOM_ISOLATION, operation = Operation.UPDATE)
    @BusinessCommand
    public void lift(long roomId, Reason reason) {
        change(roomId, false, reason, null);
    }

    @RequiresPermission(function = Function.ROOM_ISOLATION, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<RoomIsolationFact> history(long roomId) {
        store.room(roomId);
        return store.history(roomId);
    }

    @RequiresPermission(function = Function.ROOM_ISOLATION, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<com.eldercare.backend.roombedallocation.internal.isolation.domain.IsolationRoom> rooms() {
        var source = infection.getIfAvailable();
        var cordon = source == null ? null : source.cordonedAt(time.now());
        return store.rooms().stream()
                .map(room -> {
                    Boolean blocked = room.isolated()
                            ? Boolean.TRUE
                            : cordon == null
                                    ? null
                                    : cordon.floorIds().contains(room.floorId())
                                            || locations
                                                    .floor(room.floorId())
                                                    .map(f -> cordon.areaIds().contains(f.areaId()))
                                                    .orElse(false);
                    return new com.eldercare.backend.roombedallocation.internal.isolation.domain.IsolationRoom(
                            room, blocked);
                })
                .toList();
    }

    private Room change(long roomId, boolean isolated, Reason reason, Long incidentId) {
        store.lock(roomId);
        var room = store.room(roomId);
        if (room.isolated() == isolated)
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    isolated ? "ROOM_ALREADY_ISOLATED" : "ROOM_NOT_ISOLATED",
                    "Phòng đã ở trạng thái cách ly yêu cầu");
        boolean stillCordoned = false;
        if (!isolated) {
            var source = infection.getIfAvailable();
            if (source == null)
                throw new BusinessException(
                        HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_UNAVAILABLE", "Chưa có nguồn khoanh vùng007");
            var cordon = source.cordonedAt(time.now());
            long areaId = locations.floor(room.floorId()).orElseThrow().areaId();
            stillCordoned = cordon.floorIds().contains(room.floorId())
                    || cordon.areaIds().contains(areaId);
        }
        store.change(roomId, isolated, time.now(), user.accountId(), reason.value(), incidentId);
        if (!isolated && !stillCordoned) store.clearDoctorFlags(roomId);
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("M03_ROOM_ISOLATION", true))
                .entity("ROOM", roomId)
                .before(Map.of("isolated", !isolated))
                .after(Map.of("isolated", isolated))
                .reason(reason)
                .build());
        if (isolated) events.publishEvent(new RoomIsolated(roomId, incidentId));
        else events.publishEvent(new RoomIsolationLifted(roomId));
        return store.room(roomId);
    }
}
