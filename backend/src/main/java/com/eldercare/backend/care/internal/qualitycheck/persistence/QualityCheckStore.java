package com.eldercare.backend.care.internal.qualitycheck.persistence;

import com.eldercare.backend.care.internal.qualitycheck.domain.CompletedTask;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 014 T024: đọc công việc của 005 (cùng module M04) cho kiểm tra chất lượng; không sửa công việc gốc (FR-061). */
@Repository
public class QualityCheckStore {
    private final JdbcTemplate jdbc;

    public QualityCheckStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Công việc Hoàn thành/Hoàn thành trễ của ca và tầng, hoàn thành trong ca. */
    public List<CompletedTask> completed(long shiftId, long floorAreaId, Instant from, Instant to) {
        return jdbc.query(
                "SELECT id,performer_id,result_recorded_by,result_performed_at,basis_type,shift_id FROM care_task"
                        + " WHERE shift_id=? AND floor_area_id=? AND status IN ('COMPLETED','COMPLETED_LATE')"
                        + " AND result_performed_at>=? AND result_performed_at<? ORDER BY id",
                (rs, i) -> new CompletedTask(
                        rs.getLong("id"),
                        rs.getObject("performer_id", Long.class),
                        rs.getLong("result_recorded_by"),
                        rs.getObject("result_performed_at", LocalDateTime.class)
                                .atZone(BusinessTime.ZONE)
                                .toInstant(),
                        rs.getString("basis_type"),
                        rs.getLong("shift_id")),
                shiftId,
                floorAreaId,
                LocalDateTime.ofInstant(from, BusinessTime.ZONE),
                LocalDateTime.ofInstant(to, BusinessTime.ZONE));
    }

    /** Cặp (ca, tầng) có công việc hoàn thành từ {@code since}: ca có thể cần nhắc hoặc ghi không có danh sách. */
    public List<long[]> recentShifts(Instant since) {
        return jdbc.query(
                "SELECT DISTINCT shift_id,floor_area_id FROM care_task WHERE status IN ('COMPLETED','COMPLETED_LATE')"
                        + " AND result_performed_at>=? ORDER BY shift_id,floor_area_id",
                (rs, i) -> new long[] {rs.getLong(1), rs.getLong(2)},
                LocalDateTime.ofInstant(since, BusinessTime.ZONE));
    }
}
