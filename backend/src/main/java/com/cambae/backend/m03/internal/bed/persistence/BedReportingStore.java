package com.cambae.backend.m03.internal.bed.persistence;

import com.cambae.backend.m03.api.dto.*;
import com.cambae.backend.m03.api.spi.CareLevelCode;
import com.cambae.backend.m03.internal.bed.domain.BedReportingSnapshot.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** T061: đọc theo lô, dùng fact gốc cho state và view đã áp đính chính cho phân bổ. */
@Repository
public class BedReportingStore {
    private final JdbcTemplate jdbc;

    public BedReportingStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String STATE =
            "SELECT h.bed_id,h.floor_area_id,b.room_id,b.code,h.to_state,h.event,h.occurred_at ";

    private static State state(ResultSet r) throws SQLException {
        return new State(
                r.getLong("bed_id"),
                r.getLong("floor_area_id"),
                r.getLong("room_id"),
                r.getString("code"),
                BedStatus.valueOf(r.getString("to_state")),
                r.getString("event"),
                r.getTimestamp("occurred_at").toInstant());
    }

    public List<State> states(Instant at) {
        return jdbc.query(
                STATE
                        + "FROM (SELECT s.*,ROW_NUMBER() OVER(PARTITION BY bed_id ORDER BY occurred_at DESC,id DESC) rn FROM bed_state_history s WHERE occurred_at<=?) h JOIN bed b ON b.id=h.bed_id WHERE h.rn=1 ORDER BY h.bed_id",
                (r, n) -> state(r),
                Timestamp.from(at));
    }

    public List<State> history(long bed, Instant at) {
        return jdbc.query(
                STATE
                        + "FROM bed_state_history h JOIN bed b ON b.id=h.bed_id WHERE h.bed_id=? AND h.occurred_at<=? ORDER BY h.occurred_at,h.id",
                (r, n) -> state(r),
                bed,
                Timestamp.from(at));
    }

    public Optional<BedStatus> current(long bed) {
        return jdbc.query("SELECT state FROM bed WHERE id=?", (r, n) -> BedStatus.valueOf(r.getString(1)), bed).stream()
                .findFirst();
    }

    public List<Window> windows(Instant from, Instant to) {
        return jdbc.query(
                STATE
                        + ",h.until_at,CASE WHEN h.event='REUSE' THEN NULL ELSE (SELECT v.resident_id FROM v_current_allocation v WHERE v.bed_id=h.bed_id AND v.status='CLOSED' AND v.end_at<=h.occurred_at ORDER BY v.end_at DESC,v.id DESC LIMIT 1) END departed_resident_id FROM (SELECT s.*,LEAD(occurred_at) OVER(PARTITION BY bed_id ORDER BY occurred_at,id) until_at FROM bed_state_history s WHERE occurred_at<?) h JOIN bed b ON b.id=h.bed_id WHERE h.to_state='AWAITING_READY' AND (h.until_at IS NULL OR h.until_at>?) ORDER BY h.bed_id,h.occurred_at,h.id",
                (r, n) -> new Window(
                        state(r),
                        r.getTimestamp("until_at") == null
                                ? null
                                : r.getTimestamp("until_at").toInstant(),
                        r.getObject("departed_resident_id", Long.class)),
                Timestamp.from(to),
                Timestamp.from(from));
    }

    public List<RoomIsolationView> isolation(Instant at) {
        return jdbc.query(
                "SELECT h.room_id,r.floor_id,h.occurred_at,h.until_at FROM (SELECT s.*,LEAD(occurred_at) OVER(PARTITION BY room_id ORDER BY occurred_at,id) until_at FROM room_isolation_history s) h JOIN room r ON r.id=h.room_id WHERE h.action='ISOLATE' AND h.occurred_at<=? AND (h.until_at IS NULL OR h.until_at>?) ORDER BY h.room_id",
                (r, n) -> new RoomIsolationView(
                        r.getLong(1),
                        r.getLong(2),
                        r.getTimestamp(3).toInstant(),
                        r.getTimestamp(4) == null ? null : r.getTimestamp(4).toInstant()),
                Timestamp.from(at),
                Timestamp.from(at));
    }

    public List<Policy> policies() {
        return jdbc.query(
                "SELECT b.id,r.id room_id,r.floor_id,f.area_id,(r.status='ACTIVE' AND f.status='ACTIVE' AND a.status='ACTIVE' AND t.status='ACTIVE') active,r.gender_policy,t.serves_long_term,t.serves_short_term,r.isolated,(SELECT GROUP_CONCAT(c.care_level) FROM room_allowed_care_level c WHERE c.room_id=r.id) levels,EXISTS(SELECT 1 FROM v_current_allocation v WHERE v.bed_id=b.id AND v.status='FUTURE') reserved FROM bed b JOIN room r ON r.id=b.room_id JOIN floor f ON f.id=r.floor_id JOIN area a ON a.id=f.area_id JOIN room_type t ON t.id=r.room_type_id ORDER BY b.id",
                (r, n) -> new Policy(
                        r.getLong("id"),
                        r.getLong("room_id"),
                        r.getLong("floor_id"),
                        r.getLong("area_id"),
                        r.getBoolean("active"),
                        GenderPolicy.valueOf(r.getString("gender_policy")),
                        r.getString("levels") == null
                                ? Set.of()
                                : Set.copyOf(Arrays.stream(r.getString("levels").split(","))
                                        .map(CareLevelCode::valueOf)
                                        .toList()),
                        r.getBoolean("serves_long_term"),
                        r.getBoolean("serves_short_term"),
                        r.getBoolean("isolated"),
                        r.getBoolean("reserved")));
    }

    public Map<Long, Set<Long>> residentsAt(Instant at) {
        Map<Long, Set<Long>> result = new HashMap<>();
        jdbc.query(
                "SELECT bed_id,resident_id FROM v_current_allocation WHERE status IN ('ACTIVE','CLOSED') AND start_at<=? AND effective_end>?",
                (org.springframework.jdbc.core.RowCallbackHandler)
                        r -> result.computeIfAbsent(r.getLong(1), ignored -> new HashSet<>())
                                .add(r.getLong(2)),
                Timestamp.from(at),
                Timestamp.from(at));
        return result;
    }

    public List<Flags> flags() {
        return jdbc.query(
                "SELECT v.id,v.resident_id,v.bed_id,v.room_id,v.floor_id,v.start_at,v.effective_end,v.status,v.room_unsuitable,v.awaiting_request,v.awaiting_same_price,v.awaiting_doctor_confirmation FROM v_current_allocation v WHERE v.status IN ('ACTIVE','FUTURE') ORDER BY v.id",
                (r, n) -> {
                    Set<String> flags = new HashSet<>();
                    for (String flag : List.of(
                            "ROOM_UNSUITABLE",
                            "AWAITING_REQUEST",
                            "AWAITING_SAME_PRICE",
                            "AWAITING_DOCTOR_CONFIRMATION"))
                        if (r.getBoolean(flag.toLowerCase(Locale.ROOT))) flags.add(flag);
                    return new Flags(
                            r.getLong("id"),
                            r.getLong("resident_id"),
                            r.getLong("bed_id"),
                            r.getLong("room_id"),
                            r.getLong("floor_id"),
                            r.getTimestamp("start_at").toInstant(),
                            r.getTimestamp("effective_end").toInstant(),
                            r.getString("status").equals("ACTIVE"),
                            Set.copyOf(flags));
                });
    }

    public List<Long> withoutBed() {
        return jdbc.queryForList(
                "SELECT resident_id FROM resident_position WHERE without_bed_since IS NOT NULL ORDER BY resident_id",
                Long.class);
    }
}
