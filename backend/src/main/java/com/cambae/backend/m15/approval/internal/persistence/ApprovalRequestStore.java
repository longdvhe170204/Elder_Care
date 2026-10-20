package com.cambae.backend.m15.approval.internal.persistence;

import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.approval.api.ApprovalRequestView;
import com.cambae.backend.m15.approval.api.ResultRef;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Đọc, ghi bảng {@code approval_request} và {@code application_result} bằng JDBC (data-model mục 3). */
@Component
public class ApprovalRequestStore {

    public static final String COLUMNS = "SELECT r.*, a.result_type, a.result_id FROM approval_request r"
            + " LEFT JOIN application_result a ON a.approval_request_id = r.id";

    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public ApprovalRequestStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public Optional<ApprovalRequestView> find(long id) {
        return jdbc.query(COLUMNS + " WHERE r.id = ?", (rs, i) -> view(rs), id).stream()
                .findFirst();
    }

    /** Khóa dòng yêu cầu tới hết transaction ({@code SELECT … FOR UPDATE}) để các lệnh trên cùng yêu cầu tuần tự. */
    public Optional<ApprovalRequestView> lock(long id) {
        List<Long> locked =
                jdbc.queryForList("SELECT id FROM approval_request WHERE id = ? FOR UPDATE", Long.class, id);
        return locked.isEmpty() ? Optional.empty() : find(id);
    }

    public List<ApprovalRequestView> query(String where, Object... args) {
        return jdbc.query(COLUMNS + " WHERE " + where, (rs, i) -> view(rs), args);
    }

    public long insert(
            String type,
            JsonNode content,
            Target target,
            long requesterId,
            Instant createdAt,
            String reason,
            LocalDate requestedEffectiveDate,
            String displayYear) {
        jdbc.update(
                "INSERT INTO approval_request (type, status, content, entity_type, entity_id, resident_id, floor_area_id,"
                        + " requester_id, created_at, reason, requested_effective_date) VALUES (?, 'DRAFT', ?, ?, ?, ?, ?,"
                        + " ?, ?, ?, ?)",
                type,
                json.writeValueAsString(content),
                target.type(),
                target.id(),
                target.residentId(),
                target.floorAreaId(),
                requesterId,
                utc(createdAt),
                reason,
                requestedEffectiveDate);
        long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update(
                "UPDATE approval_request SET display_code = ? WHERE id = ?",
                "YC-" + displayYear + "-" + String.format("%06d", id),
                id);
        return id;
    }

    /** Cập nhật một số cột và tăng phiên bản; {@code assignments} dạng {@code "status = ?, submitted_at = ?"}. */
    public void update(long id, String assignments, Object... args) {
        Object[] all = new Object[args.length + 1];
        System.arraycopy(args, 0, all, 0, args.length);
        all[args.length] = id;
        jdbc.update("UPDATE approval_request SET " + assignments + ", version = version + 1 WHERE id = ?", all);
    }

    /** Cập nhật cột kỹ thuật của Bộ lập lịch (mốc nhắc), không đổi phiên bản người dùng đang xem. */
    public void updateWithoutVersion(long id, String assignments, Object... args) {
        Object[] all = new Object[args.length + 1];
        System.arraycopy(args, 0, all, 0, args.length);
        all[args.length] = id;
        jdbc.update("UPDATE approval_request SET " + assignments + " WHERE id = ?", all);
    }

    public void insertResult(long id, ResultRef result, Instant at) {
        jdbc.update(
                "INSERT INTO application_result (approval_request_id, result_type, result_id, recorded_at)"
                        + " VALUES (?, ?, ?, ?)",
                id,
                result.type(),
                result.id(),
                utc(at));
    }

    public String toJson(JsonNode node) {
        return json.writeValueAsString(node);
    }

    public static LocalDateTime utc(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private ApprovalRequestView view(ResultSet rs) throws SQLException {
        String resultType = rs.getString("result_type");
        return new ApprovalRequestView(
                rs.getLong("id"),
                rs.getString("display_code"),
                rs.getString("type"),
                rs.getString("status"),
                json.readTree(rs.getString("content")),
                new Target(
                        rs.getString("entity_type"),
                        rs.getString("entity_id"),
                        rs.getObject("resident_id", Long.class),
                        rs.getObject("floor_area_id", Long.class)),
                rs.getLong("requester_id"),
                instant(rs, "created_at"),
                rs.getString("reason"),
                date(rs, "requested_effective_date"),
                date(rs, "actual_effective_date"),
                instant(rs, "submitted_at"),
                rs.getObject("decided_by", Long.class),
                instant(rs, "decided_at"),
                rs.getString("decision_comment"),
                rs.getBoolean("self_approved"),
                rs.getString("closing_reason"),
                instant(rs, "applied_at"),
                rs.getBoolean("catch_up_applied"),
                resultType == null ? null : new ResultRef(resultType, rs.getString("result_id")),
                instant(rs, "reminded_at"),
                instant(rs, "escalated_at"),
                rs.getInt("version"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        LocalDateTime value = rs.getObject(column, LocalDateTime.class);
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private static LocalDate date(ResultSet rs, String column) throws SQLException {
        Date value = rs.getDate(column);
        return value == null ? null : value.toLocalDate();
    }
}
