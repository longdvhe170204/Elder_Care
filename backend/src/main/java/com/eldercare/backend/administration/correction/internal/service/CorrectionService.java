package com.eldercare.backend.administration.correction.internal.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.approval.api.ApprovalRequestApi;
import com.eldercare.backend.administration.approval.api.CreateRequest;
import com.eldercare.backend.administration.auditlog.api.AuditAction;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.AuditLogEntry;
import com.eldercare.backend.administration.auditlog.api.Basis;
import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.CorrectionChainView;
import com.eldercare.backend.administration.correction.api.CorrectionMade;
import com.eldercare.backend.administration.correction.api.CorrectionResult;
import com.eldercare.backend.administration.correction.api.CorrectionType;
import com.eldercare.backend.administration.correction.api.CorrectionView;
import com.eldercare.backend.administration.correction.api.CreationMethod;
import com.eldercare.backend.administration.correction.api.CurrentValue;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import com.eldercare.backend.administration.correction.internal.domain.Correction;
import com.eldercare.backend.administration.correction.internal.domain.CorrectionMethodSelector;
import com.eldercare.backend.administration.correction.internal.persistence.CorrectionRepository;
import com.eldercare.backend.administration.shared.api.RecordTypeAliases;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.time.BusinessTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Đính chính (research R-08; FR-023 → FR-030): kiểm bản gốc đã xác nhận, chọn cách A/B/C, khóa chuỗi đính chính của
 * bản gốc để cấp số thứ tự, ghi bản đính chính và nhật ký cùng transaction, gọi tác động dây chuyền, phát sự kiện. Quyền,
 * phạm vi và người dùng lấy từ 002 qua {@link ObjectProvider} (chưa có 002 thì dùng bản giả trong test).
 */
@Service
class CorrectionService implements CorrectionApi {

    static final AuditAction CORRECTION_ADDED = new AuditAction("CORRECTION_ADDED", true);

    private final Map<String, CorrectableRecordProvider> providers;
    private final CorrectionRepository corrections;
    private final JdbcTemplate jdbc;
    private final AuditLogApi auditLog;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;
    private final JsonMapper json;
    private final RecordTypeAliases aliases;
    private final ObjectProvider<CurrentUser> currentUser;
    private final ObjectProvider<ScopeApi> scope;
    private final ObjectProvider<AccountApi> accounts;
    /** Lấy lúc dùng: handler {@code M15.CORRECTION} của yêu cầu phê duyệt lại phụ thuộc service này. */
    private final ObjectProvider<ApprovalRequestApi> approvals;

    CorrectionService(
            List<CorrectableRecordProvider> providers,
            CorrectionRepository corrections,
            JdbcTemplate jdbc,
            AuditLogApi auditLog,
            ApplicationEventPublisher events,
            BusinessTime time,
            JsonMapper json,
            RecordTypeAliases aliases,
            ObjectProvider<CurrentUser> currentUser,
            ObjectProvider<ScopeApi> scope,
            ObjectProvider<AccountApi> accounts,
            ObjectProvider<ApprovalRequestApi> approvals) {
        this.providers = providers.stream()
                .collect(Collectors.toMap(CorrectableRecordProvider::recordType, Function.identity()));
        this.corrections = corrections;
        this.jdbc = jdbc;
        this.auditLog = auditLog;
        this.events = events;
        this.time = time;
        this.json = json;
        this.aliases = aliases;
        this.currentUser = currentUser;
        this.scope = scope;
        this.accounts = accounts;
        this.approvals = approvals;
    }

    @Override
    @BusinessCommand
    @Transactional
    public CorrectionResult create(
            String recordType, long originalRecordId, CorrectionType type, Object correctedContent, Reason reason) {
        CorrectableRecordProvider provider = provider(recordType);
        if (!provider.isConfirmed(originalRecordId)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "RECORD_NOT_CONFIRMED", "Bản gốc không tồn tại hoặc chưa xác nhận");
        }
        if (type == CorrectionType.AMEND_CONTENT) {
            if (correctedContent == null) {
                throw new InvalidValueException("Sửa nội dung cần nội dung đúng (FR-024)");
            }
            provider.validateContent(correctedContent);
        }
        CurrentUser user = currentUser.getObject();
        CreationMethod method = new CorrectionMethodSelector(scope.getObject(), accounts.getObject())
                .select(
                        user,
                        new CorrectionMethodSelector.Facts(
                                provider.originalRecorder(originalRecordId),
                                provider.floorArea(originalRecordId),
                                provider.canRecord(user),
                                time.now()))
                .filter(m -> provider.allows(user, m, originalRecordId))
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.FORBIDDEN, "CORRECTION_NOT_ALLOWED", "Không được đính chính bản ghi này (FR-026)"));
        if (method == CreationMethod.C) {
            // Cách C: lập và gửi duyệt yêu cầu M15.CORRECTION; bản đính chính thêm khi yêu cầu được áp dụng (T058)
            CorrectionRequestHandler.Content content =
                    new CorrectionRequestHandler.Content(recordType, originalRecordId, type, correctedContent);
            return new CorrectionResult.PendingApproval(approvals
                    .getObject()
                    .create(new CreateRequest(
                            CorrectionRequestHandler.TYPE, json.valueToTree(content), time.today(), reason, true)));
        }
        return new CorrectionResult.Saved(record(
                provider,
                recordType,
                originalRecordId,
                type,
                correctedContent,
                reason,
                user.accountId(),
                method,
                null));
    }

    /**
     * Thêm bản đính chính cách C khi yêu cầu {@code requestId} được áp dụng (T058), trong transaction áp dụng; người
     * thực hiện là người lập yêu cầu.
     */
    CorrectionView applyApproved(
            CorrectionRequestHandler.Content content, Reason reason, long requesterId, long requestId) {
        CorrectableRecordProvider provider = provider(content.recordType());
        return record(
                provider,
                content.recordType(),
                content.originalRecordId(),
                content.type(),
                content.correctedContent(),
                reason,
                requesterId,
                CreationMethod.C,
                requestId);
    }

    /** Bản gốc còn đính chính được không, xét tại lúc áp dụng yêu cầu (FR-029, FR-038). */
    boolean isVoided(String recordType, long originalRecordId) {
        Set<String> types = aliases.withLegacy(recordType);
        return Boolean.TRUE.equals(jdbc.query(
                "SELECT MAX(voided) FROM correction_chain WHERE record_type IN (" + placeholders(types.size())
                        + ") AND original_record_id = ?",
                rs -> rs.next() ? rs.getBoolean(1) : null,
                args(types, List.of(originalRecordId))));
    }

    boolean isConfirmed(String recordType, long originalRecordId) {
        return provider(recordType).isConfirmed(originalRecordId);
    }

    void validate(String recordType, CorrectionType type, Object correctedContent) {
        CorrectableRecordProvider provider = provider(recordType);
        if (type == CorrectionType.AMEND_CONTENT) {
            if (correctedContent == null) {
                throw new InvalidValueException("Sửa nội dung cần nội dung đúng (FR-024)");
            }
            provider.validateContent(correctedContent);
        }
    }

    Optional<Long> floorArea(String recordType, long originalRecordId) {
        return provider(recordType).floorArea(originalRecordId);
    }

    private CorrectionView record(
            CorrectableRecordProvider provider,
            String recordType,
            long originalRecordId,
            CorrectionType type,
            Object correctedContent,
            Reason reason,
            long actorId,
            CreationMethod method,
            Long approvalRequestId) {
        Correction saved = append(
                recordType, originalRecordId, type, correctedContent, reason, actorId, method, approvalRequestId);
        CorrectionView view = view(saved);
        auditLog.write(AuditLogEntry.builder()
                .action(CORRECTION_ADDED)
                .entity(recordType, originalRecordId)
                .resident(provider.residentId(originalRecordId).orElse(null))
                .after(afterOf(view))
                .reason(reason)
                .basis(approvalRequestId == null ? null : new Basis(null, approvalRequestId, null, null))
                .build());
        provider.afterCorrection(view);
        events.publishEvent(new CorrectionMade(recordType, originalRecordId, type, view.id()));
        return view;
    }

    /** Khóa chuỗi của bản gốc rồi cấp số thứ tự kế tiếp; bản gốc đã Hủy ghi nhận thì từ chối (FR-027 → FR-029). */
    private Correction append(
            String recordType,
            long originalRecordId,
            CorrectionType type,
            Object correctedContent,
            Reason reason,
            long actorId,
            CreationMethod method,
            Long approvalRequestId) {
        int created = jdbc.update(
                "INSERT IGNORE INTO correction_chain (record_type, original_record_id) VALUES (?, ?)",
                recordType,
                originalRecordId);
        Set<String> legacy = new java.util.LinkedHashSet<>(aliases.withLegacy(recordType));
        legacy.remove(recordType);
        if (created == 1 && !legacy.isEmpty()) {
            // Bản gốc đã có đính chính ghi với loại cũ trước khi gộp bảng: số thứ tự, Hủy ghi nhận nối tiếp (R-15 mục
            // 5)
            Map<String, Object> old = jdbc.queryForMap(
                    "SELECT IFNULL(MAX(correction_count), 0) AS n, IFNULL(MAX(voided), FALSE) AS v FROM correction_chain"
                            + " WHERE record_type IN (" + placeholders(legacy.size()) + ") AND original_record_id = ?",
                    args(legacy, List.of(originalRecordId)));
            jdbc.update(
                    "UPDATE correction_chain SET correction_count = ?, voided = ? WHERE record_type = ?"
                            + " AND original_record_id = ?",
                    ((Number) old.get("n")).intValue(),
                    ((Number) old.get("v")).intValue() == 1,
                    recordType,
                    originalRecordId);
        }
        Map<String, Object> chain = jdbc.queryForMap(
                "SELECT correction_count, voided FROM correction_chain"
                        + " WHERE record_type = ? AND original_record_id = ? FOR UPDATE",
                recordType,
                originalRecordId);
        if (Boolean.TRUE.equals(chain.get("voided"))) {
            throw voided();
        }
        int sequenceNo = ((Number) chain.get("correction_count")).intValue() + 1;
        jdbc.update(
                "UPDATE correction_chain SET correction_count = ?, voided = ? WHERE record_type = ? AND original_record_id = ?",
                sequenceNo,
                type == CorrectionType.VOID,
                recordType,
                originalRecordId);
        return corrections.append(new Correction(
                recordType,
                originalRecordId,
                type,
                correctedContent == null ? null : json.writeValueAsString(correctedContent),
                reason.value(),
                actorId,
                method,
                approvalRequestId,
                time.now(),
                sequenceNo));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, CurrentValue> current(String recordType, Collection<Long> originalRecordIds) {
        Map<Long, CurrentValue> result = new LinkedHashMap<>();
        if (originalRecordIds.isEmpty()) {
            return result;
        }
        Set<String> types = aliases.withLegacy(recordType);
        String where = " WHERE record_type IN (" + placeholders(types.size()) + ") AND original_record_id IN ("
                + placeholders(originalRecordIds.size()) + ")";
        Object[] args = args(types, originalRecordIds);
        Map<Long, Object> latest = new HashMap<>();
        Map<Long, Long> latestId = new HashMap<>();
        jdbc.query(
                "SELECT original_record_id, corrected_content, correction_id FROM v_latest_correction" + where,
                rs -> {
                    long id = rs.getLong(1);
                    if (rs.getLong(3) > latestId.getOrDefault(id, 0L)) {
                        latestId.put(id, rs.getLong(3));
                        latest.put(id, parse(rs.getString(2)));
                    }
                },
                args);
        Map<Long, Boolean> chains = new HashMap<>();
        jdbc.query(
                "SELECT original_record_id, voided FROM correction_chain" + where,
                rs -> {
                    chains.merge(rs.getLong(1), rs.getBoolean(2), Boolean::logicalOr);
                },
                args);
        for (Long id : originalRecordIds) {
            result.put(id, new CurrentValue(latest.get(id), chains.containsKey(id), chains.getOrDefault(id, false)));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public CorrectionChainView chain(String recordType, long originalRecordId) {
        CorrectableRecordProvider provider = provider(recordType);
        if (!provider.isConfirmed(originalRecordId)) {
            throw new NotFoundException();
        }
        List<CorrectionView> list = corrections
                .findByRecordTypeInAndOriginalRecordIdOrderBySequenceNo(
                        aliases.withLegacy(recordType), originalRecordId)
                .stream()
                .map(this::view)
                .toList();
        CurrentValue current = current(recordType, List.of(originalRecordId)).get(originalRecordId);
        Object original = provider.originalContent(originalRecordId);
        return new CorrectionChainView(
                recordType,
                originalRecordId,
                current.corrected(),
                current.voided(),
                original,
                current.content() == null ? original : current.content(),
                list);
    }

    private static BusinessException voided() {
        return new BusinessException(
                HttpStatus.CONFLICT, "RECORD_VOIDED", "Bản ghi đã Hủy ghi nhận, không đính chính thêm (FR-029)");
    }

    private CorrectableRecordProvider provider(String recordType) {
        CorrectableRecordProvider provider = providers.get(recordType);
        if (provider == null) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "CORRECTION_NOT_SUPPORTED", "Loại bản ghi này không có đính chính");
        }
        return provider;
    }

    private CorrectionView view(Correction c) {
        return new CorrectionView(
                c.getId(),
                aliases.current(c.getRecordType()),
                c.getOriginalRecordId(),
                c.getSequenceNo(),
                c.getType(),
                parse(c.getCorrectedContent()),
                c.getReason(),
                c.getActorId(),
                c.getCreationMethod(),
                c.getApprovalRequestId(),
                c.getRecordedAt());
    }

    private static Map<String, Object> afterOf(CorrectionView view) {
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("sequenceNo", view.sequenceNo());
        after.put("type", view.type().name());
        after.put("creationMethod", view.creationMethod().name());
        if (view.correctedContent() != null) {
            after.put("correctedContent", view.correctedContent());
        }
        return after;
    }

    private static String placeholders(int n) {
        return String.join(", ", java.util.Collections.nCopies(n, "?"));
    }

    private static Object[] args(Collection<String> types, Collection<?> rest) {
        return java.util.stream.Stream.concat(types.stream(), rest.stream()).toArray();
    }

    private Object parse(String value) {
        return value == null ? null : json.readValue(value, Object.class);
    }
}
