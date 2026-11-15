package com.eldercare.backend.roombedallocation.internal.allocation.persistence;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Dấu nhắc trên {@code reminder_marker} (T066, {@link AllocationReminderMarkers}); dấu thật nằm trên vị trí người cao
 * tuổi ({@link ResidentPositionStore}) hoặc phân bổ.
 */
@Repository
public class AllocationReminderStore {
    private final JdbcTemplate jdbc;
    private final AllocationReminderMarkers markers;
    private final ResidentPositionStore positions;

    public AllocationReminderStore(
            JdbcTemplate jdbc, AllocationReminderMarkers markers, ResidentPositionStore positions) {
        this.jdbc = jdbc;
        this.markers = markers;
        this.positions = positions;
    }

    public record Key(String type, long id, String flag) {}

    public record Source(long resident, Long floor, boolean open) {}

    public record Marker(Instant started, Instant last, int count) {}

    public List<Key> candidates() {
        var keys = new LinkedHashSet<Key>();
        markers.openKeys().forEach(k -> keys.add(new Key(k[0], Long.parseLong(k[1]), k[2])));
        positions.residentsWithoutBed().forEach(id -> keys.add(new Key("RESIDENT", id, "WITHOUT_BED")));
        keys.addAll(jdbc.query(
                "SELECT 'BED_ALLOCATION',id,'AWAITING_REQUEST' FROM bed_allocation WHERE status='ACTIVE' AND awaiting_request=TRUE UNION SELECT 'BED_ALLOCATION',id,'AWAITING_SAME_PRICE' FROM bed_allocation WHERE status='ACTIVE' AND awaiting_same_price=TRUE UNION SELECT 'BED_ALLOCATION',id,'ROOM_UNSUITABLE' FROM bed_allocation WHERE status IN ('ACTIVE','FUTURE') AND room_unsuitable=TRUE",
                (r, n) -> new Key(r.getString(1), r.getLong(2), r.getString(3))));
        return List.copyOf(keys);
    }

    public Long resident(Key key) {
        if (key.type().equals("RESIDENT")) return key.id();
        return jdbc
                .query("SELECT resident_id FROM bed_allocation WHERE id=?", (r, n) -> r.getLong(1), key.id())
                .stream()
                .findFirst()
                .orElse(null);
    }

    /** Lệnh gọi đã khóa người cao tuổi ({@code AllocationLocks}); dấu của người đọc sau khóa. */
    public Source source(Key key) {
        if (key.type().equals("RESIDENT"))
            return new Source(
                    key.id(),
                    null,
                    key.flag().equals("WITHOUT_BED")
                            && positions.locked(key.id()).withoutBedSince() != null);
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
        var cycle = markers.start(key.type(), key.id(), key.flag(), at);
        return new Marker(cycle.started(), cycle.last(), cycle.count());
    }

    public void mark(Key key, Instant at) {
        markers.remind(key.type(), key.id(), key.flag(), at);
    }

    public void delete(Key key) {
        markers.end(key.type(), key.id(), key.flag());
    }
}
