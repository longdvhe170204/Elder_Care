package com.cambae.backend.m03.internal.history.persistence;

import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.m03.api.dto.*;
import com.cambae.backend.m03.internal.history.domain.HistoryStay;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** G-10: tất cả lịch sử đều đọc view hiện hành, khoảng nửa mở. */
@Repository
public class LocationHistoryStore {
    private static final String SELECT = """
        SELECT v.id,v.resident_id,v.bed_id,v.room_id,v.floor_id,v.area_id,
          a.name area_name,f.name floor_name,r.name room_name,b.code bed_code,
          v.start_at,COALESCE(v.end_at,v.expected_end) end_at,v.corrected
        FROM v_current_allocation v JOIN bed b ON b.id=v.bed_id JOIN room r ON r.id=v.room_id
          JOIN floor f ON f.id=v.floor_id JOIN area a ON a.id=v.area_id
        """;
    private final JdbcTemplate jdbc;

    public LocationHistoryStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<HistoryStay> resident(long id, Instant from, Instant to) {
        return jdbc.query(
                SELECT
                        + " WHERE v.resident_id=? AND v.status IN ('ACTIVE','CLOSED') AND v.start_at<? AND v.effective_end>? ORDER BY v.start_at,v.id",
                LocationHistoryStore::row,
                id,
                Timestamp.from(to),
                Timestamp.from(from));
    }

    public List<HistoryStay> residentAt(long id, Instant at) {
        return jdbc.query(
                SELECT
                        + " WHERE v.resident_id=? AND v.status IN ('ACTIVE','CLOSED') AND v.start_at<=? AND v.effective_end>? ORDER BY v.start_at,v.id",
                LocationHistoryStore::row,
                id,
                Timestamp.from(at),
                Timestamp.from(at));
    }

    public List<LocationView> beds(LocationRef ref) {
        String column = switch (ref.type()) {
            case "AREA" -> "f.area_id";
            case "FLOOR" -> "r.floor_id";
            case "ROOM" -> "b.room_id";
            case "BED" -> "b.id";
            default -> throw new InvalidValueException("Loại vị trí không hợp lệ");
        };
        return jdbc.query(
                "SELECT b.id bed_id,b.code bed_code,r.id room_id,r.name room_name,f.id floor_id,f.name floor_name,a.id area_id,a.name area_name FROM bed b JOIN room r ON r.id=b.room_id JOIN floor f ON f.id=r.floor_id JOIN area a ON a.id=f.area_id WHERE "
                        + column + "=? ORDER BY b.id",
                (rs, i) -> new LocationView(
                        rs.getLong("area_id"),
                        rs.getString("area_name"),
                        rs.getLong("floor_id"),
                        rs.getString("floor_name"),
                        rs.getLong("room_id"),
                        rs.getString("room_name"),
                        rs.getLong("bed_id"),
                        rs.getString("bed_code")),
                ref.id());
    }

    public List<HistoryStay> location(LocationRef ref, Instant from, Instant to, boolean point) {
        String column = switch (ref.type()) {
            case "AREA" -> "area_id";
            case "FLOOR" -> "floor_id";
            case "ROOM" -> "room_id";
            case "BED" -> "bed_id";
            default -> throw new InvalidValueException("Loại vị trí không hợp lệ");
        };
        return jdbc.query(
                SELECT + " WHERE v." + column + "=? AND v.status IN ('ACTIVE','CLOSED') AND v.start_at"
                        + (point ? "<=?" : "<?") + " AND v.effective_end>? ORDER BY v.bed_id,v.start_at,v.id",
                LocationHistoryStore::row,
                ref.id(),
                Timestamp.from(to),
                Timestamp.from(from));
    }

    public List<HistoryStay> roommates(long resident, Instant from, Instant to) {
        String sql = SELECT + """
            JOIN v_current_allocation s ON s.room_id=v.room_id AND s.resident_id=?
            WHERE s.status IN ('ACTIVE','CLOSED') AND v.status IN ('ACTIVE','CLOSED')
              AND v.resident_id<>s.resident_id
              AND GREATEST(s.start_at,v.start_at,?)<LEAST(s.effective_end,v.effective_end,?)
            ORDER BY v.resident_id,v.start_at,v.id
            """;
        // Giao với từng khoảng của A; giữ riêng các khoảng rời nhau khi A quay lại phòng.
        return jdbc.query(
                sql.replace(
                        "v.start_at,COALESCE(v.end_at,v.expected_end) end_at,v.corrected",
                        "GREATEST(s.start_at,v.start_at,?) start_at,LEAST(s.effective_end,v.effective_end,?) end_at,(v.corrected OR s.corrected) corrected"),
                LocationHistoryStore::row,
                Timestamp.from(from),
                Timestamp.from(to),
                resident,
                Timestamp.from(from),
                Timestamp.from(to));
    }

    public List<Long> backfillRequests(long resident, Instant from, Instant to) {
        return jdbc.queryForList(
                "SELECT q.approval_request_id FROM bed_backfill_request q JOIN v_current_allocation v ON v.id=q.current_allocation_id WHERE q.resident_id=? AND v.effective_end>? AND q.transfer_at<?",
                Long.class,
                resident,
                Timestamp.from(from),
                Timestamp.from(to));
    }

    private static HistoryStay row(ResultSet rs, int i) throws SQLException {
        var end = rs.getTimestamp("end_at");
        return new HistoryStay(
                rs.getLong("id"),
                rs.getLong("resident_id"),
                new LocationView(
                        rs.getLong("area_id"),
                        rs.getString("area_name"),
                        rs.getLong("floor_id"),
                        rs.getString("floor_name"),
                        rs.getLong("room_id"),
                        rs.getString("room_name"),
                        rs.getLong("bed_id"),
                        rs.getString("bed_code")),
                rs.getTimestamp("start_at").toInstant(),
                end == null ? null : end.toInstant(),
                rs.getBoolean("corrected"));
    }
}
