package com.cambae.backend.m01.internal.consent.service;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.data.BusinessCommand;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.file.FileApi;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m01.api.ConsentApi;
import com.cambae.backend.m01.api.dto.ConsentView;
import com.cambae.backend.m01.api.dto.ConsenterType;
import com.cambae.backend.m01.api.events.ConsentChanged;
import com.cambae.backend.m01.internal.consent.domain.Consent;
import com.cambae.backend.m01.internal.consent.domain.CourtDecision;
import com.cambae.backend.m01.internal.consent.persistence.ConsentStore;
import com.cambae.backend.m01.internal.profile.persistence.ResidentRepository;
import com.cambae.backend.m01.internal.profile.service.ResidentProfileService;
import com.cambae.backend.m01.internal.status.service.ProfileOpenGuard;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.RequiresPermission;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.FamilyRelationshipApi;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bản đồng ý chia sẻ dữ liệu (001 US5; FR-010 → FR-016, BR-M01-08; research H-06). Hành chính ghi nhận bản có bằng
 * chứng; bản mới thay bản Hiệu lực cũ trong cùng lệnh (tối đa một bản Hiệu lực); rút lại kèm lý do và bằng chứng, làm
 * được cả trên hồ sơ trạng thái cuối (FR-014a). Cài {@link ConsentApi} cho 009, 012 và điều kiện chú thích ¹ của 002.
 */
@Service
public class ConsentService implements ConsentApi {

    public static final String ENTITY_TYPE = "M01.CONSENT";

    static final AuditAction RECORDED = new AuditAction("CONSENT_RECORDED", false);
    static final AuditAction SUPERSEDED = new AuditAction("CONSENT_SUPERSEDED", false);
    static final AuditAction WITHDRAWN = new AuditAction("CONSENT_WITHDRAWN", false);

    /** Nội dung lệnh ghi nhận; trường có thể thiếu vì kiểm ở {@link #record} để trả đúng mã lỗi. */
    public record RecordConsent(
            ConsenterType consenterType,
            Long consenterFamilyMemberId,
            CourtDecision courtDecision,
            Set<Long> familyMemberIdsAllowedHealthView,
            boolean photoConsent,
            boolean notificationConsent,
            Instant consentedAt,
            Long evidenceFileId) {}

    private final ConsentStore store;
    private final ResidentRepository residents;
    private final ProfileOpenGuard guard;
    private final FileApi files;
    private final AuditLogApi auditLog;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;
    private final ObjectProvider<FamilyRelationshipApi> relationships;
    private final ObjectProvider<CurrentUser> currentUser;

    ConsentService(
            ConsentStore store,
            ResidentRepository residents,
            ProfileOpenGuard guard,
            FileApi files,
            AuditLogApi auditLog,
            ApplicationEventPublisher events,
            BusinessTime time,
            ObjectProvider<FamilyRelationshipApi> relationships,
            ObjectProvider<CurrentUser> currentUser) {
        this.store = store;
        this.residents = residents;
        this.guard = guard;
        this.files = files;
        this.auditLog = auditLog;
        this.events = events;
        this.time = time;
        this.relationships = relationships;
        this.currentUser = currentUser;
    }

    /** Đối tượng kiểm quyền của bản đồng ý: hồ sơ người cao tuổi mà nó thuộc về. */
    @Transactional(readOnly = true)
    public Target targetOf(long consentId) {
        Consent consent = store.find(consentId).orElseThrow(NotFoundException::new);
        return ResidentProfileService.target(consent.residentId());
    }

    @RequiresPermission(function = Function.DATA_SHARING_CONSENT, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<Consent> list(Target target) {
        return store.listByResident(target.residentId());
    }

    /** Ghi nhận bản đồng ý Hiệu lực; bản Hiệu lực cũ (nếu có) chuyển Đã rút lại vì được thay thế. */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.DATA_SHARING_CONSENT, operation = Operation.CREATE)
    @Transactional
    public Consent record(Target target, RecordConsent command) {
        long residentId = target.residentId();
        residents.lockById(residentId).orElseThrow(NotFoundException::new);
        guard.requireOpen(residentId);
        validate(residentId, command);
        CurrentUser user = currentUser.getObject();
        Instant now = time.now();

        Optional<Consent> previous = store.findActive(residentId);
        previous.ifPresent(old -> store.supersede(old.id(), user.accountId(), now));
        long id = store.insert(
                residentId,
                command.consenterType(),
                command.consenterFamilyMemberId(),
                command.courtDecision(),
                command.familyMemberIdsAllowedHealthView() == null
                        ? Set.of()
                        : command.familyMemberIdsAllowedHealthView(),
                command.photoConsent(),
                command.notificationConsent(),
                command.consentedAt(),
                command.evidenceFileId(),
                now,
                user.accountId());
        previous.ifPresent(old -> store.supersededBy(old.id(), id));
        files.attach(command.evidenceFileId(), ENTITY_TYPE, id);
        if (command.courtDecision() != null) {
            files.attach(command.courtDecision().fileId(), ENTITY_TYPE, id);
        }
        Consent saved = store.find(id).orElseThrow();

        auditLog.write(AuditLogEntry.builder()
                .action(RECORDED)
                .entity(ENTITY_TYPE, id)
                .resident(residentId)
                .after(snapshot(saved))
                .build());
        previous.ifPresent(old -> auditLog.write(AuditLogEntry.builder()
                .action(SUPERSEDED)
                .entity(ENTITY_TYPE, old.id())
                .resident(residentId)
                .before(Map.of("status", "ACTIVE"))
                .after(Map.of("status", "WITHDRAWN", "supersededById", id))
                .build()));
        events.publishEvent(new ConsentChanged(residentId, id));
        return saved;
    }

    /**
     * Rút lại đồng ý (FR-014): lý do và bằng chứng bắt buộc; không kiểm hồ sơ đã đóng (FR-014a, ngoại lệ của
     * BR-M01-05). Bản không còn Hiệu lực thì 409 {@code CONSENT_NOT_ACTIVE}.
     */
    @BusinessCommand
    @RequiresPermission(function = Function.DATA_SHARING_CONSENT, operation = Operation.UPDATE)
    @Transactional
    public Consent withdraw(Target target, long consentId, Reason reason, Long evidenceFileId) {
        long residentId = target.residentId();
        residents.lockById(residentId).orElseThrow(NotFoundException::new);
        Consent consent = store.find(consentId).orElseThrow(NotFoundException::new);
        if (consent.residentId() != residentId) {
            throw new NotFoundException();
        }
        if (evidenceFileId == null) {
            throw evidenceRequired();
        }
        CurrentUser user = currentUser.getObject();
        if (store.withdraw(consentId, user.accountId(), time.now(), reason.value(), evidenceFileId) == 0) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "CONSENT_NOT_ACTIVE", "Bản đồng ý không còn Hiệu lực, không rút lại được");
        }
        files.attach(evidenceFileId, ENTITY_TYPE, consentId);
        Consent withdrawn = store.find(consentId).orElseThrow();
        auditLog.write(AuditLogEntry.builder()
                .action(WITHDRAWN)
                .entity(ENTITY_TYPE, consentId)
                .resident(residentId)
                .before(Map.of("status", "ACTIVE"))
                .after(Map.of("status", "WITHDRAWN", "evidenceFileId", evidenceFileId))
                .reason(reason)
                .build());
        events.publishEvent(new ConsentChanged(residentId, null));
        return withdrawn;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConsentView> current(long residentId) {
        return store.findActive(residentId)
                .map(c -> new ConsentView(
                        c.id(),
                        c.residentId(),
                        c.consenterType(),
                        Optional.ofNullable(c.consenterFamilyMemberId()),
                        c.familyMemberIdsAllowedHealthView(),
                        c.photoConsent(),
                        c.notificationConsent()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean includes(long residentId, long familyMemberId) {
        return store.activeIncludes(residentId, familyMemberId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> legalRepresentative(long residentId) {
        return store.findActive(residentId)
                .filter(c -> c.consenterType() == ConsenterType.LEGAL_REPRESENTATIVE)
                .map(Consent::consenterFamilyMemberId);
    }

    private void validate(long residentId, RecordConsent command) {
        if (command.evidenceFileId() == null) {
            throw evidenceRequired();
        }
        CourtDecision decision = command.courtDecision();
        Long signer = command.consenterFamilyMemberId();
        if (command.consenterType() == ConsenterType.RESIDENT) {
            if (decision != null) {
                throw new BusinessException(
                        HttpStatus.UNPROCESSABLE_CONTENT,
                        "COURT_DECISION_NOT_ALLOWED",
                        "Người cao tuổi tự ký thì không có quyết định của Tòa án");
            }
            if (signer != null) {
                throw invalidSigner("Người cao tuổi tự ký thì không có người ký thay");
            }
            return;
        }
        // FR-011, Q-348: người ký thay chỉ là người đại diện theo pháp luật, có đủ quyết định của Tòa án
        if (decision == null || !decision.complete()) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "COURT_DECISION_REQUIRED",
                    "Người ký thay cần quyết định của Tòa án: số, ngày, Tòa án và bản scan (FR-011)");
        }
        // vai trò "người đại diện" của 012 không cho quyền ký thay: chỉ xét quan hệ Hiệu lực với người cao tuổi
        FamilyRelationshipApi api = relationships.getIfAvailable();
        if (signer == null || api == null || !api.belongsToResident(signer, residentId)) {
            throw invalidSigner("Người ký thay phải là người thân có quan hệ Hiệu lực với người cao tuổi");
        }
    }

    private static BusinessException evidenceRequired() {
        return new BusinessException(
                HttpStatus.UNPROCESSABLE_CONTENT, "EVIDENCE_REQUIRED", "Cần bản ký hoặc bằng chứng đã scan");
    }

    private static BusinessException invalidSigner(String message) {
        return new BusinessException(HttpStatus.CONFLICT, "INVALID_SIGNER", message);
    }

    private static Map<String, Object> snapshot(Consent consent) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("consenterType", consent.consenterType().name());
        map.put("consenterFamilyMemberId", consent.consenterFamilyMemberId());
        if (consent.courtDecision() != null) {
            map.put("courtDecisionNumber", consent.courtDecision().number());
            map.put("courtDecisionDate", String.valueOf(consent.courtDecision().date()));
            map.put("courtDecisionCourt", consent.courtDecision().court());
            map.put("courtDecisionFileId", consent.courtDecision().fileId());
        }
        map.put("familyMemberIdsAllowedHealthView", consent.familyMemberIdsAllowedHealthView());
        map.put("photoConsent", consent.photoConsent());
        map.put("notificationConsent", consent.notificationConsent());
        map.put("consentedAt", consent.consentedAt().toString());
        map.put("evidenceFileId", consent.evidenceFileId());
        return map;
    }
}
