package com.eldercare.backend.roombedallocation.internal.allocation.persistence;

import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.roombedallocation.api.dto.BedStatus;
import com.eldercare.backend.roombedallocation.api.dto.GenderPolicy;
import com.eldercare.backend.roombedallocation.api.spi.CareLevelCode;
import com.eldercare.backend.roombedallocation.internal.allocation.domain.AllocationCandidate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** SQL chỉ thuộc M03; khoảng hiệu lực lấy từ view đã áp đính chính. */
@Repository
public class AllocationCheckStore {
    private final JdbcTemplate jdbc;
    private final CurrentAllocationOverlapStore currentOverlaps;

    public AllocationCheckStore(JdbcTemplate jdbc, CurrentAllocationOverlapStore currentOverlaps) {
        this.jdbc = jdbc;
        this.currentOverlaps = currentOverlaps;
    }

    public AllocationCandidate candidate(long bedId) {
        return candidate(bedId, false);
    }

    public AllocationCandidate currentCandidate(long bedId) {
        return candidate(bedId, true);
    }

    private AllocationCandidate candidate(long bedId, boolean current) {
        return jdbc
                .query(
                        """
            SELECT b.id,b.room_id,b.state,b.ready_deadline,b.hold_resident_id,r.floor_id,f.area_id,r.isolated,r.gender_policy,
            (r.status='ACTIVE' AND f.status='ACTIVE' AND a.status='ACTIVE') active,
            t.serves_long_term,t.serves_short_term
            FROM bed b JOIN room r ON r.id=b.room_id JOIN floor f ON f.id=r.floor_id
            JOIN area a ON a.id=f.area_id JOIN room_type t ON t.id=r.room_type_id WHERE b.id=?
            """ + (current ? " FOR SHARE" : ""),
                        (rs, i) -> new AllocationCandidate(
                                rs.getLong("id"),
                                rs.getLong("room_id"),
                                rs.getLong("floor_id"),
                                rs.getLong("area_id"),
                                BedStatus.valueOf(rs.getString("state")),
                                rs.getTimestamp("ready_deadline") == null
                                        ? null
                                        : rs.getTimestamp("ready_deadline").toInstant(),
                                rs.getObject("hold_resident_id", Long.class),
                                rs.getBoolean("active"),
                                rs.getBoolean("isolated"),
                                GenderPolicy.valueOf(rs.getString("gender_policy")),
                                Set.copyOf(jdbc
                                        .queryForList(
                                                "SELECT care_level FROM room_allowed_care_level WHERE room_id=?"
                                                        + (current ? " FOR SHARE" : ""),
                                                String.class,
                                                rs.getLong("room_id"))
                                        .stream()
                                        .map(CareLevelCode::valueOf)
                                        .toList()),
                                rs.getBoolean("serves_long_term"),
                                rs.getBoolean("serves_short_term")),
                        bedId)
                .stream()
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    public boolean overlaps(long bedId, Instant start, Instant end, Long excludedId) {
        return overlaps(bedId, start, end, excludedId, false);
    }

    public boolean overlaps(long bedId, Instant start, Instant end, Long excludedId, boolean current) {
        if (current) return currentOverlaps.overlaps(bedId, start, end, excludedId);
        return jdbc.queryForObject(
                        "SELECT COUNT(*) FROM v_current_allocation WHERE bed_id=? AND status<>'CANCELLED' AND (? IS NULL OR id<>?) AND start_at<? AND effective_end>?",
                        Long.class,
                        bedId,
                        excludedId,
                        excludedId,
                        end == null
                                ? LocalDateTime.of(9999, 12, 31, 0, 0)
                                : LocalDateTime.ofInstant(end, ZoneOffset.UTC),
                        LocalDateTime.ofInstant(start, ZoneOffset.UTC))
                > 0;
    }
}
