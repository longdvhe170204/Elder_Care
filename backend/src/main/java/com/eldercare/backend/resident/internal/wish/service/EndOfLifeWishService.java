package com.eldercare.backend.resident.internal.wish.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipApi;
import com.eldercare.backend.administration.auditlog.api.AuditAction;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.AuditLogEntry;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.OpinionSourceApi;
import com.eldercare.backend.resident.api.dto.CourtDecisionRef;
import com.eldercare.backend.resident.api.dto.EndOfLifeWishView;
import com.eldercare.backend.resident.api.dto.RecordEndOfLifeWish;
import com.eldercare.backend.resident.api.dto.SelfDeterminationAssessment;
import com.eldercare.backend.resident.api.dto.WishOpinion;
import com.eldercare.backend.resident.api.dto.WishRecordSource;
import com.eldercare.backend.resident.api.dto.WishSignerType;
import com.eldercare.backend.resident.internal.health.service.ActorNames;
import com.eldercare.backend.resident.internal.profile.persistence.ResidentRepository;
import com.eldercare.backend.resident.internal.profile.service.ResidentProfileService;
import com.eldercare.backend.resident.internal.status.service.ProfileOpenGuard;
import com.eldercare.backend.resident.internal.wish.domain.EndOfLifeWish;
import com.eldercare.backend.resident.internal.wish.persistence.EndOfLifeWishStore;
import com.eldercare.backend.resident.internal.wish.persistence.EndOfLifeWishStore.NewWish;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phiếu nguyện vọng cuối đời (001 US7; FR-023a → FR-023i; research H-09). Ghi là ghi phiên bản: bản Hiệu lực cũ chuyển
 * Được thay thế trong cùng lệnh (tối đa một bản Hiệu lực, DBR-34). Người ký theo nhận định của Bác sĩ cho chính phiếu
 * (FR-023c); mỗi ý kiến khác lưu một dòng có nguồn (FR-023g). Hồ sơ trạng thái cuối không ghi thêm phiên bản (BR-M01-05),
 * phiên bản Hiệu lực giữ nguyên để tra cứu (FR-023f). 007 gọi {@link #recordVersion} qua {@code EndOfLifeWishApi}.
 */
@Service
public class EndOfLifeWishService {

    public static final String ENTITY_TYPE = "M01.END_OF_LIFE_WISH";

    static final AuditAction RECORDED = new AuditAction("END_OF_LIFE_WISH_RECORDED", false);
    static final AuditAction SUPERSEDED = new AuditAction("END_OF_LIFE_WISH_SUPERSEDED", false);

    /** Phiên bản Hiệu lực và lịch sử; {@code hasWish} là cờ "đã có / chưa có" (FR-023e). */
    public record Summary(boolean hasWish, Optional<EndOfLifeWish> current, List<EndOfLifeWish> history) {}

    private final EndOfLifeWishStore store;
    private final ResidentRepository residents;
    private final ProfileOpenGuard guard;
    private final FileApi files;
    private final AuditLogApi auditLog;
    private final BusinessTime time;
    private final OpinionSourceApi opinionSources;
    private final AccountApi accounts;
    private final ActorNames names;
    private final ObjectProvider<FamilyRelationshipApi> relationships;
    private final ObjectProvider<CurrentUser> currentUser;

    EndOfLifeWishService(
            EndOfLifeWishStore store,
            ResidentRepository residents,
            ProfileOpenGuard guard,
            FileApi files,
            AuditLogApi auditLog,
            BusinessTime time,
            OpinionSourceApi opinionSources,
            AccountApi accounts,
            ActorNames names,
            ObjectProvider<FamilyRelationshipApi> relationships,
            ObjectProvider<CurrentUser> currentUser) {
        this.store = store;
        this.residents = residents;
        this.guard = guard;
        this.files = files;
        this.auditLog = auditLog;
        this.time = time;
        this.opinionSources = opinionSources;
        this.accounts = accounts;
        this.names = names;
        this.relationships = relationships;
        this.currentUser = currentUser;
    }

    /** Đối tượng kiểm quyền của phiếu: hồ sơ người cao tuổi mà nó thuộc về (quyền tải bản ký). */
    @Transactional(readOnly = true)
    public Target targetOf(long wishId) {
        return ResidentProfileService.target(
                store.find(wishId).orElseThrow(NotFoundException::new).residentId());
    }

    @RequiresPermission(function = Function.END_OF_LIFE_WISHES, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public Summary get(Target target) {
        Optional<EndOfLifeWish> current = store.findActive(target.residentId());
        return new Summary(current.isPresent(), current, store.listByResident(target.residentId()));
    }

    /** Họ tên người ký để hiện: người thân theo hồ sơ của 012, người cao tuổi theo hồ sơ của chính họ. */
    @Transactional(readOnly = true)
    public String signerName(EndOfLifeWish wish) {
        if (wish.signerFamilyMemberId() == null) {
            return residents
                    .findById(wish.residentId())
                    .map(r -> r.getFullName())
                    .orElse(null);
        }
        FamilyRelationshipApi api = relationships.getIfAvailable();
        return api == null
                ? null
                : api.familyMemberName(wish.signerFamilyMemberId()).orElse(null);
    }

    /** Họ tên người ghi để hiện. */
    public String actorName(long accountId) {
        return names.of(accountId);
    }

    /** Cờ "đã có phiếu Hiệu lực" cho hồ sơ; không kiểm quyền vì chỉ là có/không (FR-023e). */
    @Transactional(readOnly = true)
    public boolean hasActive(long residentId) {
        return store.findActive(residentId).isPresent();
    }

    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.END_OF_LIFE_WISHES, operation = Operation.CREATE)
    @Transactional
    public EndOfLifeWish record(Target target, RecordEndOfLifeWish command, WishRecordSource source) {
        if (command.residentId() != target.residentId()) {
            throw new NotFoundException();
        }
        residents.lockById(command.residentId()).orElseThrow(NotFoundException::new);
        guard.requireOpen(command.residentId());
        return store.find(write(command, source)).orElseThrow();
    }

    /** Phiên bản Hiệu lực cho 007 (thẻ khẩn cấp, cảnh báo nguy kịch). */
    @Transactional(readOnly = true)
    public Optional<EndOfLifeWishView> currentView(long residentId) {
        return store.findActive(residentId)
                .map(w -> new EndOfLifeWishView(
                        w.id(),
                        w.residentId(),
                        w.choice(),
                        w.firstContact(),
                        w.note(),
                        w.signerType(),
                        w.signedAt(),
                        w.source()));
    }

    /**
     * Ghi phiên bản trong transaction của lệnh gọi (007, BR-M05-16); quyền do lệnh gọi kiểm. Trả id phiên bản mới.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public long recordVersion(RecordEndOfLifeWish command, WishRecordSource source) {
        residents.lockById(command.residentId()).orElseThrow(NotFoundException::new);
        guard.requireOpen(command.residentId());
        return write(command, source);
    }

    /** Phần chung của hai đường ghi; người gọi đã khóa hồ sơ và kiểm hồ sơ chưa ở trạng thái cuối. */
    private long write(RecordEndOfLifeWish command, WishRecordSource source) {
        long residentId = command.residentId();
        validate(residentId, command);
        long accountId = currentUser.getObject().accountId();
        Instant now = time.now();
        SelfDeterminationAssessment assessment = command.assessment();

        Optional<EndOfLifeWish> previous = store.findActive(residentId);
        previous.ifPresent(old -> store.supersede(old.id()));
        long id = store.insert(
                new NewWish(
                        residentId,
                        command.choice(),
                        command.firstContact().strip(),
                        blankToNull(command.note()),
                        command.respondentType(),
                        command.respondentFamilyMemberId(),
                        blankToNull(command.relationship()),
                        command.assessingDoctorId(),
                        assessment.canSelfDetermine(),
                        now,
                        assessment.supportMethod().strip(),
                        assessment.reviewDate(),
                        command.signerType(),
                        command.signerFamilyMemberId(),
                        command.representativeSelection(),
                        command.courtDecision(),
                        command.signedAt() == null ? now : command.signedAt(),
                        command.signedCopyFileId(),
                        source,
                        accountId,
                        now),
                opinionsOf(command));
        previous.ifPresent(old -> store.supersededBy(old.id(), id));
        files.attach(command.signedCopyFileId(), ENTITY_TYPE, id);
        if (command.courtDecision() != null) {
            files.attach(command.courtDecision().fileId(), ENTITY_TYPE, id);
        }

        auditLog.write(AuditLogEntry.builder()
                .action(RECORDED)
                .entity(ENTITY_TYPE, id)
                .resident(residentId)
                .after(snapshot(store.find(id).orElseThrow()))
                .build());
        previous.ifPresent(old -> auditLog.write(AuditLogEntry.builder()
                .action(SUPERSEDED)
                .entity(ENTITY_TYPE, old.id())
                .resident(residentId)
                .before(Map.of("status", "ACTIVE"))
                .after(Map.of("status", "SUPERSEDED", "supersededById", id))
                .build()));
        return id;
    }

    private void validate(long residentId, RecordEndOfLifeWish command) {
        SelfDeterminationAssessment assessment = command.assessment();
        if (assessment == null
                || assessment.supportMethod() == null
                || assessment.supportMethod().isBlank()
                || assessment.reviewDate() == null) {
            throw unprocessable(
                    "SELF_DETERMINATION_ASSESSMENT_REQUIRED",
                    "Nhận định của Bác sĩ cần có cách hỗ trợ người cao tuổi hiểu thông tin và ngày xem lại (FR-023h)");
        }
        if (assessment.reviewDate().isBefore(time.today())) {
            throw unprocessable("INVALID_REVIEW_DATE", "Ngày xem lại nhận định không được trước ngày nhận định");
        }
        if (command.assessingDoctorId() == null || !accounts.hasRole(command.assessingDoctorId(), Role.DOCTOR)) {
            throw unprocessable("INVALID_ASSESSING_DOCTOR", "Người nhận định phải là Bác sĩ (FR-023a)");
        }
        if (command.choice() == null
                || command.firstContact() == null
                || command.firstContact().isBlank()) {
            throw unprocessable("FIRST_CONTACT_REQUIRED", "Cần lựa chọn và người cần liên hệ trước khi thực hiện");
        }
        if (command.signedCopyFileId() == null) {
            throw unprocessable("EVIDENCE_REQUIRED", "Cần bản ký đã scan");
        }
        if (command.respondentType() != WishSignerType.RESIDENT
                && (command.respondentFamilyMemberId() == null
                        || command.relationship() == null
                        || command.relationship().isBlank())) {
            throw unprocessable(
                    "RESPONDENT_REQUIRED", "Người trả lời là người đại diện cần có hồ sơ người thân và quan hệ");
        }
        validateSigner(residentId, command, assessment.canSelfDetermine());
        if (command.respondentType() != WishSignerType.RESIDENT
                && !isRelated(command.respondentFamilyMemberId(), residentId)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "INVALID_RESPONDENT",
                    "Người trả lời phải là người thân có quan hệ Hiệu lực với người cao tuổi");
        }
        for (WishOpinion opinion : opinionsOf(command)) {
            opinionSources.validate(residentId, opinion.source());
            if (opinion.content() == null || opinion.content().isBlank()) {
                throw unprocessable("OPINION_CONTENT_REQUIRED", "Ý kiến khác cần có nội dung");
            }
        }
    }

    /** FR-023c: người cao tuổi ký khi tự quyết được; ngược lại người đại diện của người bệnh hoặc theo pháp luật. */
    private void validateSigner(long residentId, RecordEndOfLifeWish command, boolean canSelfDetermine) {
        WishSignerType signer = command.signerType();
        if (signer == null) {
            throw invalidSigner("Thiếu người ký");
        }
        CourtDecisionRef court = command.courtDecision();
        if (canSelfDetermine) {
            if (signer != WishSignerType.RESIDENT || command.signerFamilyMemberId() != null) {
                throw invalidSigner("Bác sĩ nhận định người cao tuổi tự quyết được thì chính người cao tuổi ký");
            }
        } else {
            if (signer == WishSignerType.RESIDENT) {
                throw invalidSigner(
                        "Bác sĩ nhận định người cao tuổi không tự quyết được thì người đại diện của người bệnh ký");
            }
            if (!isRelated(command.signerFamilyMemberId(), residentId)) {
                throw invalidSigner("Người ký thay phải là người thân có quan hệ Hiệu lực với người cao tuổi");
            }
        }
        if (signer == WishSignerType.PATIENT_REPRESENTATIVE && command.representativeSelection() == null) {
            throw unprocessable(
                    "REPRESENTATIVE_SELECTION_REQUIRED", "Cần ghi người đại diện được chọn theo cách nào (FR-023c)");
        }
        if (signer != WishSignerType.PATIENT_REPRESENTATIVE && command.representativeSelection() != null) {
            throw unprocessable(
                    "REPRESENTATIVE_SELECTION_NOT_ALLOWED",
                    "Cách chọn người đại diện chỉ dùng cho người đại diện của người bệnh");
        }
        if (signer == WishSignerType.LEGAL_REPRESENTATIVE) {
            if (court == null
                    || isBlank(court.number())
                    || court.date() == null
                    || isBlank(court.court())
                    || court.fileId() == null) {
                throw unprocessable(
                        "COURT_DECISION_REQUIRED",
                        "Người đại diện theo pháp luật cần quyết định của Tòa án: số, ngày, Tòa án và bản scan (FR-011)");
            }
        } else if (court != null) {
            throw unprocessable(
                    "COURT_DECISION_NOT_ALLOWED", "Quyết định của Tòa án chỉ dùng cho người đại diện theo pháp luật");
        }
    }

    private boolean isRelated(Long familyMemberId, long residentId) {
        FamilyRelationshipApi api = relationships.getIfAvailable();
        return familyMemberId != null && api != null && api.belongsToResident(familyMemberId, residentId);
    }

    private static List<WishOpinion> opinionsOf(RecordEndOfLifeWish command) {
        return command.otherOpinions() == null ? List.of() : command.otherOpinions();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static BusinessException invalidSigner(String message) {
        return new BusinessException(HttpStatus.CONFLICT, "INVALID_SIGNER", message);
    }

    private static BusinessException unprocessable(String code, String message) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, code, message);
    }

    private static Map<String, Object> snapshot(EndOfLifeWish wish) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("choice", wish.choice().name());
        map.put("firstContact", wish.firstContact());
        map.put("note", wish.note());
        map.put("respondentType", wish.respondentType().name());
        map.put("respondentFamilyMemberId", wish.respondentFamilyMemberId());
        map.put("relationship", wish.relationship());
        map.put("assessingDoctorId", wish.assessingDoctorId());
        map.put("canSelfDetermine", wish.canSelfDetermine());
        map.put("supportMethod", wish.supportMethod());
        map.put("reviewDate", String.valueOf(wish.reviewDate()));
        map.put("signerType", wish.signerType().name());
        map.put("signerFamilyMemberId", wish.signerFamilyMemberId());
        map.put("representativeSelection", wish.representativeSelection());
        if (wish.courtDecision() != null) {
            map.put("courtDecisionNumber", wish.courtDecision().number());
            map.put("courtDecisionDate", String.valueOf(wish.courtDecision().date()));
            map.put("courtDecisionCourt", wish.courtDecision().court());
            map.put("courtDecisionFileId", wish.courtDecision().fileId());
        }
        map.put("otherOpinionCount", wish.otherOpinions().size());
        map.put("signedAt", wish.signedAt().toString());
        map.put("signedCopyFileId", wish.signedCopyFileId());
        map.put("source", wish.source().name());
        return map;
    }
}
