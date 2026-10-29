package com.eldercare.backend.roombedallocation.internal.allocation.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Bảng kỹ thuật nhắc; dấu thật nằm trên vị trí hoặc phân bổ. */
@Repository
public class AllocationReminderStore {
    private final JdbcTemplate jdbc;

    public AllocationReminderStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Key(String type, long id, String flag) {}

    public record Source(long resident, Long floor, boolean open) {}

    public record Marker(Instant started, Instant last, int count) {}

    public List<Key> candidates() {
        return jdbc.query(
                "SELECT target_type,target_id,flag_type FROM allocation_reminder UNION SELECT 'RESIDENT',resident_id,'WITHOUT_BED' FROM resident_position WHERE without_bed_since IS NOT NULL UNION SELECT 'BED_ALLOCATION',id,'AWAITING_REQUEST' FROM bed_allocation WHERE status='ACTIVE' AND awaiting_request=TRUE UNION SELECT 'BED_ALLOCATION',id,'AWAITING_SAME_PRICE' FROM bed_allocation WHERE status='ACTIVE' AND awaiting_same_price=TRUE UNION SELECT 'BED_ALLOCATION',id,'ROOM_UNSUITABLE' FROM bed_allocation WHERE status IN ('ACTIVE','FUTURE') AND room_unsuitable=TRUE",
                (r, n) -> new Key(r.getString(1), r.getLong(2), r.getString(3)));
    }

    public Long resident(Key key) {
        if (key.type().equals("RESIDENT")) return key.id();
        return jdbc
                .query("SELECT resident_id FROM bed_allocation WHERE id=?", (r, n) -> r.getLong(1), key.id())
                .stream()
                .findFirst()
                .orElse(null);
    }

    public Source source(Key key) {
        if (key.type().equals("RESIDENT"))
            return jdbc
                    .query(
                            "SELECT resident_id,without_bed_since IS NOT NULL FROM resident_position WHERE resident_id=? FOR UPDATE",
                            (r, n) ->
                                    new Source(r.getLong(1), null, key.flag().equals("WITHOUT_BED") && r.getBoolean(2)),
                            key.id())
                    .stream()
                    .findFirst()
                    .orElse(null);
        return jdbc
                .query(
                        "SELECT a.resident_id,r.floor_id,a.status,a.awaiting_request,a.awaiting_same_price,a.room_unsuitable FROM bed_allocation a JOIN bed b ON b.id=a.bed_id JOIN room r ON r.id=b.room_id WHERE a.id=? FOR UPDATE OF a",
                        (r, n) -> new Source(
                                r.getLong(1),
                                r.getLong(2),
                                switch (key.flag()) {
                                    case "AWAITING_REQUEST" -> r.getString(3).equals("ACTIVE") && r.getBoolean(4);
                                    case "AWAITING_SAME_PRICE" -> r.getString(3).equals("ACTIVE") && r.getBoolean(5);
                                    case "ROOM_UNSUITABLE" ->
                                        List.of("ACTIVE", "FUTURE").contains(r.getString(3)) && r.getBoolean(6);
                                    default -> false;
                                }),
                        key.id())
                .stream()
                .findFirst()
                .orElse(null);
    }

    public Marker marker(Key key, Instant at) {
        jdbc.update(
                "INSERT INTO allocation_reminder(target_type,target_id,flag_type,started_at,reminder_count) VALUES(?,?,?,?,0) ON DUPLICATE KEY UPDATE target_id=allocation_reminder.target_id",
                key.type(),
                key.id(),
                key.flag(),
                Timestamp.from(at));
        return jdbc.queryForObject(
                "SELECT started_at,last_reminded_at,reminder_count FROM allocation_reminder WHERE target_type=? AND target_id=? AND flag_type=? FOR UPDATE",
                (r, n) -> new Marker(
                        r.getTimestamp(1).toInstant(),
                        r.getTimestamp(2) == null ? null : r.getTimestamp(2).toInstant(),
                        r.getInt(3)),
                key.type(),
                key.id(),
                key.flag());
    }

    public void mark(Key key, Instant at) {
        jdbc.update(
                "UPDATE allocation_reminder SET last_reminded_at=?,reminder_count=reminder_count+1 WHERE target_type=? AND target_id=? AND flag_type=?",
                Timestamp.from(at),
                key.type(),
                key.id(),
                key.flag());
    }

    public void delete(Key key) {
        jdbc.update(
                "DELETE FROM allocation_reminder WHERE target_type=? AND target_id=? AND flag_type=?",
                key.type(),
                key.id(),
                key.flag());
    }
}
