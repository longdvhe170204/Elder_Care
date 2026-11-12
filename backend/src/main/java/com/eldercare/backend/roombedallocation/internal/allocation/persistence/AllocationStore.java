package com.eldercare.backend.roombedallocation.internal.allocation.persistence;

import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import com.eldercare.backend.roombedallocation.internal.allocation.domain.AllocationLocation;
import com.eldercare.backend.roombedallocation.internal.allocation.domain.AllocationStatus;
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
    private final ResidentPositionStore positions;

    public AllocationStore(JdbcTemplate jdbc, ResidentPositionStore positions) {
        this.jdbc = jdbc;
        this.positions = positions;
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

    /** Có phân bổ Hiệu lực hoặc Tương lai bắt đầu trước {@code before} (004 FR-021). */
    public boolean openOrFutureStartingBefore(long residentId, Instant before) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM v_current_allocation WHERE resident_id=? AND status IN ('ACTIVE','FUTURE') AND start_at<?",
                Integer.class,
                residentId,
                LocalDateTime.ofInstant(before, ZoneOffset.UTC));
        return count != null && count > 0;
    }

    public boolean hasNoBed(long residentId) {
        return positions.withoutBedSince(residentId).isPresent();
    }

    /** T066: khóa theo người là dòng {@code technical_lock} (upsert lấy khóa X ngay, như {@code resident_position} cũ). */
    public void lockPosition(long residentId) {
        positions.lock(residentId);
    }

    /**
     * RC chạy song song; transaction RR giữ độc quyền để trigger không tạo gap-lock cycle giữa các giường. T066: dòng
     * {@code technical_lock} M03.PHAN_BO do V003_95 tạo (thay {@code allocation_write_guard}).
     */
    public void guardWrites(boolean readCommitted) {
        jdbc.queryForList(
                "SELECT lock_key FROM technical_lock WHERE lock_key='M03.PHAN_BO' "
                        + (readCommitted ? "FOR SHARE" : "FOR UPDATE"),
                String.class);
    }

    public void lockBed(long bedId) {
        if (jdbc.queryForList("SELECT id FROM bed WHERE id=? FOR UPDATE", Long.class, bedId)
                .isEmpty()) throw new NotFoundException();
    }

    /** Giữ cấu hình phòng ổn định trước khi khóa giường; không nâng khóa phòng trong máy trạng thái. */
    public void guardRooms(List<Long> bedIds) {
        if (bedIds.isEmpty()) return;
        var placeholders = String.join(",", java.util.Collections.nCopies(bedIds.size(), "?"));
        var rooms = jdbc.queryForList(
                "SELECT DISTINCT room_id FROM bed WHERE id IN (" + placeholders + ") ORDER BY room_id",
                Long.class,
                bedIds.toArray());
        for (long roomId : rooms) jdbc.queryForList("SELECT id FROM room WHERE id=? FOR SHARE", Long.class, roomId);
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
