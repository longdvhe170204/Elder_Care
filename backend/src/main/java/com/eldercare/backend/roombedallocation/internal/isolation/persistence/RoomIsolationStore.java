package com.eldercare.backend.roombedallocation.internal.isolation.persistence;

import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.roombedallocation.internal.allocation.persistence.AllocationStore;
import com.eldercare.backend.roombedallocation.internal.isolation.domain.RoomIsolationFact;
import com.eldercare.backend.roombedallocation.internal.structure.domain.Room;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Khóa guard phân bổ trước phòng; không đảo thứ tự khóa của lệnh trạng thái M01. */
@Repository
public class RoomIsolationStore {
    private final JdbcTemplate jdbc;
    private final EntityManager entities;
    private final AllocationStore allocations;

    public RoomIsolationStore(JdbcTemplate jdbc, EntityManager entities, AllocationStore allocations) {
        this.jdbc = jdbc;
        this.entities = entities;
        this.allocations = allocations;
    }

    public void lock(long roomId) {
        allocations.guardWrites(false);
        if (jdbc.queryForList("SELECT id FROM room WHERE id=? FOR UPDATE", Long.class, roomId)
                .isEmpty()) throw new NotFoundException();
    }

    public Room room(long id) {
        var room = entities.find(Room.class, id);
        if (room == null) throw new NotFoundException();
        entities.refresh(room);
        room.careLevels();
        return room;
    }

    public List<Room> rooms() {
        return entities.createQuery("SELECT r FROM Room r ORDER BY r.id", Room.class).getResultList().stream()
                .peek(r -> r.careLevels())
                .toList();
    }

    public void change(long roomId, boolean isolated, Instant at, long actor, String reason, Long incident) {
        jdbc.update("UPDATE room SET isolated=?,version=version+1 WHERE id=?", isolated, roomId);
        jdbc.update(
                "INSERT INTO room_isolation_history(room_id,action,occurred_at,actor_id,reason,incident_id) VALUES(?,?,?,?,?,?)",
                roomId,
                isolated ? "ISOLATE" : "LIFT",
                Timestamp.from(at),
                actor,
                reason,
                incident);
    }

    public void clearDoctorFlags(long roomId) {
        jdbc.update(
                "UPDATE bed_allocation a JOIN bed b ON b.id=a.bed_id SET a.awaiting_doctor_confirmation=FALSE WHERE b.room_id=? AND a.status='ACTIVE' AND a.awaiting_doctor_confirmation=TRUE",
                roomId);
    }

    public List<RoomIsolationFact> history(long roomId) {
        return jdbc.query(
                "SELECT action,occurred_at,actor_id,reason,incident_id FROM room_isolation_history WHERE room_id=? ORDER BY occurred_at,id",
                (r, n) -> new RoomIsolationFact(
                        RoomIsolationFact.Action.valueOf(r.getString("action")),
                        r.getTimestamp("occurred_at").toInstant(),
                        r.getLong("actor_id"),
                        r.getString("reason"),
                        r.getObject("incident_id", Long.class)),
                roomId);
    }
}
