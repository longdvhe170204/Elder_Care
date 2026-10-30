package com.eldercare.backend.administration.parameter.internal.persistence;

import com.eldercare.backend.administration.parameter.api.dto.ParameterKind;
import com.eldercare.backend.administration.parameter.internal.domain.ParameterHistoryRow;
import com.eldercare.backend.administration.parameter.internal.domain.ParameterRow;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Đọc, ghi {@code parameter} và lịch sử {@code parameter_history} (nhóm 3, DBR-24) (research R-07; FR-011 → FR-022). */
@Component
public class ParameterStore {

    private static final String COLUMNS = "SELECT code, description, kind, unit, constraints, default_value,"
            + " current_value, used_in, version, changed_at FROM parameter";

    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    ParameterStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** Mọi tham số, hoặc của một module ({@code CFG-<module>-…}), theo mã. */
    public List<ParameterRow> list(String module) {
        if (module == null) {
            return jdbc.query(COLUMNS + " ORDER BY code", (rs, i) -> row(rs));
        }
        return jdbc.query(COLUMNS + " WHERE code LIKE ? ORDER BY code", (rs, i) -> row(rs), "CFG-" + module + "-%");
    }

    public Optional<ParameterRow> find(String code) {
        return jdbc.query(COLUMNS + " WHERE code = ?", (rs, i) -> row(rs), code).stream()
                .findFirst();
    }

    /** Tham số có mã {@code referenced}, hoặc có ràng buộc {@code greaterThan} trỏ tới {@code code}. */
    public List<ParameterRow> related(String referenced, String code) {
        return jdbc.query(
                COLUMNS + " WHERE code = ? OR JSON_UNQUOTE(JSON_EXTRACT(constraints, '$.greaterThan')) = ?",
                (rs, i) -> row(rs),
                referenced,
                code);
    }

    public ParameterKind kind(String code) {
        return ParameterKind.valueOf(
                jdbc.queryForObject("SELECT kind FROM parameter WHERE code = ?", String.class, code));
    }

    public JsonNode currentValue(String code) {
        return json.readTree(
                jdbc.queryForObject("SELECT current_value FROM parameter WHERE code = ?", String.class, code));
    }

    /** Giá trị có hiệu lực tại {@code at}; rỗng khi chưa có lần ghi nào tới lúc đó. */
    public Optional<JsonNode> valueAt(String code, Instant at) {
        return jdbc
                .queryForList(
                        "SELECT value FROM parameter_history WHERE code = ? AND effective_from <= ?"
                                + " ORDER BY effective_from DESC LIMIT 1",
                        String.class,
                        code,
                        utc(at))
                .stream()
                .findFirst()
                .map(json::readTree);
    }

    /** Các lần đổi giá trị theo thời điểm hiệu lực (FR-019). */
    public List<ParameterHistoryRow> history(String code) {
        return jdbc.query(
                "SELECT value, effective_from, actor_id, reason, is_default_restore FROM parameter_history"
                        + " WHERE code = ? ORDER BY effective_from",
                (rs, i) -> new ParameterHistoryRow(
                        json.readTree(rs.getString("value")),
                        rs.getObject("effective_from", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                        rs.getObject("actor_id", Long.class),
                        rs.getString("reason"),
                        rs.getBoolean("is_default_restore")),
                code);
    }

    /** Đổi giá trị khi phiên bản bằng {@code expected}; {@code false} khi phiên bản đã khác (FR-021). */
    public boolean changeValue(String code, JsonNode value, Instant at, long actorId, long expected) {
        return jdbc.update(
                        "UPDATE parameter SET current_value = ?, version = version + 1, changed_at = ?, changed_by = ?"
                                + " WHERE code = ? AND version = ?",
                        json.writeValueAsString(value),
                        utc(at),
                        actorId,
                        code,
                        expected)
                == 1;
    }

    public void addHistory(
            String code, JsonNode value, Instant at, long actorId, String reason, boolean defaultRestore) {
        jdbc.update(
                "INSERT INTO parameter_history (code, value, effective_from, actor_id, reason, is_default_restore)"
                        + " VALUES (?, ?, ?, ?, ?, ?)",
                code,
                json.writeValueAsString(value),
                utc(at),
                actorId,
                reason,
                defaultRestore);
    }

    private ParameterRow row(ResultSet rs) throws SQLException {
        LocalDateTime changedAt = rs.getObject("changed_at", LocalDateTime.class);
        String constraints = rs.getString("constraints");
        return new ParameterRow(
                rs.getString("code"),
                rs.getString("description"),
                ParameterKind.valueOf(rs.getString("kind")),
                rs.getString("unit"),
                constraints == null ? null : json.readTree(constraints),
                json.readTree(rs.getString("default_value")),
                json.readTree(rs.getString("current_value")),
                rs.getString("used_in"),
                rs.getInt("version"),
                changedAt == null ? null : changedAt.toInstant(ZoneOffset.UTC));
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
