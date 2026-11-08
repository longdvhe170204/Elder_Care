package com.eldercare.backend.billing.internal.procurement.persistence;

import com.eldercare.backend.billing.api.dto.ProcurementRequestView;
import com.eldercare.backend.billing.api.dto.ProcurementStatus;
import com.eldercare.backend.billing.api.dto.ProcurementTotalView;
import com.eldercare.backend.billing.internal.procurement.domain.ProcurementContent;
import com.eldercare.backend.billing.internal.procurement.domain.ProcurementRequestRow;
import com.eldercare.backend.billing.internal.procurement.domain.ProcurementRequestRow.Line;
import com.eldercare.backend.billing.internal.procurement.domain.ProcurementRequestRow.LinkedSource;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.dto.ProcurementSourceType;
import com.eldercare.backend.roombedallocation.api.dto.ProcurementType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * 019 T006, T007: đọc, ghi {@code procurement_request}. Thời điểm theo {@link BusinessTime#ZONE} như bảng nghiệp vụ khác
 * (không phải UTC của reminder_marker). Cột đổi ở lệnh chuyển trạng thái chỉ nằm trong {@link #TRANSITION_COLUMNS}.
 */
@Repository
public class ProcurementRequestStore {
    private static final String COLUMNS = "id, department_id, created_by_staff_id, type, priority, needed_by,"
            + " related_file_ids, `lines`, estimated_total, reason, needed_on, reference, original_request_id,"
            + " approved_amount, reduction_reason, decided_by_staff_id, approved_at, actual_amount, purchased_on,"
            + " supplier, receipt_file_ids, purchase_recorded_by_staff_id, purchase_recorded_at, reconciliation_result,"
            + " discrepancy_amount, discrepancy_resolution, reconciled_by_staff_id, linked_sources, status,"
            + " status_reason, created_at, locked_at, version";
    private static final Set<String> TRANSITION_COLUMNS = Set.of(
            "approved_amount",
            "reduction_reason",
            "decided_by_staff_id",
            "approved_at",
            "actual_amount",
            "purchased_on",
            "supplier",
            "receipt_file_ids",
            "purchase_recorded_by_staff_id",
            "purchase_recorded_at",
            "reconciliation_result",
            "discrepancy_amount",
            "discrepancy_resolution",
            "reconciled_by_staff_id");
    private static final TypeReference<List<Line>> LINES = new TypeReference<>() {};
    private static final TypeReference<List<LinkedSource>> SOURCES = new TypeReference<>() {};
    private static final TypeReference<List<Long>> IDS = new TypeReference<>() {};
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};

    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public ProcurementRequestStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public long insert(ProcurementContent content, long createdByStaffId, Instant at) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO procurement_request (department_id, created_by_staff_id, type, priority,"
                                    + " needed_by, related_file_ids, `lines`, estimated_total, reason, needed_on,"
                                    + " reference, original_request_id, linked_sources, status, created_at)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '[]', 'DRAFT', ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    int i = bindContent(ps, content, 1, createdByStaffId);
                    ps.setObject(i, local(at));
                    return ps;
                },
                keys);
        return keys.getKey().longValue();
    }

    public void updateContent(long id, ProcurementContent content) {
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement("UPDATE procurement_request SET department_id = ?, type = ?"
                            + ", priority = ?, needed_by = ?,"
                            + " related_file_ids = ?, `lines` = ?, estimated_total = ?, reason = ?, needed_on = ?, reference = ?,"
                            + " original_request_id = ?, version = version + 1 WHERE id = ?");
            ps.setLong(1, content.departmentId());
            int i = bindContentTail(ps, content, 2);
            ps.setLong(i, id);
            return ps;
        });
    }

    /** Đổi trạng thái kèm các cột ở {@link #TRANSITION_COLUMNS}; {@code lockedAt} có ở trạng thái cuối. */
    public void transition(
            long id, ProcurementStatus to, String statusReason, Instant lockedAt, Map<String, Object> columns) {
        var sql = new StringBuilder("UPDATE procurement_request SET status = ?, status_reason = ?, locked_at = ?");
        var args = new ArrayList<Object>(List.of(to.name()));
        args.add(statusReason);
        args.add(lockedAt == null ? null : local(lockedAt));
        columns.forEach((column, value) -> {
            if (!TRANSITION_COLUMNS.contains(column)) throw new IllegalArgumentException(column);
            sql.append(", ").append(column).append(" = ?");
            args.add(value instanceof Instant at ? local(at) : value instanceof List<?> list ? write(list) : value);
        });
        sql.append(", version = version + 1 WHERE id = ?");
        args.add(id);
        jdbc.update(sql.toString(), args.toArray());
    }

    public void linkSources(long id, List<LinkedSource> sources) {
        jdbc.update(
                "UPDATE procurement_request SET linked_sources = ?, version = version + 1 WHERE id = ?",
                write(sources),
                id);
    }

    /** DBR-36: nguồn đã gắn vào một yêu cầu nào đó. Gọi dưới khóa kỹ thuật của nguồn. */
    public boolean sourceLinked(ProcurementSourceType type, long sourceId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM procurement_request WHERE JSON_CONTAINS(linked_sources,"
                        + " JSON_OBJECT('sourceType', ?, 'sourceId', ?)))",
                Boolean.class,
                type.name(),
                sourceId));
    }

    public Optional<ProcurementRequestRow> find(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM procurement_request WHERE id = ?", this::row, id).stream()
                .findFirst();
    }

    public Optional<ProcurementRequestRow> lock(long id) {
        return jdbc
                .query("SELECT " + COLUMNS + " FROM procurement_request WHERE id = ? FOR UPDATE", this::row, id)
                .stream()
                .findFirst();
    }

    /**
     * BR-M11-20: yêu cầu đang ở {@code status} có mốc {@code since} (approved_at hoặc purchase_recorded_at) trước
     * {@code cutoff}.
     */
    public List<ProcurementRequestRow> waitingSince(ProcurementStatus status, String since, Instant cutoff) {
        if (!Set.of("approved_at", "purchase_recorded_at").contains(since)) throw new IllegalArgumentException(since);
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM procurement_request WHERE status = ? AND " + since + " < ? ORDER BY id",
                this::row,
                status.name(),
                local(cutoff));
    }

    /** W-09: yêu cầu ở trạng thái cuối, khóa trước {@code cutoff} (mốc tính hạn lưu giữ). */
    public List<com.eldercare.backend.administration.retention.api.spi.RetentionProvider.Candidate> lockedBefore(
            Instant cutoff, int batch) {
        return jdbc.query(
                "SELECT id, locked_at FROM procurement_request WHERE locked_at IS NOT NULL AND locked_at < ?"
                        + " ORDER BY locked_at, id LIMIT ?",
                (rs, i) -> new com.eldercare.backend.administration.retention.api.spi.RetentionProvider.Candidate(
                        rs.getLong(1), instant(rs, "locked_at")),
                local(cutoff),
                batch);
    }

    /** Xóa bằng tài khoản loại bỏ dữ liệu (trigger chặn tài khoản khác). */
    public void delete(JdbcTemplate retention, Collection<Long> ids) {
        if (ids.isEmpty()) return;
        retention.update(
                "DELETE FROM procurement_request WHERE id IN (" + String.join(",", Collections.nCopies(ids.size(), "?"))
                        + ")",
                ids.toArray());
    }

    public boolean exists(long id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM procurement_request WHERE id = ?)", Boolean.class, id));
    }

    /**
     * Danh sách theo bộ lọc; {@code departmentIds}, {@code createdBy} rỗng là không lọc theo điều đó, có cả hai thì lấy
     * hợp (bộ phận mình hoặc do mình lập).
     */
    public List<ProcurementRequestRow> list(
            Set<Long> departmentIds, Long createdBy, Long departmentId, ProcurementStatus status) {
        var sql = new StringBuilder("SELECT " + COLUMNS + " FROM procurement_request WHERE 1 = 1");
        var args = new ArrayList<Object>();
        if (departmentIds != null || createdBy != null) {
            var any = new ArrayList<String>();
            if (departmentIds != null && !departmentIds.isEmpty()) {
                any.add("department_id IN (" + String.join(",", Collections.nCopies(departmentIds.size(), "?")) + ")");
                args.addAll(departmentIds);
            }
            if (createdBy != null) {
                any.add("created_by_staff_id = ?");
                args.add(createdBy);
            }
            sql.append(any.isEmpty() ? " AND 1 = 0" : " AND (" + String.join(" OR ", any) + ")");
        }
        if (departmentId != null) {
            sql.append(" AND department_id = ?");
            args.add(departmentId);
        }
        if (status != null) {
            sql.append(" AND status = ?");
            args.add(status.name());
        }
        sql.append(" ORDER BY created_at DESC, id DESC");
        return jdbc.query(sql.toString(), this::row, args.toArray());
    }

    /**
     * T023 (016 CP.MUA_SAM): yêu cầu có ngày lập trong [from, to] theo {@link BusinessTime#ZONE}; {@code departmentIds}
     * rỗng là toàn viện.
     */
    public List<ProcurementRequestView> statistics(Set<Long> departmentIds, LocalDate from, LocalDate to) {
        return jdbc.query(
                "SELECT id, department_id, type, status, estimated_total, approved_amount, actual_amount, created_at,"
                        + " purchased_on FROM procurement_request" + statisticsWhere(departmentIds)
                        + " ORDER BY created_at, id",
                (rs, n) -> new ProcurementRequestView(
                        rs.getLong("id"),
                        rs.getLong("department_id"),
                        ProcurementType.valueOf(rs.getString("type")),
                        ProcurementStatus.valueOf(rs.getString("status")),
                        rs.getLong("estimated_total"),
                        rs.getObject("approved_amount", Long.class),
                        rs.getObject("actual_amount", Long.class),
                        rs.getObject("created_at", LocalDateTime.class).toLocalDate(),
                        rs.getObject("purchased_on", LocalDate.class)),
                statisticsArgs(departmentIds, from, to));
    }

    /** T023: đếm, cộng tiền theo (bộ phận, trạng thái) bằng GROUP BY tại DB; tiền chưa có tính 0. */
    public List<ProcurementTotalView> totals(Set<Long> departmentIds, LocalDate from, LocalDate to) {
        return jdbc.query(
                "SELECT department_id, status, COUNT(*) AS request_count,"
                        + " COALESCE(SUM(approved_amount), 0) AS approved_amount,"
                        + " COALESCE(SUM(actual_amount), 0) AS actual_amount FROM procurement_request"
                        + statisticsWhere(departmentIds) + " GROUP BY department_id, status"
                        + " ORDER BY department_id, status",
                (rs, n) -> new ProcurementTotalView(
                        rs.getLong("department_id"),
                        ProcurementStatus.valueOf(rs.getString("status")),
                        rs.getLong("request_count"),
                        rs.getLong("approved_amount"),
                        rs.getLong("actual_amount")),
                statisticsArgs(departmentIds, from, to));
    }

    /** Khoảng nửa mở trên created_at để dùng chỉ mục (department_id, created_at). */
    private static String statisticsWhere(Set<Long> departmentIds) {
        return " WHERE created_at >= ? AND created_at < ?"
                + (departmentIds.isEmpty()
                        ? ""
                        : " AND department_id IN (" + String.join(",", Collections.nCopies(departmentIds.size(), "?"))
                                + ")");
    }

    private static Object[] statisticsArgs(Set<Long> departmentIds, LocalDate from, LocalDate to) {
        var args = new ArrayList<Object>(
                List.of(from.atStartOfDay(), to.plusDays(1).atStartOfDay()));
        args.addAll(departmentIds);
        return args.toArray();
    }

    private int bindContent(PreparedStatement ps, ProcurementContent content, int i, long createdBy)
            throws SQLException {
        ps.setLong(i++, content.departmentId());
        ps.setLong(i++, createdBy);
        return bindContentTail(ps, content, i);
    }

    private int bindContentTail(PreparedStatement ps, ProcurementContent content, int i) throws SQLException {
        ps.setString(i++, content.type().name());
        ps.setString(i++, content.priority());
        ps.setObject(i++, content.neededBy() == null ? null : local(content.neededBy()));
        ps.setString(i++, content.relatedFileIds() == null ? null : write(content.relatedFileIds()));
        ps.setString(i++, write(content.lines()));
        ps.setLong(i++, content.estimatedTotal());
        ps.setString(i++, content.reason());
        ps.setObject(i++, content.neededOn());
        ps.setString(i++, content.reference() == null ? null : write(content.reference()));
        ps.setObject(i++, content.originalRequestId());
        return i;
    }

    private ProcurementRequestRow row(ResultSet rs, int n) throws SQLException {
        return new ProcurementRequestRow(
                rs.getLong("id"),
                rs.getLong("department_id"),
                rs.getLong("created_by_staff_id"),
                ProcurementType.valueOf(rs.getString("type")),
                rs.getString("priority"),
                instant(rs, "needed_by"),
                read(rs.getString("related_file_ids"), IDS),
                read(rs.getString("lines"), LINES),
                rs.getLong("estimated_total"),
                rs.getString("reason"),
                rs.getObject("needed_on", LocalDate.class),
                read(rs.getString("reference"), MAP),
                rs.getObject("original_request_id", Long.class),
                rs.getObject("approved_amount", Long.class),
                rs.getString("reduction_reason"),
                rs.getObject("decided_by_staff_id", Long.class),
                instant(rs, "approved_at"),
                rs.getObject("actual_amount", Long.class),
                rs.getObject("purchased_on", LocalDate.class),
                rs.getString("supplier"),
                read(rs.getString("receipt_file_ids"), IDS),
                rs.getObject("purchase_recorded_by_staff_id", Long.class),
                instant(rs, "purchase_recorded_at"),
                rs.getString("reconciliation_result"),
                rs.getObject("discrepancy_amount", Long.class),
                rs.getString("discrepancy_resolution"),
                rs.getObject("reconciled_by_staff_id", Long.class),
                read(rs.getString("linked_sources"), SOURCES),
                ProcurementStatus.valueOf(rs.getString("status")),
                rs.getString("status_reason"),
                instant(rs, "created_at"),
                instant(rs, "locked_at"),
                rs.getInt("version"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        var value = rs.getObject(column, LocalDateTime.class);
        return value == null ? null : value.atZone(BusinessTime.ZONE).toInstant();
    }

    private static LocalDateTime local(Instant at) {
        return LocalDateTime.ofInstant(at, BusinessTime.ZONE);
    }

    private String write(Object value) {
        return json.writeValueAsString(value);
    }

    private <T> T read(String value, TypeReference<T> type) {
        return value == null ? null : json.readValue(value, type);
    }
}
