package com.cambae.backend.m03.internal.allocation.persistence;

import com.cambae.backend.m03.api.dto.LocationView;
import com.cambae.backend.m03.api.spi.CareLevelCode;
import com.cambae.backend.m03.internal.allocation.domain.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** T027: chỉ tra view hiện hành; phạm vi lọc ở service trước khi trả kết quả. */
@Repository
public class AllocationReadStore {
    private final JdbcTemplate jdbc;

    public AllocationReadStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<AllocationDetails> list(Long residentId, Long bedId, AllocationStatus status) {
        String sql =
                "SELECT v.*,a.name area_name,f.name floor_name,r.name room_name,b.code bed_code FROM v_current_allocation v JOIN bed b ON b.id=v.bed_id JOIN room r ON r.id=v.room_id JOIN floor f ON f.id=v.floor_id JOIN area a ON a.id=v.area_id WHERE 1=1";
        List<Object> args = new ArrayList<>();
        if (residentId != null) {
            sql += " AND v.resident_id=?";
            args.add(residentId);
        }
        if (bedId != null) {
            sql += " AND v.bed_id=?";
            args.add(bedId);
        }
        if (status != null) {
            sql += " AND v.status=?";
            args.add(status.name());
        }
        return jdbc.query(sql + " ORDER BY v.start_at,v.id", AllocationReadStore::row, args.toArray());
    }

    public Instant withoutBedSince(long residentId) {
        var rows = jdbc.query(
                "SELECT without_bed_since FROM resident_position WHERE resident_id=?",
                (rs, i) -> instant(rs, "without_bed_since"),
                residentId);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private static AllocationDetails row(ResultSet rs, int index) throws SQLException {
        List<String> flags = new ArrayList<>();
        if (rs.getBoolean("room_unsuitable")) flags.add("ROOM_UNSUITABLE");
        if (rs.getBoolean("awaiting_doctor_confirmation")) flags.add("AWAITING_DOCTOR_CONFIRMATION");
        if (rs.getBoolean("awaiting_request")) flags.add("AWAITING_REQUEST");
        if (rs.getBoolean("awaiting_same_price")) flags.add("AWAITING_SAME_PRICE");
        return new AllocationDetails(
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
                AllocationStatus.valueOf(rs.getString("status")),
                instant(rs, "start_at"),
                instant(rs, "planned_start_at"),
                instant(rs, "end_at"),
                instant(rs, "expected_end"),
                rs.getString("start_reason"),
                rs.getString("end_reason"),
                rs.getString("cancellation_reason"),
                rs.getObject("opened_by_id", Long.class),
                rs.getObject("closed_by_id", Long.class),
                rs.getString("basis_type"),
                rs.getObject("basis_id", Long.class),
                rs.getObject("previous_allocation_id", Long.class),
                CareLevelCode.valueOf(rs.getString("checked_care_level")),
                CareLevelSource.valueOf(rs.getString("care_level_source")),
                rs.getObject("prescribing_doctor_id", Long.class),
                rs.getBoolean("backfilled"),
                instant(rs, "actual_recorded_at"),
                instant(rs, "planned_at"),
                rs.getBoolean("executed_late"),
                rs.getBoolean("waiting_for_bed_ready"),
                List.copyOf(flags),
                rs.getObject("confirming_doctor_id", Long.class),
                instant(rs, "doctor_confirmed_at"),
                rs.getString("doctor_confirmation_reason"),
                rs.getBoolean("corrected"));
    }

    private static Instant instant(ResultSet rs, String field) throws SQLException {
        var value = rs.getTimestamp(field);
        return value == null ? null : value.toInstant();
    }
}
