package com.cambae.backend.m15.account.offline.internal;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.offline.SyncOutcome;
import com.cambae.backend.common.offline.SyncResult;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.common.web.IdempotencyFilter;
import com.cambae.backend.m15.account.api.BedAllocationApi;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.Function;
import com.cambae.backend.m15.account.api.OfflineHandler;
import com.cambae.backend.m15.account.api.Operation;
import com.cambae.backend.m15.account.api.PermissionApi;
import com.cambae.backend.m15.account.api.PermissionKey;
import com.cambae.backend.m15.account.api.PermissionResult;
import com.cambae.backend.m15.account.api.ScopeApi;
import com.cambae.backend.m15.account.api.ScopeFilter;
import com.cambae.backend.m15.account.api.Target;
import com.cambae.backend.m15.account.api.events.RecordPendingReview;
import com.cambae.backend.m15.account.api.events.RecordSyncedAfterPermissionLoss;
import com.cambae.backend.m15.account.scope.internal.ActiveAssignments;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Điều phối đồng bộ ngoại tuyến (002 T056; research A-11; FR-041a, FR-041c, BR-M15-20). Lần kết nối trước khi ngoại
 * tuyến là giá trị nhỏ hơn giữa {@code lastConnectedAt} app gửi và {@code device.last_connected_at} máy chủ đã thấy (đọc
 * trước khi cập nhật bằng thời điểm đồng bộ; plan 002 ghi chú Phase 6). Mỗi bản ghi một transaction:
 *
 * <ol>
 *   <li>trùng {@code idempotencyKey} của cùng người ghi thì trả lại kết quả lần đầu (bảng {@code idempotency_key} của
 *       000, khóa {@code sync:<key>}, 7 ngày);
 *   <li>loại không có handler, thao tác bắt buộc trực tuyến (Q-01), nội dung sai: {@code REJECTED};
 *   <li>thời điểm thiết bị không hợp lý theo FR-041c (a)–(c): lưu {@code pending_review_record}, nhật ký, sự kiện báo
 *       người quyết định, {@code PENDING_REVIEW};
 *   <li>tài khoản không Hoạt động hoặc không có quyền tại thời điểm thiết bị: {@code REJECTED} kèm lý do;
 *   <li>còn đủ quyền lúc đồng bộ: {@code ACCEPTED}; không còn: {@code ACCEPTED_WITH_FLAG} và sự kiện
 *       {@code RecordSyncedAfterPermissionLoss}; handler ghi bản ghi thật với hiệu lực là thời điểm thiết bị.
 * </ol>
 */
@Service
public class SyncService {

    static final PermissionKey VIEW_PROFILE = new PermissionKey(Function.RESIDENT_PROFILE, Operation.VIEW);

    private final JdbcTemplate jdbc;
    private final BusinessTime time;
    private final TransactionTemplate tx;
    private final OfflineHandlers handlers;
    private final PendingReviewRecords pending;
    private final PermissionApi permissions;
    private final ScopeApi scope;
    private final ActiveAssignments assignments;
    private final ParameterApi parameters;
    private final AuditLogApi auditLog;
    private final ApplicationEventPublisher events;
    private final JsonMapper json;
    // ObjectProvider: 003 cài; trước đó chỉ test khai bản giả
    private final ObjectProvider<BedAllocationApi> bedAllocations;

    SyncService(
            JdbcTemplate jdbc,
            BusinessTime time,
            TransactionTemplate tx,
            OfflineHandlers handlers,
            PendingReviewRecords pending,
            PermissionApi permissions,
            ScopeApi scope,
            ActiveAssignments assignments,
            ParameterApi parameters,
            AuditLogApi auditLog,
            ApplicationEventPublisher events,
            JsonMapper json,
            ObjectProvider<BedAllocationApi> bedAllocations) {
        this.jdbc = jdbc;
        this.time = time;
        this.tx = tx;
        this.handlers = handlers;
        this.pending = pending;
        this.permissions = permissions;
        this.scope = scope;
        this.assignments = assignments;
        this.parameters = parameters;
        this.auditLog = auditLog;
        this.events = events;
        this.json = json;
        this.bedAllocations = bedAllocations;
    }

    /** Một bản ghi trong lô; {@code content} là JSON gốc. */
    public record SyncRecord(String idempotencyKey, String recordType, Instant deviceTime, String content) {}

    public record RecordResult(String idempotencyKey, SyncResult result, String reason, String reference) {}

    public record Report(boolean sessionEnded, List<Long> residentsInScope, List<RecordResult> results) {}

    public Report sync(CurrentUser user, Instant lastConnectedAt, List<SyncRecord> records) {
        Instant syncAt = time.now();
        Instant lastConnected = tx.execute(status -> lastConnection(user, lastConnectedAt, syncAt));
        List<RecordResult> results = records.stream()
                .map(r -> tx.execute(status -> {
                    try {
                        return once(user, r, () -> process(user, r, syncAt, lastConnected));
                    } catch (BusinessException e) {
                        // Lỗi khi module ghi bản ghi thật: không giữ phần đã ghi, gửi lại được
                        status.setRollbackOnly();
                        return new RecordResult(r.idempotencyKey(), SyncResult.REJECTED, e.getMessage(), null);
                    }
                }))
                .toList();
        boolean sessionEnded = !activeNow(user.accountId()) || !sessionOpen(user);
        return new Report(sessionEnded, sessionEnded ? List.of() : residentsInScope(user), results);
    }

    private RecordResult process(CurrentUser user, SyncRecord r, Instant syncAt, Instant lastConnected) {
        Optional<OfflineHandler<Object>> found = handlers.byType(r.recordType());
        if (found.isEmpty()) {
            return rejected(r, "UNKNOWN_RECORD_TYPE");
        }
        OfflineHandler<Object> handler = found.get();
        if (handler.requiresOnline()) {
            return rejected(r, "REQUIRES_ONLINE");
        }
        Object content;
        try {
            content = handlers.content(handler, r.content());
            handler.validate(content);
        } catch (JacksonException e) {
            return rejected(r, "INVALID_CONTENT");
        } catch (BusinessException e) {
            return rejected(r, e.getMessage());
        }
        Target target = handler.target(content);
        Optional<PendingReason> implausible = implausible(r.deviceTime(), syncAt, lastConnected);
        if (implausible.isPresent()) {
            return pendingReview(user, r, target, syncAt, lastConnected, implausible.get());
        }
        if (!activeAt(user.accountId(), r.deviceTime())) {
            return rejected(r, "ACCOUNT_NOT_ACTIVE");
        }
        PermissionResult atDevice = permissions.hasPermissionAt(user, handler.permissionKey(), target, r.deviceTime());
        if (!atDevice.allowed()) {
            return rejected(r, "PERMISSION_DENIED_" + atDevice.failedLayer().orElseThrow());
        }
        boolean stillAllowed = activeNow(user.accountId())
                && permissions
                        .hasPermission(user, handler.permissionKey(), target)
                        .allowed();
        SyncOutcome outcome = stillAllowed
                ? new SyncOutcome(SyncResult.ACCEPTED, null)
                : new SyncOutcome(SyncResult.ACCEPTED_WITH_FLAG, "PERMISSION_LOST_BEFORE_SYNC");
        String reference = handler.apply(content, outcome, r.deviceTime());
        if (!stillAllowed) {
            events.publishEvent(new RecordSyncedAfterPermissionLoss(
                    r.recordType(), reference, floor(target, r.deviceTime(), syncAt)));
        }
        return new RecordResult(r.idempotencyKey(), outcome.result(), outcome.reason(), reference);
    }

    /** FR-041c: (a) muộn hơn đồng bộ, (b) sớm hơn lần kết nối cuối, (c) ngoại tuyến quá CFG-M15-08. */
    private Optional<PendingReason> implausible(Instant deviceTime, Instant syncAt, Instant lastConnected) {
        if (deviceTime.isAfter(syncAt)) {
            return Optional.of(PendingReason.LATER_THAN_SYNC);
        }
        if (deviceTime.isBefore(lastConnected)) {
            return Optional.of(PendingReason.EARLIER_THAN_LAST_CONNECTION);
        }
        Duration limit = parameters.read(ParameterCode.CFG_M15_08).asDuration();
        if (Duration.between(lastConnected, syncAt).compareTo(limit) > 0) {
            return Optional.of(PendingReason.EXCEEDS_CFG_M15_08);
        }
        return Optional.empty();
    }

    private RecordResult pendingReview(
            CurrentUser user,
            SyncRecord r,
            Target target,
            Instant syncAt,
            Instant lastConnected,
            PendingReason reason) {
        Long floor = floor(target, r.deviceTime(), syncAt);
        long id = pending.insert(
                r.recordType(),
                r.content(),
                r.idempotencyKey(),
                user.accountId(),
                user.deviceId(),
                r.deviceTime(),
                syncAt,
                lastConnected,
                reason,
                floor);
        auditLog.write(AuditLogEntry.builder()
                .action(OfflineAuditActions.OFFLINE_PENDING_REVIEW)
                .entity(OfflineAuditActions.ENTITY, id)
                .resident(target.residentId())
                .after(Map.of("status", ReviewStatus.PENDING_REVIEW.name()))
                .details(Map.of("recordType", r.recordType(), "pendingReason", reason.name()))
                .deviceTime(r.deviceTime())
                .build());
        events.publishEvent(new RecordPendingReview(id, floor));
        return new RecordResult(
                r.idempotencyKey(), SyncResult.PENDING_REVIEW, reason.name(), OfflineAuditActions.ENTITY + "/" + id);
    }

    /** Tầng/khu của bản ghi lúc ghi (vị trí người cao tuổi lúc ghi, CHK051); không xác định được thì không gắn tầng. */
    private Long floor(Target target, Instant deviceTime, Instant syncAt) {
        Instant at = deviceTime.isAfter(syncAt) ? syncAt : deviceTime;
        return assignments.floorOrEmpty(target, at).orElse(null);
    }

    /** Chống lặp theo bản ghi: cùng người ghi, cùng khóa thì trả lại kết quả lần đầu. */
    private RecordResult once(CurrentUser user, SyncRecord r, Supplier<RecordResult> work) {
        String key = "sync:" + r.idempotencyKey();
        String owner = Long.toString(user.accountId());
        String hash = hash(r);
        Instant now = time.now();
        jdbc.update(
                "DELETE FROM idempotency_key WHERE key_value = ? AND username = ? AND expires_at <= ?",
                key,
                owner,
                utc(now));
        try {
            jdbc.update(
                    "INSERT INTO idempotency_key (key_value, username, request_hash, expires_at) VALUES (?, ?, ?, ?)",
                    key,
                    owner,
                    hash,
                    utc(now.plus(IdempotencyFilter.RETENTION)));
        } catch (DuplicateKeyException e) {
            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT request_hash, result_json FROM idempotency_key WHERE key_value = ? AND username = ?",
                    key,
                    owner);
            if (!hash.equals(row.get("request_hash"))) {
                return rejected(r, "IDEMPOTENCY_KEY_REUSED");
            }
            if (row.get("result_json") == null) {
                return rejected(r, "IDEMPOTENCY_IN_PROGRESS");
            }
            return json.readValue(row.get("result_json").toString(), RecordResult.class);
        }
        RecordResult result = work.get();
        jdbc.update(
                "UPDATE idempotency_key SET result_json = ? WHERE key_value = ? AND username = ?",
                json.writeValueAsString(result),
                key,
                owner);
        return result;
    }

    private Instant lastConnection(CurrentUser user, Instant reported, Instant syncAt) {
        Optional<Instant> seen = jdbc
                .queryForList(
                        "SELECT last_connected_at FROM device WHERE device_id = ? FOR UPDATE",
                        LocalDateTime.class,
                        user.deviceId())
                .stream()
                .findFirst()
                .map(t -> t.toInstant(ZoneOffset.UTC));
        jdbc.update(
                "INSERT INTO device (device_id, last_connected_at, last_account_id) VALUES (?, ?, ?) AS new"
                        + " ON DUPLICATE KEY UPDATE last_connected_at = new.last_connected_at,"
                        + " last_account_id = new.last_account_id",
                user.deviceId(),
                utc(syncAt),
                user.accountId());
        return seen.filter(s -> s.isBefore(reported)).orElse(reported);
    }

    /** Trạng thái Hoạt động tại {@code t} theo lịch sử trạng thái; tài khoản chưa có lịch sử thì theo trạng thái hiện tại. */
    private boolean activeAt(long accountId, Instant t) {
        List<String> at = jdbc.queryForList(
                "SELECT status FROM account_status_history WHERE account_id = ? AND effective_from <= ?"
                        + " ORDER BY effective_from DESC LIMIT 1",
                String.class,
                accountId,
                utc(t));
        if (!at.isEmpty()) {
            return "ACTIVE".equals(at.getFirst());
        }
        Integer rows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM account_status_history WHERE account_id = ?", Integer.class, accountId);
        return rows != null && rows == 0 && activeNow(accountId);
    }

    private boolean activeNow(long accountId) {
        Integer active = jdbc.queryForObject(
                "SELECT COUNT(*) FROM account WHERE id = ? AND (status = 'ACTIVE'"
                        + " OR (status = 'TEMPORARILY_LOCKED' AND locked_until <= ?))",
                Integer.class,
                accountId,
                utc(time.now()));
        return active != null && active > 0;
    }

    private boolean sessionOpen(CurrentUser user) {
        Integer open = jdbc.queryForObject(
                "SELECT COUNT(*) FROM login_session WHERE account_id = ? AND device_id = ? AND ended_at IS NULL",
                Integer.class,
                user.accountId(),
                user.deviceId());
        return open != null && open > 0;
    }

    /** Người cao tuổi còn trong phạm vi để app chặn ghi mới cho người ngoài phạm vi (CHK057). */
    private List<Long> residentsInScope(CurrentUser user) {
        ScopeFilter filter = scope.filter(user, VIEW_PROFILE);
        if (!filter.facilityWide()) {
            return filter.residentIds().stream().sorted().toList();
        }
        BedAllocationApi beds = bedAllocations.getIfAvailable();
        return beds == null
                ? List.of()
                : beds.residentsIn(ScopeFilter.FACILITY, time.now()).stream()
                        .sorted()
                        .toList();
    }

    private static RecordResult rejected(SyncRecord r, String reason) {
        return new RecordResult(r.idempotencyKey(), SyncResult.REJECTED, reason, null);
    }

    private static String hash(SyncRecord r) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(
                    (r.recordType() + "\n" + r.deviceTime() + "\n" + r.content()).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
