package com.eldercare.backend.resident.internal.wish.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipApi;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.OpinionSourceApi;
import com.eldercare.backend.resident.api.dto.PreferredHospital;
import com.eldercare.backend.resident.api.dto.WishOpinion;
import com.eldercare.backend.resident.api.dto.WishSignerType;
import com.eldercare.backend.resident.internal.health.service.SelfReportedHealth;
import com.eldercare.backend.resident.internal.profile.persistence.ResidentRepository;
import com.eldercare.backend.resident.internal.status.service.ProfileOpenGuard;
import com.eldercare.backend.resident.internal.wish.domain.EndOfLifeDraftContent;
import com.eldercare.backend.resident.internal.wish.domain.EndOfLifeWishDraft;
import com.eldercare.backend.resident.internal.wish.domain.TreatmentDraftContent;
import com.eldercare.backend.resident.internal.wish.domain.TreatmentWishDraft;
import com.eldercare.backend.resident.internal.wish.persistence.WishDraftStore;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bản Nháp tự khai của phiếu nguyện vọng do Hành chính lập (001 T111; Q-412, BR-M01-18; FR-023n → FR-023q). Hành chính
 * lập, sửa Nháp khi người cao tuổi còn Đang tiếp nhận (chú thích ⁵², ⁵⁰ kiểm ở 002), chỉ xem, sửa Nháp mình lập; Bác sĩ,
 * Điều dưỡng xem Nháp, Hủy nháp kèm lý do, kiểm chứng ở {@link EndOfLifeWishService#verifyDraft},
 * {@link TreatmentWishService#verifyDraft}. Nháp không có hiệu lực: các đường đọc phiếu chỉ đọc phiên bản đã có hiệu lực.
 */
@Service
public class WishDraftService {

    static final AuditAction CREATED = new AuditAction("WISH_DRAFT_CREATED", false);
    static final AuditAction EDITED = new AuditAction("WISH_DRAFT_EDITED", false);
    static final AuditAction DISCARDED = new AuditAction("WISH_DRAFT_DISCARDED", false);

    private static final int MAX_HOSPITALS = 2;

    /** Nháp sau lệnh lập, sửa; {@code created} cho biết lệnh đã tạo Nháp mới (201) hay sửa Nháp có sẵn (200). */
    public record Saved<T>(T draft, boolean created) {}

    private final WishDraftStore drafts;
    private final ResidentRepository residents;
    private final ProfileOpenGuard guard;
    private final FileApi files;
    private final AuditLogApi auditLog;
    private final BusinessTime time;
    private final OpinionSourceApi opinionSources;
    private final ObjectProvider<FamilyRelationshipApi> relationships;
    private final ObjectProvider<CurrentUser> currentUser;

    WishDraftService(
            WishDraftStore drafts,
            ResidentRepository residents,
            ProfileOpenGuard guard,
            FileApi files,
            AuditLogApi auditLog,
            BusinessTime time,
            OpinionSourceApi opinionSources,
            ObjectProvider<FamilyRelationshipApi> relationships,
            ObjectProvider<CurrentUser> currentUser) {
        this.drafts = drafts;
        this.residents = residents;
        this.guard = guard;
        this.files = files;
        this.auditLog = auditLog;
        this.time = time;
        this.opinionSources = opinionSources;
        this.relationships = relationships;
        this.currentUser = currentUser;
    }

    static BusinessException noDraft() {
        return new BusinessException(HttpStatus.NOT_FOUND, "NO_DRAFT", "Không có bản Nháp tự khai");
    }

    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.END_OF_LIFE_WISHES, operation = Operation.CREATE)
    @Transactional
    public Saved<EndOfLifeWishDraft> saveEndOfLife(Target target, EndOfLifeDraftContent content) {
        long residentId = lockForOffice(target);
        guard.requireOpen(residentId);
        requireAdmitting(residentId);
        validateEndOfLife(residentId, content);
        Optional<EndOfLifeWishDraft> existing = drafts.findEndOfLife(residentId);
        long id;
        if (existing.isPresent()) {
            requireWriter(existing.get().recordedBy());
            id = existing.get().id();
            drafts.updateEndOfLife(id, content);
        } else {
            id = drafts.insertEndOfLife(residentId, content, me().accountId(), time.now());
        }
        attachOnce(content.signedCopyFileId(), EndOfLifeWishService.ENTITY_TYPE, id);
        if (content.courtDecision() != null) {
            attachOnce(content.courtDecision().fileId(), EndOfLifeWishService.ENTITY_TYPE, id);
        }
        audit(
                existing.isPresent() ? EDITED : CREATED,
                EndOfLifeWishService.ENTITY_TYPE,
                id,
                residentId,
                snapshot(content));
        return new Saved<>(drafts.findEndOfLifeById(id).orElseThrow(), existing.isEmpty());
    }

    @RequiresPermission(function = Function.END_OF_LIFE_WISHES, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public EndOfLifeWishDraft endOfLife(Target target) {
        EndOfLifeWishDraft draft = drafts.findEndOfLife(target.residentId()).orElseThrow(WishDraftService::noDraft);
        requireViewer(target.residentId(), draft.recordedBy());
        return draft;
    }

    @BusinessCommand
    @RequiresPermission(function = Function.END_OF_LIFE_WISHES, operation = Operation.UPDATE)
    @Transactional
    public EndOfLifeWishDraft discardEndOfLife(Target target, Reason reason) {
        requireClinician();
        long residentId = target.residentId();
        residents.lockById(residentId).orElseThrow(NotFoundException::new);
        guard.requireOpen(residentId);
        EndOfLifeWishDraft draft = drafts.findEndOfLife(residentId).orElseThrow(WishDraftService::noDraft);
        discard(draft.id(), EndOfLifeWishService.ENTITY_TYPE, residentId, reason);
        return drafts.findEndOfLifeById(draft.id()).orElseThrow();
    }

    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.ALLERGY_CONDITION, operation = Operation.CREATE)
    @Transactional
    public Saved<TreatmentWishDraft> saveTreatment(Target target, TreatmentDraftContent content) {
        long residentId = lockForOffice(target);
        guard.requireOpen(residentId);
        requireAdmitting(residentId);
        validateTreatment(residentId, content);
        Optional<TreatmentWishDraft> existing = drafts.findTreatment(residentId);
        long id;
        if (existing.isPresent()) {
            requireWriter(existing.get().recordedBy());
            id = existing.get().id();
            drafts.updateTreatment(id, content);
        } else {
            id = drafts.insertTreatment(residentId, content, me().accountId(), time.now());
        }
        attachOnce(content.signedCopyFileId(), TreatmentWishService.ENTITY_TYPE, id);
        audit(
                existing.isPresent() ? EDITED : CREATED,
                TreatmentWishService.ENTITY_TYPE,
                id,
                residentId,
                snapshot(content));
        return new Saved<>(drafts.findTreatmentById(id).orElseThrow(), existing.isEmpty());
    }

    @RequiresPermission(function = Function.ALLERGY_CONDITION, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public TreatmentWishDraft treatment(Target target) {
        TreatmentWishDraft draft = drafts.findTreatment(target.residentId()).orElseThrow(WishDraftService::noDraft);
        requireViewer(target.residentId(), draft.recordedBy());
        return draft;
    }

    @BusinessCommand
    @RequiresPermission(function = Function.ALLERGY_CONDITION, operation = Operation.UPDATE)
    @Transactional
    public TreatmentWishDraft discardTreatment(Target target, Reason reason) {
        requireClinician();
        long residentId = target.residentId();
        residents.lockById(residentId).orElseThrow(NotFoundException::new);
        guard.requireOpen(residentId);
        TreatmentWishDraft draft = drafts.findTreatment(residentId).orElseThrow(WishDraftService::noDraft);
        discard(draft.id(), TreatmentWishService.ENTITY_TYPE, residentId, reason);
        return drafts.findTreatmentById(draft.id()).orElseThrow();
    }

    /** Loại phiếu còn Nháp chưa kiểm chứng ({@code END_OF_LIFE}, {@code TREATMENT}); cảnh báo, nhắc FR-023r. */
    @Transactional(readOnly = true)
    public List<String> openDraftKinds(long residentId) {
        return drafts.openDraftKinds(residentId);
    }

    private long lockForOffice(Target target) {
        if (!SelfReportedHealth.isOfficeOnly(me())) {
            throw SelfReportedHealth.deniedForOffice(
                    "Bản Nháp tự khai do Hành chính lập; Bác sĩ, Điều dưỡng ghi phiên bản hoặc kiểm chứng Nháp");
        }
        long residentId = target.residentId();
        residents.lockById(residentId).orElseThrow(NotFoundException::new);
        return residentId;
    }

    private void requireWriter(long writer) {
        if (writer != me().accountId()) {
            throw SelfReportedHealth.deniedForOffice("Hành chính chỉ sửa bản Nháp do chính mình lập (FR-023q)");
        }
    }

    /** Hành chính chỉ xem Nháp mình lập; ngoài Hành chính chỉ Bác sĩ, Điều dưỡng xem Nháp (FR-023q). */
    private void requireViewer(long residentId, long writer) {
        CurrentUser user = me();
        if (SelfReportedHealth.isOfficeOnly(user)) {
            requireAdmitting(residentId);
            requireWriter(writer);
        } else if (!user.roles().contains(Role.DOCTOR) && !user.roles().contains(Role.NURSING_STAFF)) {
            throw SelfReportedHealth.deniedForOffice("Chỉ Bác sĩ, Điều dưỡng xem bản Nháp tự khai (FR-023q)");
        }
    }

    /**
     * Hành chính chỉ khi người cao tuổi còn Đang tiếp nhận (BR-M01-18). Kiểm ở dịch vụ vì phiếu khi điều trị theo dòng
     * "Dị ứng, bệnh nền" của Phụ lục 27 (chú thích 50), dòng đó chưa kiểm điều kiện ở lớp vai trò.
     */
    private void requireAdmitting(long residentId) {
        if (!guard.isAdmitting(residentId)) {
            throw SelfReportedHealth.deniedForOffice(
                    "Hành chính chỉ lập, xem bản Nháp tự khai khi người cao tuổi còn Đang tiếp nhận (BR-M01-18)");
        }
    }

    private void requireClinician() {
        if (SelfReportedHealth.isOfficeOnly(me())) {
            throw SelfReportedHealth.deniedForOffice("Hành chính không kiểm chứng, không Hủy nháp (FR-023p)");
        }
    }

    private void discard(long id, String entityType, long residentId, Reason reason) {
        drafts.discard(id, me().accountId(), time.now(), reason.value());
        audit(DISCARDED, entityType, id, residentId, Map.of("status", "DISCARDED", "reason", reason.value()));
    }

    private void validateEndOfLife(long residentId, EndOfLifeDraftContent content) {
        WishSignerType respondent = content.respondentType();
        if (respondent == null) {
            throw unprocessable("RESPONDENT_REQUIRED", "Cần ghi người trả lời");
        }
        boolean hasMember = content.respondentFamilyMemberId() != null
                || (content.relationship() != null && !content.relationship().isBlank());
        if (respondent == WishSignerType.RESIDENT && hasMember) {
            throw unprocessable("RESPONDENT_REQUIRED", "Người cao tuổi tự trả lời thì không ghi người thân, quan hệ");
        }
        if (respondent != WishSignerType.RESIDENT) {
            if (content.respondentFamilyMemberId() == null
                    || content.relationship() == null
                    || content.relationship().isBlank()) {
                throw unprocessable(
                        "RESPONDENT_REQUIRED", "Người trả lời là người đại diện cần có hồ sơ người thân và quan hệ");
            }
            if (!isRelated(content.respondentFamilyMemberId(), residentId)) {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "INVALID_RESPONDENT",
                        "Người trả lời phải là người thân có quan hệ Hiệu lực với người cao tuổi");
            }
        }
        if (content.signerFamilyMemberId() != null && !isRelated(content.signerFamilyMemberId(), residentId)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "INVALID_SIGNER",
                    "Người ký thay phải là người thân có quan hệ Hiệu lực với người cao tuổi");
        }
        validateOpinions(residentId, content.otherOpinions());
    }

    private void validateTreatment(long residentId, TreatmentDraftContent content) {
        if (content.respondent() == null) {
            throw unprocessable("RESPONDENT_REQUIRED", "Cần ghi người trả lời");
        }
        opinionSources.validate(residentId, content.respondent());
        List<PreferredHospital> hospitals =
                content.preferredHospitals() == null ? List.of() : content.preferredHospitals();
        if (hospitals.size() > MAX_HOSPITALS) {
            throw unprocessable("TOO_MANY_PREFERRED_HOSPITALS", "Tối đa hai bệnh viện ưu tiên (FR-023j)");
        }
        FamilyRelationshipApi family = relationships.getIfAvailable();
        for (Long memberId :
                content.notifyFamilyMemberIds() == null ? List.<Long>of() : content.notifyFamilyMemberIds()) {
            if (memberId == null || family == null || !family.belongsToResident(memberId, residentId)) {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "INVALID_FAMILY_MEMBER",
                        "Người cần báo phải là người thân có quan hệ Hiệu lực với người cao tuổi");
            }
        }
        validateOpinions(residentId, content.otherOpinions());
    }

    private void validateOpinions(long residentId, List<WishOpinion> opinions) {
        for (WishOpinion opinion : opinions == null ? List.<WishOpinion>of() : opinions) {
            opinionSources.validate(residentId, opinion.source());
            if (opinion.content() == null || opinion.content().isBlank()) {
                throw unprocessable("OPINION_CONTENT_REQUIRED", "Ý kiến khác cần có nội dung");
            }
        }
    }

    private boolean isRelated(Long familyMemberId, long residentId) {
        FamilyRelationshipApi api = relationships.getIfAvailable();
        return familyMemberId != null && api != null && api.belongsToResident(familyMemberId, residentId);
    }

    private void attachOnce(Long fileId, String entityType, long wishId) {
        if (fileId != null && files.attachedTo(entityType, wishId).stream().noneMatch(f -> f.id() == fileId)) {
            files.attach(fileId, entityType, wishId);
        }
    }

    private void audit(AuditAction action, String entityType, long id, long residentId, Map<String, Object> after) {
        auditLog.write(AuditLogEntry.builder()
                .action(action)
                .entity(entityType, id)
                .resident(residentId)
                .after(after)
                .build());
    }

    private CurrentUser me() {
        return currentUser.getObject();
    }

    private static BusinessException unprocessable(String code, String message) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, code, message);
    }

    private static Map<String, Object> snapshot(EndOfLifeDraftContent c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "DRAFT");
        map.put("choice", c.choice() == null ? null : c.choice().name());
        map.put("firstContact", c.firstContact());
        map.put("note", c.note());
        map.put("respondentType", c.respondentType().name());
        map.put("respondentFamilyMemberId", c.respondentFamilyMemberId());
        map.put("signerType", c.signerType() == null ? null : c.signerType().name());
        map.put(
                "otherOpinionCount",
                c.otherOpinions() == null ? 0 : c.otherOpinions().size());
        map.put("signedCopyFileId", c.signedCopyFileId());
        return map;
    }

    private static Map<String, Object> snapshot(TreatmentDraftContent c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "DRAFT");
        map.put("preferredHospitals", c.preferredHospitals());
        map.put("askFamilyFirst", c.askFamilyFirst());
        map.put("notifyFamilyMemberIds", c.notifyFamilyMemberIds());
        map.put(
                "accompaniedBy",
                c.accompaniedBy() == null ? null : c.accompaniedBy().name());
        map.put("note", c.note());
        map.put("respondentType", c.respondent().type().name());
        map.put(
                "otherOpinionCount",
                c.otherOpinions() == null ? 0 : c.otherOpinions().size());
        map.put("signedCopyFileId", c.signedCopyFileId());
        return map;
    }
}
