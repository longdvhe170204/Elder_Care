package com.cambae.backend.m03.internal.structure.persistence;

import com.cambae.backend.common.data.CatalogBase;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.UnmetCondition;
import com.cambae.backend.m03.internal.structure.domain.Area;
import com.cambae.backend.m03.internal.structure.domain.BedTransferReason;
import com.cambae.backend.m03.internal.structure.domain.Floor;
import com.cambae.backend.m03.internal.structure.domain.Room;
import com.cambae.backend.m03.internal.structure.domain.RoomType;
import com.cambae.backend.m03.internal.structure.domain.StructureKind;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** T015: JPA quản lý danh mục; các truy vấn kiểm tham chiếu chỉ đọc bảng M03. */
@Repository
public class StructureStore {
    private final EntityManager entities;
    private final JdbcTemplate jdbc;

    public StructureStore(EntityManager entities, JdbcTemplate jdbc) {
        this.entities = entities;
        this.jdbc = jdbc;
    }

    private Class<? extends CatalogBase> type(StructureKind kind) {
        return switch (kind) {
            case AREA -> Area.class;
            case FLOOR -> Floor.class;
            case ROOM_TYPE -> RoomType.class;
            case ROOM -> Room.class;
            case BED_TRANSFER_REASON -> BedTransferReason.class;
        };
    }

    public CatalogBase find(StructureKind kind, long id, boolean lock) {
        var value =
                lock ? entities.find(type(kind), id, LockModeType.PESSIMISTIC_WRITE) : entities.find(type(kind), id);
        if (value == null) throw new NotFoundException();
        initialize(value);
        return value;
    }

    private void initialize(CatalogBase value) {
        if (value instanceof Room r) r.careLevels();
        if (value instanceof RoomType t) t.careLevels();
    }

    public List<CatalogBase> list(StructureKind kind) {
        return entities
                .createQuery("SELECT x FROM " + type(kind).getSimpleName() + " x ORDER BY x.id", CatalogBase.class)
                .getResultList()
                .stream()
                .peek(this::initialize)
                .toList();
    }

    public CatalogBase save(CatalogBase entity) {
        entities.persist(entity);
        entities.flush();
        return entity;
    }

    public void flush() {
        entities.flush();
    }

    public void remove(CatalogBase entity) {
        entities.remove(entity);
        entities.flush();
    }

    public boolean tableExists(String name) {
        return jdbc.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=?",
                        Integer.class,
                        name)
                > 0;
    }

    public List<UnmetCondition> activeChildren(StructureKind kind, long id) {
        String query = switch (kind) {
            case AREA -> "SELECT id, name FROM floor WHERE area_id=? AND status='ACTIVE' ORDER BY id";
            case FLOOR -> "SELECT id, name FROM room WHERE floor_id=? AND status='ACTIVE' ORDER BY id";
            case ROOM -> "SELECT id,code FROM bed WHERE room_id=? AND state<>'RETIRED' ORDER BY id";
            default -> null;
        };
        if (query == null) return List.of();
        return jdbc.query(query, (rs, i) -> new UnmetCondition("FR-005", rs.getString(2) + " #" + rs.getLong(1)), id);
    }

    public boolean hasAllocationHistory(StructureKind kind, long id) {
        if (!tableExists("bed_allocation")) return false;
        String where = switch (kind) {
            case AREA -> "f.area_id=?";
            case FLOOR -> "r.floor_id=?";
            case ROOM -> "b.room_id=?";
            default -> null;
        };
        if (where == null) return false;
        return jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed_allocation a JOIN bed b ON b.id=a.bed_id JOIN room r ON r.id=b.room_id JOIN floor f ON f.id=r.floor_id WHERE "
                                + where,
                        Integer.class,
                        id)
                > 0;
    }

    public void lockRoomBeds(long roomId) {
        jdbc.queryForList("SELECT id FROM bed WHERE room_id=? ORDER BY id FOR UPDATE", Long.class, roomId);
    }

    public List<Long> allocatedResidents(long roomId) {
        if (!tableExists("bed_allocation")) return List.of();
        return jdbc.queryForList(
                "SELECT DISTINCT a.resident_id FROM v_current_allocation a WHERE a.room_id=? AND a.status IN ('ACTIVE','FUTURE') ORDER BY a.resident_id",
                Long.class,
                roomId);
    }

    public boolean activeLocation(com.cambae.backend.m03.api.dto.LocationRef location) {
        String sql = switch (location.type()) {
            case "AREA" -> "SELECT COUNT(*) FROM area a WHERE a.id=? AND a.status='ACTIVE'";
            case "FLOOR" ->
                "SELECT COUNT(*) FROM floor f JOIN area a ON a.id=f.area_id WHERE f.id=? AND f.status='ACTIVE' AND a.status='ACTIVE'";
            case "ROOM" ->
                "SELECT COUNT(*) FROM room r JOIN floor f ON f.id=r.floor_id JOIN area a ON a.id=f.area_id WHERE r.id=? AND r.status='ACTIVE' AND f.status='ACTIVE' AND a.status='ACTIVE'";
            case "BED" ->
                "SELECT COUNT(*) FROM bed b JOIN room r ON r.id=b.room_id JOIN floor f ON f.id=r.floor_id JOIN area a ON a.id=f.area_id WHERE b.id=? AND b.state<>'RETIRED' AND r.status='ACTIVE' AND f.status='ACTIVE' AND a.status='ACTIVE'";
            default -> null;
        };
        return sql != null && jdbc.queryForObject(sql, Integer.class, location.id()) > 0;
    }

    public java.util.Set<Long> floorIds(long areaId) {
        return java.util.Set.copyOf(
                jdbc.queryForList("SELECT id FROM floor WHERE area_id=? ORDER BY id", Long.class, areaId));
    }

    public java.util.Optional<com.cambae.backend.m03.api.dto.LocationRef> dayCareLocation() {
        if (!tableExists("day_care_rest_area")) return java.util.Optional.empty();
        return jdbc
                .query(
                        "SELECT floor_id,area_id FROM day_care_rest_area WHERE status='ACTIVE'",
                        (rs, i) -> rs.getObject(1) == null
                                ? new com.cambae.backend.m03.api.dto.LocationRef("AREA", rs.getLong(2))
                                : new com.cambae.backend.m03.api.dto.LocationRef("FLOOR", rs.getLong(1)))
                .stream()
                .findFirst();
    }

    public boolean currentRoomAllows(long residentId, com.cambae.backend.m03.api.spi.CareLevelCode level) {
        if (!tableExists("bed_allocation")) return true;
        return jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed_allocation a JOIN bed b ON b.id=a.bed_id WHERE a.resident_id=? AND a.status='ACTIVE' AND NOT EXISTS(SELECT 1 FROM room_allowed_care_level l WHERE l.room_id=b.room_id AND l.care_level=?)",
                        Integer.class,
                        residentId,
                        level.name())
                == 0;
    }

    public boolean hasFutureAllocation(long bedId) {
        return tableExists("bed_allocation")
                && jdbc.queryForObject(
                                "SELECT COUNT(*) FROM bed_allocation WHERE bed_id=? AND status='FUTURE'",
                                Integer.class,
                                bedId)
                        > 0;
    }

    public boolean bedEverAllocated(long bedId) {
        return tableExists("bed_allocation")
                && jdbc.queryForObject("SELECT COUNT(*) FROM bed_allocation WHERE bed_id=?", Integer.class, bedId) > 0;
    }
}
