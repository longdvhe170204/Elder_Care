package com.eldercare.backend.roombedallocation.internal.isolation.persistence;

import com.eldercare.backend.administration.shared.api.StatusHistoryApi;
import com.eldercare.backend.administration.shared.api.dto.HistoryEntry;
import com.eldercare.backend.administration.shared.api.dto.HistoryKind;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.roombedallocation.internal.allocation.persistence.AllocationStore;
import com.eldercare.backend.roombedallocation.internal.isolation.domain.RoomIsolationFact;
import com.eldercare.backend.roombedallocation.internal.structure.domain.Room;
import jakarta.persistence.EntityManager;
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
    private final StatusHistoryApi histories;

    public RoomIsolationStore(
            JdbcTemplate jdbc, EntityManager entities, AllocationStore allocations, StatusHistoryApi histories) {
        this.jdbc = jdbc;
        this.entities = entities;
        this.allocations = allocations;
        this.histories = histories;
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

    /** Phòng trên các tầng đã cho (T053: phòng của vùng khoanh vừa gỡ). */
    public List<Room> roomsOnFloors(java.util.Collection<Long> floorIds) {
        if (floorIds.isEmpty()) return List.of();
        return entities.createQuery("SELECT r FROM Room r WHERE r.floorId IN :floors ORDER BY r.id", Room.class)
                .setParameter("floors", floorIds)
                .getResultList();
    }

    public List<Room> rooms() {
        return entities.createQuery("SELECT r FROM Room r ORDER BY r.id", Room.class).getResultList().stream()
                .peek(r -> r.careLevels())
                .toList();
    }

    public void change(long roomId, boolean isolated, Instant at, long actor, String reason, Long incident) {
        jdbc.update("UPDATE room SET isolated=?,version=version+1 WHERE id=?", isolated, roomId);
        // Lịch sử cách ly ở status_history (R-15, gộp 2026-11-13): điểm M03.ROOM / ISOLATION, căn cứ là sự cố của 007.
        histories.add(
                RoomIsolationFact.TRACK,
                roomId,
                new HistoryEntry<>(
                        HistoryKind.POINT,
                        null,
                        isolated ? "ISOLATE" : "LIFT",
                        null,
                        at,
                        null,
                        actor,
                        false,
                        new Reason(reason),
                        incident == null ? null : RoomIsolationFact.INCIDENT_BASIS,
                        incident,
                        null,
                        null,
                        null));
    }

    /** Giường còn Chờ sẵn sàng mà phân bổ vừa đóng là của {@code residentId} (FR-045). */
    public List<com.eldercare.backend.roombedallocation.internal.isolation.domain.LeftAwaitingReadyBed>
            awaitingReadyBedsLeftBy(long residentId) {
        return jdbc.query(
                "SELECT b.id,r.floor_id,b.awaiting_ready_since FROM bed b JOIN room r ON r.id=b.room_id"
                        + " JOIN bed_allocation a ON a.id=b.last_closed_allocation_id"
                        + " WHERE b.state='AWAITING_READY' AND a.resident_id=? ORDER BY b.id",
                (r, n) -> new com.eldercare.backend.roombedallocation.internal.isolation.domain.LeftAwaitingReadyBed(
                        r.getLong(1), r.getLong(2), r.getTimestamp(3).toInstant()),
                residentId);
    }

    public void clearDoctorFlags(long roomId) {
        jdbc.update(
                "UPDATE bed_allocation a JOIN bed b ON b.id=a.bed_id SET a.awaiting_doctor_confirmation=FALSE WHERE b.room_id=? AND a.status='ACTIVE' AND a.awaiting_doctor_confirmation=TRUE",
                roomId);
    }

    public List<RoomIsolationFact> history(long roomId) {
        return jdbc.query(
                "SELECT value action,starts_at occurred_at,actor_id,reason,basis_id incident_id FROM status_history"
                        + " WHERE entity_type='M03.ROOM' AND attribute='ISOLATION' AND entity_id=? ORDER BY starts_at,id",
                (r, n) -> new RoomIsolationFact(
                        RoomIsolationFact.Action.valueOf(r.getString("action")),
                        r.getTimestamp("occurred_at").toInstant(),
                        r.getLong("actor_id"),
                        r.getString("reason"),
                        r.getObject("incident_id", Long.class)),
                roomId);
    }
}
