package com.cambae.backend.m15.auditlog.internal;

import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.FieldVisibilityApi;
import com.cambae.backend.m15.account.api.InfoGroup;
import com.cambae.backend.m15.account.api.PermissionApi;
import com.cambae.backend.m15.account.api.ScopeApi;
import com.cambae.backend.m15.account.api.Target;
import com.cambae.backend.m15.account.api.Visibility;
import com.cambae.backend.m15.auditlog.api.AuditLogPermissions;
import com.cambae.backend.m15.auditlog.api.FieldClassificationProvider;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tra cứu nhật ký (research R-06; FR-049, BR-M15-11, SC-006). Quản lý viện tra cứu toàn viện; nhân viên chỉ xem lịch
 * sử của đối tượng có quyền xem và trong phạm vi; người khác nhận 403 (toàn viện) hoặc 404 (đối tượng, không lộ tồn
 * tại). Giá trị trường người xem không được xem đổi thành {@code {"changed": true}} ở đây, không gửi về client; có trường
 * bị che thì ẩn lý do. Sắp theo thời điểm giảm dần, phân trang theo khóa {@code (occurred_at, id)}. Tra cứu không ghi
 * nhật ký (FR-043a).
 */
@Service
public class AuditLogQueryService {

    /** Số dòng mỗi trang (thông số kỹ thuật, không phải CFG). */
    static final int PAGE_SIZE = 50;

    private static final Map<String, Object> CHANGED = Map.of("changed", true);
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};
    private static final String COLUMNS = "SELECT id, occurred_at, device_time, actor_type, actor_id, role, action,"
            + " entity_type, entity_id, resident_id, data_before, data_after, reason, basis, details FROM audit_log";

    private final JdbcTemplate jdbc;
    private final JsonMapper json;
    private final ObjectProvider<CurrentUser> currentUser;
    private final ObjectProvider<PermissionApi> permissions;
    private final ObjectProvider<ScopeApi> scope;
    private final ObjectProvider<FieldVisibilityApi> fieldVisibility;
    private final ObjectProvider<FieldClassificationProvider> classifications;

    AuditLogQueryService(
            JdbcTemplate jdbc,
            JsonMapper json,
            ObjectProvider<CurrentUser> currentUser,
            ObjectProvider<PermissionApi> permissions,
            ObjectProvider<ScopeApi> scope,
            ObjectProvider<FieldVisibilityApi> fieldVisibility,
            ObjectProvider<FieldClassificationProvider> classifications) {
        this.jdbc = jdbc;
        this.json = json;
        this.currentUser = currentUser;
        this.permissions = permissions;
        this.scope = scope;
        this.fieldVisibility = fieldVisibility;
        this.classifications = classifications;
    }

    /** Bộ lọc tra cứu toàn viện; trường {@code null} là không lọc. */
    public record Filter(
            String entityType,
            String entityId,
            Long actorId,
            List<String> actions,
            Instant from,
            Instant to,
            Boolean selfApproved) {}

    public enum ActorType {
        USER,
        SCHEDULER
    }

    public record Actor(ActorType type, Long id, String fullName, String role) {}

    public record EntityRef(String type, String id) {}

    /** Một bản ghi nhật ký theo schema {@code AuditLogEntry} của contracts/api-000.yaml. */
    public record Entry(
            String id,
            Instant occurredAt,
            Instant deviceTime,
            Actor actor,
            String action,
            EntityRef entity,
            Map<String, Object> dataBefore,
            Map<String, Object> dataAfter,
            String reason,
            boolean reasonHidden,
            Map<String, Object> basis,
            Map<String, Object> details) {}

    public record Page(List<Entry> items, String nextCursor) {}

    /** Tra cứu toàn viện, chỉ Quản lý viện (cột X của Phụ lục 27). */
    @Transactional(readOnly = true)
    public Page search(Filter filter, String cursor) {
        CurrentUser user = currentUser.getObject();
        if (!permissions
                .getObject()
                .hasPermission(user, AuditLogPermissions.VIEW_FACILITY, Target.UNSPECIFIED)
                .allowed()) {
            throw new PermissionDeniedException("Chỉ Quản lý viện tra cứu nhật ký toàn viện");
        }
        List<String> where = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (filter.entityType() != null) {
            where.add("entity_type = ?");
            args.add(filter.entityType());
        }
        if (filter.entityId() != null) {
            where.add("entity_id = ?");
            args.add(filter.entityId());
        }
        if (filter.actorId() != null) {
            where.add("actor_id = ?");
            args.add(filter.actorId());
        }
        if (filter.actions() != null && !filter.actions().isEmpty()) {
            where.add("action IN ("
                    + String.join(",", Collections.nCopies(filter.actions().size(), "?")) + ")");
            args.addAll(filter.actions());
        }
        if (filter.from() != null) {
            where.add("occurred_at >= ?");
            args.add(utc(filter.from()));
        }
        if (filter.to() != null) {
            where.add("occurred_at <= ?");
            args.add(utc(filter.to()));
        }
        if (filter.selfApproved() != null) {
            // Dấu tự duyệt nằm ở chi tiết (FR-035); dòng không có dấu là không tự duyệt
            where.add("COALESCE(JSON_CONTAINS(details, 'true', '$.selfApproved'), 0) = ?");
            args.add(filter.selfApproved() ? 1 : 0);
        }
        return page(user, where, args, cursor);
    }

    /** Lịch sử một đối tượng: Quản lý viện, hoặc nhân viên có quyền xem đối tượng trong phạm vi (cột P). */
    @Transactional(readOnly = true)
    public Page history(String entityType, String entityId, String cursor) {
        CurrentUser user = currentUser.getObject();
        Target target = new Target(entityType, entityId, null, null);
        boolean facility = permissions
                .getObject()
                .hasPermission(user, AuditLogPermissions.VIEW_FACILITY, Target.UNSPECIFIED)
                .allowed();
        // Loại đối tượng chưa phân loại trường thì chỉ Quản lý viện xem (contracts/module-interfaces.md mục 1.3)
        if (!facility
                && (!classificationsByType().containsKey(entityType)
                        || !permissions
                                .getObject()
                                .hasPermission(user, AuditLogPermissions.VIEW, target)
                                .allowed()
                        || !scope.getObject().inScope(user, target))) {
            throw new NotFoundException();
        }
        List<Object> args = new ArrayList<>(List.of(entityType, entityId));
        return page(user, new ArrayList<>(List.of("entity_type = ?", "entity_id = ?")), args, cursor);
    }

    private Page page(CurrentUser user, List<String> where, List<Object> args, String cursor) {
        if (cursor != null) {
            Cursor after = Cursor.parse(cursor);
            where.add("(occurred_at < ? OR (occurred_at = ? AND id < ?))");
            args.add(after.occurredAt());
            args.add(after.occurredAt());
            args.add(after.id());
        }
        String sql = COLUMNS + (where.isEmpty() ? "" : " WHERE " + String.join(" AND ", where))
                + " ORDER BY occurred_at DESC, id DESC LIMIT " + (PAGE_SIZE + 1);
        List<Row> rows = jdbc.query(sql, (rs, i) -> row(rs), args.toArray());
        boolean more = rows.size() > PAGE_SIZE;
        List<Row> shown = more ? rows.subList(0, PAGE_SIZE) : rows;
        Map<String, Map<String, InfoGroup>> groups = classificationsByType();
        List<Entry> items = shown.stream().map(r -> mask(user, r, groups)).toList();
        String next =
                more ? new Cursor(shown.getLast().occurredAt(), shown.getLast().id()).format() : null;
        return new Page(items, next);
    }

    private Entry mask(CurrentUser user, Row row, Map<String, Map<String, InfoGroup>> groups) {
        Map<String, InfoGroup> fields = groups.getOrDefault(row.entityType(), Map.of());
        Target target = new Target(row.entityType(), row.entityId(), row.residentId(), null);
        Map<InfoGroup, Boolean> visible = new EnumMap<>(InfoGroup.class);
        Function<String, Boolean> canSee = field -> visible.computeIfAbsent(
                fields.getOrDefault(field, InfoGroup.INTERNAL_PUBLIC),
                group -> fieldVisibility.getObject().canView(user, group, target) == Visibility.FULL);
        Map<String, Object> before = maskValues(row.before(), canSee);
        Map<String, Object> after = maskValues(row.after(), canSee);
        boolean hidden = visible.containsValue(false);
        return new Entry(
                String.valueOf(row.id()),
                row.occurredAt().toInstant(ZoneOffset.UTC),
                row.deviceTime() == null ? null : row.deviceTime().toInstant(ZoneOffset.UTC),
                new Actor(actorType(row.actorType()), row.actorId(), null, row.role()),
                row.action(),
                new EntityRef(row.entityType(), row.entityId()),
                before,
                after,
                hidden ? null : row.reason(),
                hidden,
                row.basis(),
                row.details());
    }

    private static Map<String, Object> maskValues(Map<String, Object> values, Function<String, Boolean> canSee) {
        if (values == null) {
            return null;
        }
        Map<String, Object> masked = new LinkedHashMap<>();
        values.forEach((field, value) -> masked.put(field, canSee.apply(field) ? value : CHANGED));
        return masked;
    }

    private static ActorType actorType(String stored) {
        return switch (stored) {
            case "USER" -> ActorType.USER;
            case "SCHEDULER" -> ActorType.SCHEDULER;
            // BOOTSTRAP do 002 ghi (khởi tạo Quản lý viện đầu tiên); hợp đồng 000 chưa có giá trị này
            default -> throw new IllegalStateException("Loại người thực hiện chưa có trong hợp đồng: " + stored);
        };
    }

    private Map<String, Map<String, InfoGroup>> classificationsByType() {
        return classifications
                .orderedStream()
                .collect(Collectors.toMap(
                        FieldClassificationProvider::entityType, FieldClassificationProvider::fieldGroups));
    }

    private Row row(ResultSet rs) throws SQLException {
        return new Row(
                rs.getLong("id"),
                rs.getObject("occurred_at", LocalDateTime.class),
                rs.getObject("device_time", LocalDateTime.class),
                rs.getString("actor_type"),
                rs.getObject("actor_id", Long.class),
                rs.getString("role"),
                rs.getString("action"),
                rs.getString("entity_type"),
                rs.getString("entity_id"),
                rs.getObject("resident_id", Long.class),
                readMap(rs.getString("data_before")),
                readMap(rs.getString("data_after")),
                rs.getString("reason"),
                readMap(rs.getString("basis")),
                readMap(rs.getString("details")));
    }

    private Map<String, Object> readMap(String value) {
        return value == null ? null : json.readValue(value, MAP);
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private record Row(
            long id,
            LocalDateTime occurredAt,
            LocalDateTime deviceTime,
            String actorType,
            Long actorId,
            String role,
            String action,
            String entityType,
            String entityId,
            Long residentId,
            Map<String, Object> before,
            Map<String, Object> after,
            String reason,
            Map<String, Object> basis,
            Map<String, Object> details) {}

    /** Con trỏ trang: thời điểm (micro giây UTC) và id của dòng cuối trang trước. */
    private record Cursor(LocalDateTime occurredAt, long id) {

        String format() {
            long micros = ChronoUnit.MICROS.between(Instant.EPOCH, occurredAt.toInstant(ZoneOffset.UTC));
            return micros + "_" + id;
        }

        static Cursor parse(String value) {
            String[] parts = value.split("_", -1);
            try {
                if (parts.length != 2) {
                    throw new NumberFormatException(value);
                }
                Instant at = Instant.EPOCH.plus(Long.parseLong(parts[0]), ChronoUnit.MICROS);
                return new Cursor(LocalDateTime.ofInstant(at, ZoneOffset.UTC), Long.parseLong(parts[1]));
            } catch (NumberFormatException e) {
                throw new InvalidValueException("Con trỏ trang không hợp lệ");
            }
        }
    }
}
