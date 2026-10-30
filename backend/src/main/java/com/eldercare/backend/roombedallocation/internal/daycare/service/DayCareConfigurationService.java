package com.eldercare.backend.roombedallocation.internal.daycare.service;

import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.*;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import com.eldercare.backend.roombedallocation.api.dto.LocationRef;
import com.eldercare.backend.roombedallocation.internal.daycare.domain.*;
import com.eldercare.backend.roombedallocation.internal.daycare.persistence.DayCareStore;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** T051: lệnh danh mục cùng khóa với bên đăng ký; mọi thay đổi ghi nhật ký cùng transaction. */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class DayCareConfigurationService {
    private final DayCareStore store;
    private final DayCareRecounting recount;
    private final LocationStructureApi locations;
    private final AuditLogApi audit;

    public DayCareConfigurationService(
            DayCareStore store, DayCareRecounting recount, LocationStructureApi locations, AuditLogApi audit) {
        this.store = store;
        this.recount = recount;
        this.locations = locations;
        this.audit = audit;
    }

    @RequiresPermission(function = Function.ROOM_BED_CONFIGURATION, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<DayCareAreaState> areas() {
        return store.areas(false);
    }

    @RequiresPermission(function = Function.ROOM_BED_CONFIGURATION, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<DayCareSessionState> sessions() {
        return store.sessions(false);
    }

    @RequiresPermission(function = Function.ROOM_BED_CONFIGURATION, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<SessionCapacityRow> counts(LocalDate from, LocalDate to) {
        return recount.counts(from, to);
    }

    @RequiresPermission(function = Function.ROOM_BED_CONFIGURATION, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    public DayCareAreaState createArea(String code, String name, Long floorId, Long areaId) {
        store.lock();
        validateArea(code, name, floorId, areaId);
        if (store.activeArea(true).isPresent()) throw conflict("DAY_CARE_REST_AREA_EXISTS", "Đã có khu nghỉ Hiệu lực");
        var entity = new DayCareRestArea(code, name, floorId, areaId);
        store.save(entity);
        var result = area(entity.getId());
        log("CREATED", "DAY_CARE_REST_AREA", entity.getId(), Map.of(), areaValues(result), null);
        return result;
    }

    @RequiresPermission(function = Function.ROOM_BED_CONFIGURATION, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    public DayCareAreaState updateArea(long id, String etag, String code, String name, Long floorId, Long areaId) {
        store.lock();
        var entity = store.find(DayCareRestArea.class, id);
        VersionEtag.requireMatch(etag, entity.getVersion(), area(id));
        validateArea(code, name, floorId, areaId);
        var before = areaValues(area(id));
        entity.revise(code, name, floorId, areaId);
        store.flush();
        var result = area(id);
        log("UPDATED", "DAY_CARE_REST_AREA", id, before, areaValues(result), null);
        return result;
    }

    @RequiresPermission(function = Function.ROOM_BED_CONFIGURATION, operation = Operation.UPDATE)
    @BusinessCommand
    public DayCareAreaState deactivateArea(long id, String etag, Reason reason, LocalDate today) {
        store.lock();
        var entity = store.find(DayCareRestArea.class, id);
        VersionEtag.requireMatch(etag, entity.getVersion(), area(id));
        if (recount.source().hasFutureArrivals(today))
            throw conflict("DAY_CARE_FUTURE_ARRIVALS", "Còn lịch đến hiện tại hoặc tương lai");
        var before = areaValues(area(id));
        entity.deactivate();
        store.flush();
        var result = area(id);
        log("DEACTIVATED", "DAY_CARE_REST_AREA", id, before, areaValues(result), reason);
        return result;
    }

    @RequiresPermission(function = Function.ROOM_BED_CONFIGURATION, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    public DayCareSessionChange createSession(String name, LocalTime start, LocalTime end) {
        store.lock();
        validateSession(null, name, start, end);
        var entity = new DayCareSessionEntry(name, start, end);
        store.save(entity);
        var result = session(entity.getId());
        log("CREATED", "DAY_CARE_SESSION", entity.getId(), Map.of(), sessionValues(result), null);
        return new DayCareSessionChange(result, recount.recount());
    }

    @RequiresPermission(function = Function.ROOM_BED_CONFIGURATION, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    public DayCareSessionChange updateSession(long id, String etag, String name, LocalTime start, LocalTime end) {
        store.lock();
        var entity = store.find(DayCareSessionEntry.class, id);
        VersionEtag.requireMatch(etag, entity.getVersion(), session(id));
        validateSession(entity.isActive() ? id : -1L, name, start, end);
        var before = sessionValues(session(id));
        entity.revise(name, start, end);
        store.flush();
        var result = session(id);
        log("UPDATED", "DAY_CARE_SESSION", id, before, sessionValues(result), null);
        return new DayCareSessionChange(result, recount.recount());
    }

    @RequiresPermission(function = Function.ROOM_BED_CONFIGURATION, operation = Operation.UPDATE)
    @BusinessCommand
    public DayCareSessionState deactivateSession(long id, String etag, Reason reason) {
        store.lock();
        var entity = store.find(DayCareSessionEntry.class, id);
        VersionEtag.requireMatch(etag, entity.getVersion(), session(id));
        var before = sessionValues(session(id));
        entity.deactivate();
        store.flush();
        var result = session(id);
        log("DEACTIVATED", "DAY_CARE_SESSION", id, before, sessionValues(result), reason);
        recount.recount();
        return result;
    }

    private DayCareAreaState area(long id) {
        return store.areas(true).stream().filter(x -> x.id() == id).findFirst().orElseThrow(NotFoundException::new);
    }

    private DayCareSessionState session(long id) {
        return store.sessions(true).stream()
                .filter(x -> x.id() == id)
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    private void validateArea(String code, String name, Long floor, Long area) {
        if (code == null
                || code.isBlank()
                || code.length() > 30
                || name == null
                || name.isBlank()
                || name.length() > 100
                || (floor == null) == (area == null))
            throw new InvalidValueException("Khu nghỉ cần mã, tên và đúng một tầng hoặc khu vực");
        if (!locations.active(floor != null ? new LocationRef("FLOOR", floor) : new LocationRef("AREA", area)))
            throw conflict("DAY_CARE_LOCATION_INACTIVE", "Vị trí khu nghỉ phải Hiệu lực");
    }

    private void validateSession(Long id, String name, LocalTime start, LocalTime end) {
        if (name == null
                || name.isBlank()
                || name.length() > 50
                || start == null
                || end == null
                || !end.isAfter(start)
                || start.getSecond() != 0
                || end.getSecond() != 0
                || start.getNano() != 0
                || end.getNano() != 0) throw new InvalidValueException("Tên hoặc giờ buổi không hợp lệ");
        if (Objects.equals(id, -1L)) return;
        for (var item : store.sessions(true))
            if ("ACTIVE".equals(item.status()) && !Objects.equals(item.id(), id)) {
                if (item.name().equalsIgnoreCase(name))
                    throw conflict("DAY_CARE_SESSION_NAME_EXISTS", "Tên buổi Hiệu lực đã tồn tại");
                if (start.isBefore(item.endTime()) && end.isAfter(item.startTime()))
                    throw conflict("DAY_CARE_SESSION_OVERLAP", "Khung giờ chồng buổi Hiệu lực khác");
            }
    }

    private Map<String, Object> areaValues(DayCareAreaState value) {
        var values = new LinkedHashMap<String, Object>();
        values.put("code", value.code());
        values.put("name", value.name());
        values.put("floorId", value.floorId());
        values.put("areaId", value.areaId());
        values.put("status", value.status());
        return values;
    }

    private Map<String, Object> sessionValues(DayCareSessionState value) {
        return Map.of(
                "name",
                value.name(),
                "startTime",
                value.startTime().toString(),
                "endTime",
                value.endTime().toString(),
                "status",
                value.status());
    }

    private void log(
            String action, String type, long id, Map<String, Object> before, Map<String, Object> after, Reason reason) {
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("M03_DAY_CARE_" + action, reason != null))
                .entity(type, id)
                .before(before)
                .after(after)
                .reason(reason)
                .build());
    }

    private BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }
}
