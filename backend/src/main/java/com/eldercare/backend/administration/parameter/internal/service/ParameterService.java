package com.eldercare.backend.administration.parameter.internal.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.parameter.api.dto.ParameterKind;
import com.eldercare.backend.administration.parameter.api.dto.ParameterPermissions;
import com.eldercare.backend.administration.parameter.api.dto.ParameterValue;
import com.eldercare.backend.administration.parameter.api.events.ParameterChanged;
import com.eldercare.backend.administration.parameter.api.spi.ParameterValueCheck;
import com.eldercare.backend.administration.parameter.internal.domain.ParameterHistoryRow;
import com.eldercare.backend.administration.parameter.internal.domain.ParameterRow;
import com.eldercare.backend.administration.parameter.internal.domain.ParameterValidator;
import com.eldercare.backend.administration.parameter.internal.domain.ParameterValidator.Definition;
import com.eldercare.backend.administration.parameter.internal.persistence.ParameterStore;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.VersionMismatchException;
import com.eldercare.backend.common.time.BusinessTime;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
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

    private final ParameterStore store;
    private final JsonMapper json;
    private final BusinessTime time;
    private final AuditLogApi auditLog;
    private final ApplicationEventPublisher events;
    private final ParameterValidator validator;
    private final ObjectProvider<CurrentUser> currentUser;
    private final ObjectProvider<PermissionApi> permissions;
    private final ObjectProvider<AccountApi> accounts;
    private final ObjectProvider<ParameterValueCheck> valueChecks;
    private final Map<ParameterCode, ParameterValue> cache = new ConcurrentHashMap<>();

    ParameterService(
            ParameterStore store,
            JsonMapper json,
            BusinessTime time,
            AuditLogApi auditLog,
            ApplicationEventPublisher events,
            ObjectProvider<CurrentUser> currentUser,
            ObjectProvider<PermissionApi> permissions,
            ObjectProvider<AccountApi> accounts,
            ObjectProvider<ParameterValueCheck> valueChecks) {
        this.store = store;
        this.json = json;
        this.time = time;
        this.auditLog = auditLog;
        this.events = events;
        this.validator = new ParameterValidator();
        this.currentUser = currentUser;
        this.permissions = permissions;
        this.accounts = accounts;
        this.valueChecks = valueChecks;
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
        BOOTSTRAP,
        // Lần đăng nhập thất bại, chưa xác định người (002 FR-015); chỉ nhật ký có
        ANONYMOUS
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
        JsonNode value = store.valueAt(code.code(), at)
                .orElseThrow(() -> new IllegalStateException(code.code() + " chưa có giá trị tại " + at));
        return new ParameterValue(code, store.kind(code.code()), value);
    }

    /** Danh sách tham số, lọc theo module (ví dụ {@code M15}) nếu có (FR-011). */
    @Transactional(readOnly = true)
    public List<ParameterView> list(String module) {
        require(ParameterPermissions.VIEW);
        return store.list(module).stream().map(ParameterService::view).toList();
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
        List<ParameterHistoryRow> rows = store.history(code);
        List<HistoryEntry> entries = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            ParameterHistoryRow row = rows.get(i);
            Instant to = i + 1 < rows.size() ? rows.get(i + 1).from() : null;
            if (asOf == null || (!row.from().isAfter(asOf) && (to == null || asOf.isBefore(to)))) {
                entries.add(new HistoryEntry(
                        row.value(),
                        row.from(),
                        to,
                        row.actorId() == null
                                ? new Actor(ActorType.SCHEDULER, null, null, null)
                                : new Actor(ActorType.USER, row.actorId(), fullName(row.actorId()), null),
                        row.reason(),
                        row.defaultRestore()));
            }
        }
        return entries;
    }

    /**
     * Đổi giá trị hoặc khôi phục mặc định (FR-013 → FR-016, FR-020 → FR-022): kiểm quyền và giá trị (kiểm chung rồi
     * {@link ParameterValueCheck} của module sở hữu, 017 research E-04), {@code UPDATE ...
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
            ParameterCode parameter = ParameterCode.of(code);
            valueChecks
                    .orderedStream()
                    .filter(check -> check.code() == parameter)
                    .forEach(check -> check.check(next));
        }
        Instant now = time.now();
        if (!store.changeValue(code, next, now, user.accountId(), expected)) {
            throw new VersionMismatchException(find(code));
        }
        store.addHistory(code, next, now, user.accountId(), reason.value(), restoreDefault);
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

    /** Họ tên người thực hiện theo hồ sơ tài khoản (002 T032a); không có thì {@code null}. */
    private String fullName(Long accountId) {
        return accountId == null
                ? null
                : accounts.getObject().fullName(accountId).orElse(null);
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
        return store.find(code).map(ParameterService::view).orElseThrow(NotFoundException::new);
    }

    /** Tham số mà {@code target} tham chiếu, hoặc tham chiếu tới {@code target} (ràng buộc {@code greaterThan}). */
    private List<Definition> related(ParameterView target) {
        String referenced = target.constraints() != null && target.constraints().has("greaterThan")
                ? target.constraints().get("greaterThan").asString()
                : null;
        return store.related(referenced, target.code()).stream()
                .map(ParameterService::view)
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

    private static ParameterView view(ParameterRow r) {
        return new ParameterView(
                r.code(),
                r.description(),
                r.kind(),
                r.unit(),
                r.constraints(),
                r.defaultValue(),
                r.currentValue(),
                r.usedIn(),
                r.version(),
                r.changedAt());
    }

    private ParameterValue load(ParameterCode code) {
        return new ParameterValue(code, store.kind(code.code()), store.currentValue(code.code()));
    }
}
