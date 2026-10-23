package com.cambae.backend.m03.internal.bed.persistence;

import com.cambae.backend.m03.internal.bed.domain.Bed;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** T016: ghi thuộc tính giường, không có SQL ghi cột state. */
@Repository
public class BedStore {
    private final EntityManager entities;
    private final JdbcTemplate jdbc;

    public BedStore(EntityManager entities, JdbcTemplate jdbc) {
        this.entities = entities;
        this.jdbc = jdbc;
    }

    public Bed create(long roomId, String code) {
        var bed = new Bed(roomId, code);
        entities.persist(bed);
        entities.flush();
        return bed;
    }

    public long usableBeds(long roomId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM bed WHERE room_id=? AND state<>'RETIRED'", Long.class, roomId);
    }

    public void refresh(Bed bed) {
        entities.refresh(bed);
    }

    public Bed lock(long id) {
        var bed = entities.find(Bed.class, id, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (bed == null) throw new com.cambae.backend.common.error.NotFoundException();
        return bed;
    }

    public void flush() {
        entities.flush();
    }

    public java.util.Optional<com.cambae.backend.m03.internal.bed.domain.BedDetails> details(long id) {
        return rows(" WHERE b.id=?", java.util.List.of(id)).stream().findFirst();
    }

    public java.util.List<com.cambae.backend.m03.internal.bed.domain.BedDetails> list(
            Long areaId, Long floorId, Long roomId, java.util.List<com.cambae.backend.m03.api.dto.BedStatus> states) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        java.util.List<Object> args = new java.util.ArrayList<>();
        if (areaId != null) {
            where.append(" AND a.id=?");
            args.add(areaId);
        }
        if (floorId != null) {
            where.append(" AND f.id=?");
            args.add(floorId);
        }
        if (roomId != null) {
            where.append(" AND r.id=?");
            args.add(roomId);
        }
        if (states != null && !states.isEmpty()) {
            where.append(" AND b.state IN (")
                    .append(String.join(",", java.util.Collections.nCopies(states.size(), "?")))
                    .append(")");
            states.forEach(s -> args.add(s.name()));
        }
        return rows(where.toString(), args);
    }

    private java.util.List<com.cambae.backend.m03.internal.bed.domain.BedDetails> rows(
            String where, java.util.List<Object> args) {
        return jdbc.query(
                "SELECT b.*,a.id area_id,a.name area_name,f.id floor_id,f.name floor_name,r.name room_name FROM bed b JOIN room r ON r.id=b.room_id JOIN floor f ON f.id=r.floor_id JOIN area a ON a.id=f.area_id"
                        + where + " ORDER BY b.id",
                (rs, i) -> new com.cambae.backend.m03.internal.bed.domain.BedDetails(
                        rs.getLong("id"),
                        rs.getLong("room_id"),
                        rs.getString("code"),
                        com.cambae.backend.m03.api.dto.BedStatus.valueOf(rs.getString("state")),
                        rs.getInt("version"),
                        new com.cambae.backend.m03.api.dto.LocationView(
                                rs.getLong("area_id"),
                                rs.getString("area_name"),
                                rs.getLong("floor_id"),
                                rs.getString("floor_name"),
                                rs.getLong("room_id"),
                                rs.getString("room_name"),
                                rs.getLong("id"),
                                rs.getString("code")),
                        rs.getString("hold_reason"),
                        rs.getObject("hold_resident_id", Long.class),
                        instant(rs.getObject("hold_until", java.time.LocalDateTime.class)),
                        instant(rs.getObject("ready_deadline", java.time.LocalDateTime.class)),
                        false,
                        null,
                        java.util.List.of()),
                args.toArray());
    }

    private java.time.Instant instant(java.time.LocalDateTime value) {
        return value == null ? null : value.toInstant(java.time.ZoneOffset.UTC);
    }
}
