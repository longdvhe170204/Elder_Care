package com.eldercare.backend.roombedallocation.internal.vehicle.persistence;

import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 019 T004 phần lịch xe (W-09): lịch xe đã khóa (Đã hoàn thành, Đã hủy) cho bước loại bỏ dữ liệu. */
@Repository
public class VehicleScheduleStore {
    private static final String KIND = "M03.VEHICLE_SCHEDULE";
    private final JdbcTemplate jdbc;

    public VehicleScheduleStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean exists(long id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM simple_record WHERE kind = ? AND id = ?)", Boolean.class, KIND, id));
    }

    /**
     * SC-005: id lịch Đã đặt, Đang dùng của xe, đọc có khóa ({@code FOR UPDATE}) sau khóa dòng xe, để transaction thứ hai
     * thấy lịch vừa ghi của transaction trước (đọc thường ở REPEATABLE READ dùng ảnh dữ liệu cũ).
     */
    public List<Long> activeIdsForUpdate(long vehicleId) {
        return jdbc.queryForList(
                "SELECT id FROM simple_record WHERE kind = ? AND entity_type = 'M03.ASSET' AND entity_id = ?"
                        + " AND status IN ('BOOKED', 'IN_USE') ORDER BY id FOR UPDATE",
                Long.class,
                KIND,
                vehicleId);
    }

    public List<RetentionProvider.Candidate> lockedBefore(Instant cutoff, int batch) {
        return jdbc.query(
                "SELECT id, locked_at FROM simple_record WHERE kind = ? AND locked_at IS NOT NULL AND locked_at < ?"
                        + " ORDER BY locked_at, id LIMIT ?",
                (rs, i) -> new RetentionProvider.Candidate(
                        rs.getLong(1),
                        rs.getObject(2, LocalDateTime.class)
                                .atZone(BusinessTime.ZONE)
                                .toInstant()),
                KIND,
                LocalDateTime.ofInstant(cutoff, BusinessTime.ZONE),
                batch);
    }

    /** Xóa bằng tài khoản loại bỏ dữ liệu. */
    public void delete(JdbcTemplate retention, Collection<Long> ids) {
        if (ids.isEmpty()) return;
        var args = new java.util.ArrayList<Object>();
        args.add(KIND);
        args.addAll(ids);
        retention.update(
                "DELETE FROM simple_record WHERE kind = ? AND id IN ("
                        + String.join(",", Collections.nCopies(ids.size(), "?")) + ")",
                args.toArray());
    }
}
