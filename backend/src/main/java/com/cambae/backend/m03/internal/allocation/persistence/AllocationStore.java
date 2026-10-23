package com.cambae.backend.m03.internal.allocation.persistence;

import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.m03.api.dto.LocationView;
import com.cambae.backend.m03.internal.allocation.domain.AllocationLocation;
import com.cambae.backend.m03.internal.allocation.domain.AllocationStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** T026: đọc vị trí từ view đã áp đính chính, không đọc dữ liệu M01. */
@Repository
public class AllocationStore {
    private static final String LOCATION = """
        SELECT v.id,v.resident_id,v.status,v.bed_id,v.room_id,v.floor_id,v.area_id,
          a.name area_name,f.name floor_name,r.name room_name,b.code bed_code
        FROM v_current_allocation v
        JOIN bed b ON b.id=v.bed_id JOIN room r ON r.id=v.room_id
        JOIN floor f ON f.id=v.floor_id JOIN area a ON a.id=v.area_id
        """;
    private final JdbcTemplate jdbc;

    public AllocationStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<AllocationLocation> at(Instant at) {
        var timestamp = LocalDateTime.ofInstant(at, ZoneOffset.UTC);
        return jdbc.query(
                LOCATION
                        + " WHERE v.status IN ('ACTIVE','CLOSED') AND v.start_at<=? AND (v.end_at IS NULL OR v.end_at>?) ORDER BY v.id",
                AllocationStore::location,
                timestamp,
                timestamp);
    }

    public List<AllocationLocation> openOrFuture(long residentId) {
        return jdbc.query(
                LOCATION
                        + " WHERE v.resident_id=? AND v.status IN ('ACTIVE','FUTURE') ORDER BY CASE v.status WHEN 'ACTIVE' THEN 0 ELSE 1 END,v.id",
                AllocationStore::location,
                residentId);
    }

    public boolean hasNoBed(long residentId) {
        return jdbc.queryForObject(
                        "SELECT COUNT(*) FROM resident_position WHERE resident_id=? AND without_bed_since IS NOT NULL",
                        Integer.class,
                        residentId)
                > 0;
    }

    public void lockPosition(long residentId) {
        // Nhánh trùng khóa lấy khóa độc quyền ngay, tránh nâng khóa chia sẻ của INSERT IGNORE.
        jdbc.update(
                "INSERT INTO resident_position(resident_id) VALUES(?) ON DUPLICATE KEY UPDATE resident_id=resident_position.resident_id",
                residentId);
        jdbc.queryForList(
                "SELECT resident_id FROM resident_position WHERE resident_id=? FOR UPDATE", Long.class, residentId);
    }

    public void lockBed(long bedId) {
        if (jdbc.queryForList("SELECT id FROM bed WHERE id=? FOR UPDATE", Long.class, bedId)
                .isEmpty()) throw new NotFoundException();
    }

    private static AllocationLocation location(ResultSet row, int index) throws SQLException {
        return new AllocationLocation(
                row.getLong("id"),
                row.getLong("resident_id"),
                AllocationStatus.valueOf(row.getString("status")),
                new LocationView(
                        row.getLong("area_id"),
                        row.getString("area_name"),
                        row.getLong("floor_id"),
                        row.getString("floor_name"),
                        row.getLong("room_id"),
                        row.getString("room_name"),
                        row.getLong("bed_id"),
                        row.getString("bed_code")));
    }
}
