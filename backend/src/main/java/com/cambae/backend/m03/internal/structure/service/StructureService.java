package com.cambae.backend.m03.internal.structure.service;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.data.BusinessCommand;
import com.cambae.backend.common.data.CatalogBase;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.ConditionNotMetException;
import com.cambae.backend.common.error.UnmetCondition;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m03.api.dto.LocationRef;
import com.cambae.backend.m03.api.spi.LocationReferences;
import com.cambae.backend.m03.internal.structure.domain.Area;
import com.cambae.backend.m03.internal.structure.domain.BedTransferReason;
import com.cambae.backend.m03.internal.structure.domain.Floor;
import com.cambae.backend.m03.internal.structure.domain.Room;
import com.cambae.backend.m03.internal.structure.domain.RoomType;
import com.cambae.backend.m03.internal.structure.domain.StructureChange;
import com.cambae.backend.m03.internal.structure.domain.StructureKind;
import com.cambae.backend.m03.internal.structure.persistence.StructureStore;
import com.cambae.backend.m15.account.api.RequiresPermission;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T015: lệnh danh mục theo FR-001–FR-007, quyền cấu hình thật và nhật ký cùng transaction. */
@Service
public class StructureService {
    private final StructureStore store;
    private final ObjectProvider<LocationReferences> references;
    private final AuditLogApi audit;
    private final com.cambae.backend.m03.internal.allocation.service.ResidentAllocationFacts residentFacts;

    public StructureService(
            StructureStore store,
            ObjectProvider<LocationReferences> references,
            AuditLogApi audit,
            com.cambae.backend.m03.internal.allocation.service.ResidentAllocationFacts residentFacts) {
        this.store = store;
        this.references = references;
        this.audit = audit;
        this.residentFacts = residentFacts;
    }

    @RequiresPermission(function = Function.ROOM_BED_CONFIGURATION, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public CatalogBase create(StructureKind kind, StructureChange change) {
        validate(change, kind);
        CatalogBase entity = switch (kind) {
            case AREA -> new Area(change.code(), change.name());
            case FLOOR -> {
                active(StructureKind.AREA, change.parentId());
                yield new Floor(change.parentId(), change.code(), change.name(), change.buildingName());
            }
            case ROOM_TYPE ->
                new RoomType(
                        change.name(),
                        change.standardBeds(),
                        change.careLevels(),
                        change.servesLongTerm(),
                        change.servesShortTerm());
            case ROOM -> {
                active(StructureKind.FLOOR, change.parentId());
                var type = (RoomType) active(StructureKind.ROOM_TYPE, change.roomTypeId());
                yield new Room(
                        change.parentId(),
                        change.roomTypeId(),
                        change.code(),
                        change.name(),
                        change.genderPolicy(),
                        change.careLevels() == null ? type.careLevels() : change.careLevels());
            }
            case BED_TRANSFER_REASON ->
                new BedTransferReason(change.code(), change.name(), Boolean.TRUE.equals(change.medicalSafety()));
        };
        try {
            store.save(entity);
        } catch (DataIntegrityViolationException error) {
            throw duplicate();
        }
        log(kind, entity, "CREATED", Map.of(), snapshot(entity), change.reason());
        return entity;
    }

    @RequiresPermission(function = Function.ROOM_BED_CONFIGURATION, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public CatalogBase update(StructureKind kind, long id, String ifMatch, StructureChange change) {
        validate(change, kind);
        var entity = store.find(kind, id, true);
        VersionEtag.requireMatch(ifMatch, entity.getVersion(), snapshot(entity));
        var before = snapshot(entity);
        switch (entity) {
            case Area a -> a.revise(change.code(), change.name());
            case Floor f -> {
                if (f.areaId() != change.parentId()) {
                    if (store.hasAllocationHistory(StructureKind.FLOOR, id))
                        throw condition("FR-007", "Không đổi khu vực của tầng đã có lịch sử phân bổ");
                    active(StructureKind.AREA, change.parentId());
                }
                f.revise(change.parentId(), change.code(), change.name(), change.buildingName());
            }
            case RoomType t ->
                t.revise(
                        change.name(),
                        change.standardBeds(),
                        change.careLevels(),
                        change.servesLongTerm(),
                        change.servesShortTerm());
            case Room r -> {
                store.lockRoomBeds(id);
                if (r.floorId() != change.parentId()) {
                    if (store.hasAllocationHistory(StructureKind.ROOM, id))
                        throw condition("FR-007", "Không đổi tầng của phòng đã có lịch sử phân bổ");
                    active(StructureKind.FLOOR, change.parentId());
                }
                var type = (RoomType) store.find(StructureKind.ROOM_TYPE, change.roomTypeId(), true);
                if (r.roomTypeId() != change.roomTypeId() && !type.isActive())
                    throw condition("FR-004", "Loại phòng đã ngừng hiệu lực");
                var levels = change.careLevels() == null ? r.careLevels() : change.careLevels();
                if (r.genderPolicy() != change.genderPolicy() || !r.careLevels().equals(levels)) {
                    var residents = store.allocatedResidents(id);
                    List<UnmetCondition> violations = new ArrayList<>();
                    for (long resident : residents) {
                        if (!residentFacts.genderAllowed(resident, change.genderPolicy()))
                            violations.add(new UnmetCondition(
                                    "FR-006", "Người cao tuổi #" + resident + " không phù hợp chính sách giới tính"));
                        if (residentFacts
                                .careLevel(resident)
                                .filter(level -> levels.contains(level.level()))
                                .isEmpty())
                            violations.add(new UnmetCondition(
                                    "FR-006", "Người cao tuổi #" + resident + " không phù hợp mức chăm sóc của phòng"));
                    }
                    if (!violations.isEmpty()) throw new ConditionNotMetException(violations);
                }
                r.revise(
                        change.parentId(),
                        change.roomTypeId(),
                        change.code(),
                        change.name(),
                        change.genderPolicy(),
                        levels);
            }
            case BedTransferReason r -> {
                if (r.fixed() && r.medicalSafety() != Boolean.TRUE.equals(change.medicalSafety()))
                    throw condition("FR-030", "Dòng lý do cố định không đổi cờ y tế/an toàn");
                r.revise(change.code(), change.name(), Boolean.TRUE.equals(change.medicalSafety()));
            }
            default -> throw new IllegalArgumentException("Danh mục không thuộc cơ cấu");
        }
        try {
            store.flush();
        } catch (DataIntegrityViolationException error) {
            throw duplicate();
        }
        log(kind, entity, "UPDATED", before, snapshot(entity), change.reason());
        return entity;
    }

    @RequiresPermission(function = Function.ROOM_BED_CONFIGURATION, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public CatalogBase detail(StructureKind kind, long id) {
        return store.find(kind, id, false);
    }

    @RequiresPermission(function = Function.ROOM_BED_CONFIGURATION, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<CatalogBase> list(StructureKind kind) {
        return store.list(kind);
    }

    @RequiresPermission(function = Function.ROOM_BED_CONFIGURATION, operation = Operation.UPDATE)
    @BusinessCommand
    @Transactional
    public CatalogBase deactivate(StructureKind kind, long id, String ifMatch, Reason reason) {
        store.lockDayCareGuard(kind);
        var entity = store.find(kind, id, true);
        VersionEtag.requireMatch(ifMatch, entity.getVersion(), snapshot(entity));
        if (entity instanceof BedTransferReason r && r.fixed())
            throw condition("FR-030", "Không ngừng hiệu lực lý do cố định");
        List<UnmetCondition> blockers = new ArrayList<>(store.activeChildren(kind, id));
        if (isLocation(kind))
            references()
                    .forEach(provider -> provider.currentReferences(new LocationRef(kind.name(), id))
                            .forEach(ref -> blockers.add(new UnmetCondition("FR-005", ref.label() + " #" + ref.id()))));
        if (!blockers.isEmpty()) throw new ConditionNotMetException(blockers);
        var before = snapshot(entity);
        entity.deactivate();
        store.flush();
        log(kind, entity, "DEACTIVATED", before, snapshot(entity), reason);
        return entity;
    }

    @RequiresPermission(function = Function.ROOM_BED_CONFIGURATION, operation = Operation.UPDATE)
    @BusinessCommand
    @Transactional
    public CatalogBase reactivate(StructureKind kind, long id, String ifMatch, Reason reason) {
        var entity = store.find(kind, id, true);
        VersionEtag.requireMatch(ifMatch, entity.getVersion(), snapshot(entity));
        if (entity instanceof Floor floor) active(StructureKind.AREA, floor.areaId());
        if (entity instanceof Room room) active(StructureKind.FLOOR, room.floorId());
        var before = snapshot(entity);
        Optional<CatalogBase> sameName = (kind == StructureKind.ROOM_TYPE || kind == StructureKind.BED_TRANSFER_REASON)
                ? store.list(kind).stream()
                        .filter(e ->
                                e.isActive() && e.getId() != id && e.getName().equalsIgnoreCase(entity.getName()))
                        .findFirst()
                : Optional.empty();
        entity.reactivate(sameName);
        try {
            store.flush();
        } catch (DataIntegrityViolationException error) {
            throw duplicate();
        }
        log(kind, entity, "REACTIVATED", before, snapshot(entity), reason);
        return entity;
    }

    @RequiresPermission(function = Function.ROOM_BED_CONFIGURATION, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void delete(StructureKind kind, long id, String ifMatch) {
        store.lockDayCareGuard(kind);
        var entity = store.find(kind, id, true);
        VersionEtag.requireMatch(ifMatch, entity.getVersion(), snapshot(entity));
        if (entity instanceof BedTransferReason r && r.fixed()) throw condition("FR-030", "Không xóa lý do cố định");
        if (store.hasAllocationHistory(kind, id)
                || (isLocation(kind)
                        && references().stream().anyMatch(p -> p.everReferenced(new LocationRef(kind.name(), id)))))
            throw condition("FR-005", "Danh mục đã từng được tham chiếu");
        var before = snapshot(entity);
        try {
            store.remove(entity);
        } catch (DataIntegrityViolationException error) {
            throw condition("FR-005", "Danh mục đã được tham chiếu");
        }
        log(kind, entity, "DELETED", before, Map.of(), null);
    }

    private List<LocationReferences> references() {
        var list = references.orderedStream().toList();
        if (list.isEmpty())
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_UNAVAILABLE", "Chưa có nguồn kiểm tham chiếu vị trí");
        return list;
    }

    private boolean isLocation(StructureKind kind) {
        return kind == StructureKind.AREA || kind == StructureKind.FLOOR || kind == StructureKind.ROOM;
    }

    private CatalogBase active(StructureKind kind, long id) {
        var entity = store.find(kind, id, true);
        if (!entity.isActive()) throw condition("FR-004", "Vị trí hoặc loại phòng đã ngừng hiệu lực #" + id);
        return entity;
    }

    private void validate(StructureChange c, StructureKind kind) {
        if (c.name() == null || c.name().isBlank() || c.name().length() > 100) throw invalid();
        if (kind != StructureKind.ROOM_TYPE
                && (c.code() == null
                        || c.code().isBlank()
                        || c.code().length() > (kind == StructureKind.BED_TRANSFER_REASON ? 30 : 20))) throw invalid();
        if ((kind == StructureKind.FLOOR || kind == StructureKind.ROOM) && c.parentId() == null) throw invalid();
        if (kind == StructureKind.ROOM && (c.roomTypeId() == null || c.genderPolicy() == null)) throw invalid();
        if (kind == StructureKind.ROOM_TYPE
                && (c.standardBeds() == null
                        || c.standardBeds() < 1
                        || c.standardBeds() > 32767
                        || c.careLevels() == null
                        || c.careLevels().isEmpty()
                        || c.servesLongTerm() == null
                        || c.servesShortTerm() == null
                        || (!c.servesLongTerm() && !c.servesShortTerm()))) throw invalid();
        if (kind == StructureKind.ROOM
                && c.careLevels() != null
                && c.careLevels().isEmpty()) throw invalid();
    }

    private BusinessException invalid() {
        return new BusinessException(
                HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_STRUCTURE", "Thông tin cơ cấu không hợp lệ");
    }

    private BusinessException duplicate() {
        return new BusinessException(
                HttpStatus.CONFLICT, "DUPLICATE_CODE", "Mã hoặc tên danh mục đang hiệu lực đã tồn tại");
    }

    private ConditionNotMetException condition(String code, String message) {
        return new ConditionNotMetException(List.of(new UnmetCondition(code, message)));
    }

    private Map<String, Object> snapshot(CatalogBase e) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("version", e.getVersion());
        result.put("name", e.getName());
        result.put("status", e.getStatus().name());
        switch (e) {
            case Area a -> result.put("code", a.code());
            case Floor f -> {
                result.put("areaId", f.areaId());
                result.put("code", f.code());
                result.put("buildingName", f.buildingName());
            }
            case RoomType t -> {
                result.put("standardBeds", t.standardBeds());
                result.put("careLevels", t.careLevels());
                result.put("servesLongTerm", t.servesLongTerm());
                result.put("servesShortTerm", t.servesShortTerm());
            }
            case Room r -> {
                result.put("floorId", r.floorId());
                result.put("roomTypeId", r.roomTypeId());
                result.put("code", r.code());
                result.put("genderPolicy", r.genderPolicy());
                result.put("careLevels", r.careLevels());
            }
            case BedTransferReason r -> {
                result.put("code", r.code());
                result.put("medicalSafety", r.medicalSafety());
            }
            default -> throw new IllegalArgumentException("Danh mục không thuộc cơ cấu");
        }
        return result;
    }

    private void log(
            StructureKind kind,
            CatalogBase entity,
            String action,
            Map<String, Object> before,
            Map<String, Object> after,
            Reason reason) {
        Map<String, Object> oldValues = new LinkedHashMap<>(), newValues = new LinkedHashMap<>();
        var keys = new java.util.LinkedHashSet<>(before.keySet());
        keys.addAll(after.keySet());
        for (var key : keys)
            if (!key.equals("version") && !Objects.equals(before.get(key), after.get(key))) {
                oldValues.put(key, before.get(key));
                newValues.put(key, after.get(key));
            }
        String entityType = kind == StructureKind.BED_TRANSFER_REASON ? "TRANSFER_REASON" : kind.name();
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("M03_STRUCTURE_" + action, reason != null))
                .entity("M03." + entityType, entity.getId())
                .before(oldValues)
                .after(newValues)
                .reason(reason)
                .build());
    }
}
