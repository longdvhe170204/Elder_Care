package com.cambae.backend.m15.parameter.internal;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.data.BusinessCommand;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.common.error.VersionMismatchException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.PermissionApi;
import com.cambae.backend.m15.account.api.PermissionKey;
import com.cambae.backend.m15.account.api.Target;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterChanged;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import com.cambae.backend.m15.parameter.api.ParameterKind;
import com.cambae.backend.m15.parameter.api.ParameterPermissions;
import com.cambae.backend.m15.parameter.api.ParameterValue;
import com.cambae.backend.m15.parameter.internal.ParameterValidator.Definition;
import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tham số cấu hình (research R-07; FR-011 → FR-022, DBR-24, BR-M15-04). Giá trị hiện hành giữ trong bộ đệm của tiến
 * trình; lệnh đổi giá trị xóa bộ đệm khi transaction kết thúc (commit hay rollback), nên lần kiểm tra quy tắc đầu tiên
 * sau commit đã thấy giá trị mới (FR-016, SC-004). Giá trị quá khứ đọc từ parameter_history (FR-019). Xem và đổi chỉ
 * Quản lý viện (Phụ lục 27, cột C).
 */
@Service
public class ParameterService implements ParameterApi {

    static final String ENTITY = "M15.PARAMETER";
    static final AuditAction PARAMETER_CHANGED = new AuditAction("PARAMETER_CHANGED", true);

    private static final String COLUMNS = "SELECT code, description, kind, unit, constraints, default_value,"
            + " current_value, used_in, version, changed_at FROM parameter";

    private final JdbcTemplate jdbc;
    private final JsonMapper json;
    private final BusinessTime time;
    private final AuditLogApi auditLog;
    private final ApplicationEventPublisher events;
    private final ParameterValidator validator;
    private final ObjectProvider<CurrentUser> currentUser;
    private final ObjectProvider<PermissionApi> permissions;
    private final Map<ParameterCode, ParameterValue> cache = new ConcurrentHashMap<>();

    ParameterService(
            JdbcTemplate jdbc,
            JsonMapper json,
            BusinessTime time,
            AuditLogApi auditLog,
            ApplicationEventPublisher events,
            ObjectProvider<CurrentUser> currentUser,
            ObjectProvider<PermissionApi> permissions) {
        this.jdbc = jdbc;
        this.json = json;
        this.time = time;
        this.auditLog = auditLog;
        this.events = events;
        this.validator = new ParameterValidator();
        this.currentUser = currentUser;
        this.permissions = permissions;
    }

    /** Một tham số theo schema {@code Parameter} của contracts/api-000.yaml. */
    public record ParameterView(
            String code,
            String description,
            ParameterKind type,
            String unit,
            @Schema(implementation = Object.class) JsonNode constraints,
            @Schema(implementation = Object.class) JsonNode defaultValue,
            @Schema(implementation = Object.class) JsonNode currentValue,
            String usedIn,
            int version,
            Instant changedAt) {}

    public enum ActorType {
        USER,
        SCHEDULER,
        // Công cụ khởi tạo Quản lý viện đầu tiên (002 BR-M15-22); chỉ nhật ký có
        BOOTSTRAP
    }

    public record Actor(ActorType type, Long id, String fullName, String role) {}

    /** Một dòng lịch sử theo schema {@code ParameterHistoryEntry}; {@code effectiveTo} suy ra từ dòng kế tiếp. */
    public record HistoryEntry(
            @Schema(implementation = Object.class) JsonNode value,
            Instant effectiveFrom,
            Instant effectiveTo,
            Actor actor,
            String reason,
            boolean isDefaultRestore) {}

    @Override
    public ParameterValue read(ParameterCode code) {
        return cache.computeIfAbsent(code, this::load);
    }

    @Override
    public ParameterValue readAt(ParameterCode code, Instant at) {
        List<String> values = jdbc.queryForList(
                "SELECT value FROM parameter_history WHERE code = ? AND effective_from <= ?"
                        + " ORDER BY effective_from DESC LIMIT 1",
                String.class,
                code.code(),
                utc(at));
        if (values.isEmpty()) {
            throw new IllegalStateException(code.code() + " chưa có giá trị tại " + at);
        }
        return new ParameterValue(code, kindOf(code), json.readTree(values.getFirst()));
    }

    /** Danh sách tham số, lọc theo module (ví dụ {@code M15}) nếu có (FR-011). */
    @Transactional(readOnly = true)
    public List<ParameterView> list(String module) {
        require(ParameterPermissions.VIEW);
        if (module == null) {
            return jdbc.query(COLUMNS + " ORDER BY code", (rs, i) -> view(rs));
        }
        return jdbc.query(COLUMNS + " WHERE code LIKE ? ORDER BY code", (rs, i) -> view(rs), "CFG-" + module + "-%");
    }

    /** Chi tiết một tham số; mã không có hoặc người xem không có quyền thì 404 (hợp đồng chỉ khai 404). */
    @Transactional(readOnly = true)
    public ParameterView detail(String code) {
        if (!allowed(ParameterPermissions.VIEW)) {
            throw new NotFoundException();
        }
        return find(code);
    }

    /** Lịch sử giá trị theo thời gian tăng dần; có {@code asOf} thì chỉ dòng hiện hành tại thời điểm đó (FR-012, FR-019). */
    @Transactional(readOnly = true)
    public List<HistoryEntry> history(String code, Instant asOf) {
        if (!allowed(ParameterPermissions.VIEW)) {
            throw new NotFoundException();
        }
        find(code);
        List<Row> rows = jdbc.query(
                "SELECT value, effective_from, actor_id, reason, is_default_restore FROM parameter_history"
                        + " WHERE code = ? ORDER BY effective_from",
                (rs, i) -> new Row(
                        json.readTree(rs.getString("value")),
                        rs.getObject("effective_from", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                        rs.getObject("actor_id", Long.class),
                        rs.getString("reason"),
                        rs.getBoolean("is_default_restore")),
                code);
        List<HistoryEntry> entries = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            Instant to = i + 1 < rows.size() ? rows.get(i + 1).from() : null;
            if (asOf == null || (!row.from().isAfter(asOf) && (to == null || asOf.isBefore(to)))) {
                entries.add(new HistoryEntry(
                        row.value(),
                        row.from(),
                        to,
                        row.actorId() == null
                                ? new Actor(ActorType.SCHEDULER, null, null, null)
                                : new Actor(ActorType.USER, row.actorId(), null, null),
                        row.reason(),
                        row.defaultRestore()));
            }
        }
        return entries;
    }

    /**
     * Đổi giá trị hoặc khôi phục mặc định (FR-013 → FR-016, FR-020 → FR-022): kiểm quyền và giá trị, {@code UPDATE ...
     * WHERE version = ?} (lệch phiên bản thì 412 kèm giá trị hiện hành), thêm dòng lịch sử, ghi nhật ký cùng transaction,
     * phát {@link ParameterChanged}; bộ đệm xóa khi transaction kết thúc.
     */
    @BusinessCommand(requiresReason = true)
    @Transactional
    public ParameterView changeValue(
            String code, String ifMatch, JsonNode value, boolean restoreDefault, Reason reason) {
        CurrentUser user = require(ParameterPermissions.CHANGE);
        ParameterView current = find(code);
        long expected = parseVersion(ifMatch);
        JsonNode next = restoreDefault ? current.defaultValue() : value;
        if (expected != current.version()) {
            throw new VersionMismatchException(current);
        }
        if (!(restoreDefault && next.isNull())) {
            validator.validate(definition(current), next, related(current));
        }
        Instant now = time.now();
        int updated = jdbc.update(
                "UPDATE parameter SET current_value = ?, version = version + 1, changed_at = ?, changed_by = ?"
                        + " WHERE code = ? AND version = ?",
                json.writeValueAsString(next),
                utc(now),
                user.accountId(),
                code,
                expected);
        if (updated == 0) {
            throw new VersionMismatchException(find(code));
        }
        jdbc.update(
                "INSERT INTO parameter_history (code, value, effective_from, actor_id, reason, is_default_restore)"
                        + " VALUES (?, ?, ?, ?, ?, ?)",
                code,
                json.writeValueAsString(next),
                utc(now),
                user.accountId(),
                reason.value(),
                restoreDefault);
        Map<String, Object> before = new HashMap<>();
        before.put("value", current.currentValue());
        Map<String, Object> after = new HashMap<>();
        after.put("value", next);
        if (restoreDefault) {
            after.put("isDefaultRestore", true);
        }
        auditLog.write(AuditLogEntry.builder()
                .action(PARAMETER_CHANGED)
                .entity(ENTITY, code)
                .before(before)
                .after(after)
                .reason(reason)
                .build());
        ParameterCode parameter = ParameterCode.of(code);
        events.publishEvent(new ParameterChanged(parameter, current.currentValue(), next, now));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                cache.remove(parameter);
            }
        });
        return find(code);
    }

    private CurrentUser require(PermissionKey key) {
        CurrentUser user = currentUser.getObject();
        if (!permissions
                .getObject()
                .hasPermission(user, key, Target.UNSPECIFIED)
                .allowed()) {
            throw new PermissionDeniedException("Chỉ Quản lý viện xem và đổi tham số (BR-M15-04)");
        }
        return user;
    }

    private boolean allowed(PermissionKey key) {
        return permissions
                .getObject()
                .hasPermission(currentUser.getObject(), key, Target.UNSPECIFIED)
                .allowed();
    }

    private ParameterView find(String code) {
        List<ParameterView> found = jdbc.query(COLUMNS + " WHERE code = ?", (rs, i) -> view(rs), code);
        if (found.isEmpty()) {
            throw new NotFoundException();
        }
        return found.getFirst();
    }

    /** Tham số mà {@code target} tham chiếu, hoặc tham chiếu tới {@code target} (ràng buộc {@code greaterThan}). */
    private List<Definition> related(ParameterView target) {
        String referenced = target.constraints() != null && target.constraints().has("greaterThan")
                ? target.constraints().get("greaterThan").asString()
                : null;
        return jdbc
                .query(
                        COLUMNS + " WHERE code = ? OR JSON_UNQUOTE(JSON_EXTRACT(constraints, '$.greaterThan')) = ?",
                        (rs, i) -> view(rs),
                        referenced,
                        target.code())
                .stream()
                .map(ParameterService::definition)
                .toList();
    }

    private static Definition definition(ParameterView view) {
        return new Definition(view.code(), view.type(), view.unit(), view.constraints(), view.currentValue());
    }

    /** If-Match {@code "3"}, {@code W/"3"} hoặc {@code 3}; không đọc được thì coi như sai phiên bản (FR-021). */
    private static long parseVersion(String ifMatch) {
        String value = ifMatch == null ? "" : ifMatch.strip();
        if (value.startsWith("W/")) {
            value = value.substring(2);
        }
        try {
            return Long.parseLong(value.replace("\"", ""));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private ParameterView view(ResultSet rs) throws SQLException {
        LocalDateTime changedAt = rs.getObject("changed_at", LocalDateTime.class);
        String constraints = rs.getString("constraints");
        return new ParameterView(
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

    private ParameterValue load(ParameterCode code) {
        Map<String, Object> row =
                jdbc.queryForMap("SELECT kind, current_value FROM parameter WHERE code = ?", code.code());
        return new ParameterValue(
                code,
                ParameterKind.valueOf((String) row.get("kind")),
                json.readTree(row.get("current_value").toString()));
    }

    private ParameterKind kindOf(ParameterCode code) {
        return ParameterKind.valueOf(
                jdbc.queryForObject("SELECT kind FROM parameter WHERE code = ?", String.class, code.code()));
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private record Row(JsonNode value, Instant from, Long actorId, String reason, boolean defaultRestore) {}
}
