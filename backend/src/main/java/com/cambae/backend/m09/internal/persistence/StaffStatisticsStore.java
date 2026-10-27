package com.cambae.backend.m09.internal.persistence;

import com.cambae.backend.m09.internal.domain.StaffStatisticsRows;
import com.cambae.backend.m09.shift.internal.persistence.ShiftStore;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StaffStatisticsStore {
    private final JdbcTemplate jdbc;

    public StaffStatisticsStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private Instant instant(ResultSet r, String column) throws SQLException {
        var value = r.getObject(column, LocalDateTime.class);
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    public List<StaffStatisticsRows.Handover> handovers(Instant from, Instant to) {
        return jdbc.query(
                "SELECT h.* FROM shift_handover h JOIN work_shift s ON s.id=h.shift_id WHERE s.starts_at<? AND s.ends_at>? AND s.status NOT IN ('DRAFT','CANCELLED') ORDER BY s.starts_at,h.id",
                this::handover,
                ShiftStore.sql(to),
                ShiftStore.sql(from));
    }

    /** 016 FR-020: Đã lập, Có ý kiến, hoặc Bản nháp của ca đang Chờ bàn giao. */
    public List<StaffStatisticsRows.Handover> openHandovers() {
        return jdbc.query(
                "SELECT h.* FROM shift_handover h JOIN work_shift s ON s.id=h.shift_id WHERE s.status NOT IN ('DRAFT','CANCELLED') AND (h.status IN ('PREPARED','HAS_COMMENTS') OR (h.status='DRAFT' AND s.status='AWAITING_HANDOVER')) ORDER BY s.ends_at,h.id",
                this::handover);
    }

    private StaffStatisticsRows.Handover handover(ResultSet r, int i) throws SQLException {
        return new StaffStatisticsRows.Handover(
                r.getLong("id"),
                r.getLong("shift_id"),
                r.getString("status"),
                instant(r, "finalized_at"),
                instant(r, "confirmed_at"));
    }

    public List<StaffStatisticsRows.Assignment> assignments(Instant from, Instant to) {
        return jdbc.query(
                "SELECT a.* FROM staff_assignment a JOIN work_shift s ON s.id=a.shift_id WHERE a.status NOT IN ('DRAFT','CANCELLED') AND s.status NOT IN ('DRAFT','CANCELLED') AND a.starts_at<? AND COALESCE(a.actual_end_at,a.ends_at)>? ORDER BY a.starts_at,a.id",
                (r, i) -> new StaffStatisticsRows.Assignment(
                        r.getLong("id"),
                        r.getLong("staff_id"),
                        r.getLong("shift_id"),
                        r.getString("target_type"),
                        r.getLong("target_id"),
                        instant(r, "starts_at"),
                        r.getObject("actual_end_at") == null ? instant(r, "ends_at") : instant(r, "actual_end_at"),
                        r.getString("role")),
                ShiftStore.sql(to),
                ShiftStore.sql(from));
    }

    public List<StaffStatisticsRows.Assignment> floorManagers(Instant from, Instant to) {
        return jdbc.query(
                "SELECT * FROM floor_manager_assignment WHERE starts_at<? AND (ends_at IS NULL OR ends_at>?) AND (ends_at IS NULL OR ends_at>starts_at) ORDER BY starts_at,id",
                (r, i) -> new StaffStatisticsRows.Assignment(
                        r.getLong("id"),
                        r.getLong("staff_id"),
                        null,
                        "FLOOR_OR_AREA",
                        r.getLong("floor_area_id"),
                        instant(r, "starts_at"),
                        instant(r, "ends_at"),
                        "FLOOR_MANAGER"),
                ShiftStore.sql(to),
                ShiftStore.sql(from));
    }

    public List<StaffStatisticsRows.Assignment> shiftDuties(Instant from, Instant to) {
        String members = "SELECT m.id,m.staff_id,m.shift_id,s.scope_type,s.scope_id,'SHIFT_MEMBER' AS duty,"
                + " GREATEST(s.starts_at,COALESCE(m.added_at,s.starts_at)) AS effective_from,"
                + " LEAST(s.ends_at,COALESCE(m.absent_at,s.ends_at),COALESCE(m.left_at,s.ends_at),COALESCE(m.withdrawn_on_resignation_at,s.ends_at)) AS effective_to"
                + " FROM shift_staff m JOIN work_shift s ON s.id=m.shift_id WHERE s.status NOT IN ('DRAFT','CANCELLED') AND m.approved_leave_request_id IS NULL";
        String leads = "SELECT h.id,h.staff_id,h.shift_id,s.scope_type,s.scope_id,'SHIFT_LEAD' AS duty,"
                + " GREATEST(s.starts_at,h.starts_at) AS effective_from,"
                + " LEAST(s.ends_at,COALESCE(h.ends_at,s.ends_at),COALESCE(m.absent_at,s.ends_at),COALESCE(m.left_at,s.ends_at),COALESCE(m.withdrawn_on_resignation_at,s.ends_at)) AS effective_to"
                + " FROM shift_lead_history h JOIN work_shift s ON s.id=h.shift_id LEFT JOIN shift_staff m ON m.shift_id=h.shift_id AND m.staff_id=h.staff_id"
                + " WHERE s.status NOT IN ('DRAFT','CANCELLED') AND m.approved_leave_request_id IS NULL";
        return jdbc.query(
                "SELECT * FROM (" + members + " UNION ALL " + leads
                        + ") duties WHERE effective_from<? AND effective_to>? AND effective_to>effective_from ORDER BY effective_from,duty,id",
                (r, i) -> new StaffStatisticsRows.Assignment(
                        r.getLong("id"),
                        r.getLong("staff_id"),
                        r.getLong("shift_id"),
                        r.getString("scope_type"),
                        r.getLong("scope_id"),
                        instant(r, "effective_from"),
                        instant(r, "effective_to"),
                        r.getString("duty")),
                ShiftStore.sql(to),
                ShiftStore.sql(from));
    }

    /** Chỉ bảng M09; nhật ký phải đọc qua AuditLogApi của A. */
    public Optional<Long> shiftForAudit(String type, long id) {
        String table = switch (type) {
            case "M09.HANDOVER" -> "shift_handover";
            case "M09.ASSIGNMENT" -> "staff_assignment";
            case "M09.SHIFT_STAFF", "M09.STAFF_ABSENCE" -> "shift_staff";
            case "M09.SHIFT_LEAD_HISTORY" -> "shift_lead_history";
            default -> null;
        };
        if (table == null) return Optional.empty();
        return jdbc.query("SELECT shift_id FROM " + table + " WHERE id=?", (r, i) -> r.getLong(1), id).stream()
                .findFirst();
    }
}
