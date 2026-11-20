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
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.OpinionSourceApi;
import com.eldercare.backend.resident.api.dto.HealthInsurance;
import com.eldercare.backend.resident.api.dto.NotifyContact;
import com.eldercare.backend.resident.api.dto.OpinionSource;
import com.eldercare.backend.resident.api.dto.PreferredHospital;
import com.eldercare.backend.resident.api.dto.TreatmentEscort;
import com.eldercare.backend.resident.api.dto.TreatmentWishView;
import com.eldercare.backend.resident.api.dto.Warning;
import com.eldercare.backend.resident.api.dto.WishOpinion;
import com.eldercare.backend.resident.internal.health.service.ActorNames;
import com.eldercare.backend.resident.internal.health.service.SelfReportedHealth;
import com.eldercare.backend.resident.internal.profile.persistence.ResidentRepository;
import com.eldercare.backend.resident.internal.profile.service.ResidentProfileService;
import com.eldercare.backend.resident.internal.status.service.ProfileOpenGuard;
import com.eldercare.backend.resident.internal.wish.domain.NewTreatmentWish;
import com.eldercare.backend.resident.internal.wish.domain.TreatmentWish;
import com.eldercare.backend.resident.internal.wish.domain.TreatmentWishDraft;
import com.eldercare.backend.resident.internal.wish.persistence.TreatmentWishStore;
import com.eldercare.backend.resident.internal.wish.persistence.WishDraftStore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phiếu nguyện vọng khi điều trị (001 US7; FR-023j → FR-023m, BR-M01-16, DBR-39; research H-18). Cùng mẫu phiên bản với
 * phiếu cuối đời nhưng bản cũ chuyển Hết hiệu lực, không có nhận định "tự quyết được" và không nhắc khi thiếu
 * (FR-023l). Quyền theo dòng "Dị ứng, bệnh nền" của Phụ lục 27 (FR-023m). Cung cấp {@link #currentView} cho 007.
 */
@Service
public class TreatmentWishService {

    public static final String ENTITY_TYPE = "M01.TREATMENT_WISH";
    public static final String NO_FAMILY_MEMBER_TO_NOTIFY = "NO_FAMILY_MEMBER_TO_NOTIFY";

    static final AuditAction RECORDED = new AuditAction("TREATMENT_WISH_RECORDED", false);
    static final AuditAction EXPIRED = new AuditAction("TREATMENT_WISH_EXPIRED", false);
    static final AuditAction DRAFT_VERIFIED = new AuditAction("WISH_DRAFT_VERIFIED", false);

    private static final int MAX_HOSPITALS = 2;

    public record RecordCommand(
            List<PreferredHospital> preferredHospitals,
            HealthInsurance healthInsurance,
            boolean askFamilyFirst,
            List<Long> notifyFamilyMemberIds,
            TreatmentEscort accompaniedBy,
            String note,
            OpinionSource respondent,
            List<WishOpinion> otherOpinions,
            Long signedCopyFileId) {}

    public record Result(TreatmentWish wish, List<Warning> warnings) {}

    public record Summary(Optional<TreatmentWish> active, List<TreatmentWish> history) {}

    private final TreatmentWishStore store;
    private final WishDraftStore drafts;
    private final ResidentRepository residents;
    private final ProfileOpenGuard guard;
    private final FileApi files;
    private final AuditLogApi auditLog;
    private final BusinessTime time;
    private final OpinionSourceApi opinionSources;
    private final ActorNames names;
    private final ObjectProvider<FamilyRelationshipApi> relationships;
    private final ObjectProvider<CurrentUser> currentUser;

    TreatmentWishService(
            TreatmentWishStore store,
            WishDraftStore drafts,
            ResidentRepository residents,
            ProfileOpenGuard guard,
            FileApi files,
            AuditLogApi auditLog,
            BusinessTime time,
            OpinionSourceApi opinionSources,
            ActorNames names,
            ObjectProvider<FamilyRelationshipApi> relationships,
            ObjectProvider<CurrentUser> currentUser) {
        this.store = store;
        this.drafts = drafts;
        this.residents = residents;
        this.guard = guard;
        this.files = files;
        this.auditLog = auditLog;
        this.time = time;
        this.opinionSources = opinionSources;
        this.names = names;
        this.relationships = relationships;
        this.currentUser = currentUser;
    }

    /** Đối tượng kiểm quyền của phiếu: hồ sơ người cao tuổi mà nó thuộc về (quyền tải bản ký). */
    @Transactional(readOnly = true)
    /** Ô T⁵⁰ của Hành chính chỉ là tự khai về sức khỏe (Q-400); phiếu nguyện vọng khi điều trị chỉ cho Bác sĩ, Điều dưỡng. */
    private void requireNotOfficeOnly() {
        if (SelfReportedHealth.isOfficeOnly(currentUser.getObject())) {
            throw SelfReportedHealth.deniedForOffice(
                    "Hành chính chỉ biết đã có hay chưa có phiếu nguyện vọng khi điều trị");
        }
    }

    /** Tệp gắn vào phiếu: Hành chính chỉ tải tệp của bản Nháp do chính mình lập (Q-412, FR-023q). */
    @Transactional(readOnly = true)
    public boolean officeMayDownload(long wishId, CurrentUser user) {
        return !SelfReportedHealth.isOfficeOnly(user)
                || drafts.draftWriterOf(wishId)
                        .filter(w -> w == user.accountId())
                        .isPresent();
    }

    public Target targetOf(long wishId) {
        return ResidentProfileService.target(store.residentIdOf(wishId).orElseThrow(NotFoundException::new));
    }

    @RequiresPermission(function = Function.ALLERGY_CONDITION, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public Summary get(Target target) {
        requireNotOfficeOnly();
        return new Summary(store.findActive(target.residentId()), store.listByResident(target.residentId()));
    }

    /** Cờ "đã có phiếu Hiệu lực" cho hồ sơ (FR-023m); người gọi tự quyết định ai được thấy cờ. */
    @Transactional(readOnly = true)
    public boolean hasActive(long residentId) {
        return store.existsActive(residentId);
    }

    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.ALLERGY_CONDITION, operation = Operation.CREATE)
    @Transactional
    public Result record(Target target, RecordCommand command) {
        requireNotOfficeOnly();
        long residentId = target.residentId();
        residents.lockById(residentId).orElseThrow(NotFoundException::new);
        guard.requireOpen(residentId);
        if (drafts.findTreatment(residentId).isPresent()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "DRAFT_ALREADY_EXISTS",
                    "Còn bản Nháp tự khai: kiểm chứng hoặc Hủy nháp trước khi ghi phiên bản mới (FR-023p)");
        }
        return write(residentId, command, null);
    }

    /**
     * Kiểm chứng bản Nháp tự khai do Hành chính lập (Q-412, FR-023p): lệnh mang đủ nội dung phiếu, cùng kiểm tra như ghi
     * phiên bản; dòng Nháp thành phiên bản Hiệu lực (giữ id, người lập), phiên bản Hiệu lực trước Hết hiệu lực.
     */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.ALLERGY_CONDITION, operation = Operation.CREATE)
    @Transactional
    public Result verifyDraft(Target target, RecordCommand command) {
        requireNotOfficeOnly();
        long residentId = target.residentId();
        residents.lockById(residentId).orElseThrow(NotFoundException::new);
        guard.requireOpen(residentId);
        TreatmentWishDraft draft = drafts.findTreatment(residentId).orElseThrow(WishDraftService::noDraft);
        return write(residentId, command, draft);
    }

    /** Phần chung của ghi thẳng và kiểm chứng; người gọi đã khóa hồ sơ và kiểm hồ sơ chưa ở trạng thái cuối. */
    private Result write(long residentId, RecordCommand command, TreatmentWishDraft draft) {
        List<Long> notify = new ArrayList<>(new LinkedHashSet<>(
                command.notifyFamilyMemberIds() == null ? List.of() : command.notifyFamilyMemberIds()));
        List<WishOpinion> opinions = command.otherOpinions() == null ? List.of() : command.otherOpinions();
        validate(residentId, command, notify, opinions);
        long accountId = currentUser.getObject().accountId();
        Instant now = time.now();

        Optional<TreatmentWish> previous = store.findActive(residentId);
        previous.ifPresent(old -> store.expire(old.id()));
        NewTreatmentWish version = new NewTreatmentWish(
                residentId,
                command.preferredHospitals().stream()
                        .map(h -> new PreferredHospital(
                                h.name().strip(), h.address().strip()))
                        .toList(),
                command.healthInsurance(),
                command.askFamilyFirst(),
                notify,
                command.accompaniedBy(),
                command.note() == null || command.note().isBlank()
                        ? null
                        : command.note().strip(),
                command.respondent(),
                command.signedCopyFileId(),
                draft == null ? accountId : draft.recordedBy(),
                draft == null ? now : draft.recordedAt());
        long id;
        if (draft == null) {
            id = store.insert(version, opinions);
        } else {
            id = draft.id();
            store.activateDraft(id, version, opinions, accountId, now);
        }
        previous.ifPresent(old -> store.supersededBy(old.id(), id));
        if (command.signedCopyFileId() != null
                && files.attachedTo(ENTITY_TYPE, id).stream().noneMatch(f -> f.id() == command.signedCopyFileId())) {
            files.attach(command.signedCopyFileId(), ENTITY_TYPE, id);
        }
        TreatmentWish saved = store.findActive(residentId).orElseThrow();

        auditLog.write(AuditLogEntry.builder()
                .action(draft == null ? RECORDED : DRAFT_VERIFIED)
                .entity(ENTITY_TYPE, id)
                .resident(residentId)
                .after(snapshot(saved))
                .build());
        previous.ifPresent(old -> auditLog.write(AuditLogEntry.builder()
                .action(EXPIRED)
                .entity(ENTITY_TYPE, old.id())
                .resident(residentId)
                .before(Map.of("status", "ACTIVE"))
                .after(Map.of("status", "EXPIRED", "supersededById", id))
                .build()));
        List<Warning> warnings = command.askFamilyFirst() && notify.isEmpty()
                ? List.of(new Warning(
                        NO_FAMILY_MEMBER_TO_NOTIFY,
                        "Phiếu yêu cầu hỏi gia đình trước nhưng chưa chọn người thân cần báo (FR-023j)"))
                : List.of();
        return new Result(saved, warnings);
    }

    /** Phiếu Hiệu lực cho 007: thẻ khẩn cấp, lệnh Chuyển viện, phiếu tóm tắt chuyển viện (FR-023, SC-014). */
    @Transactional(readOnly = true)
    public Optional<TreatmentWishView> currentView(long residentId) {
        return store.findActive(residentId)
                .map(w -> new TreatmentWishView(
                        w.id(),
                        w.residentId(),
                        w.preferredHospitals(),
                        Optional.ofNullable(w.healthInsurance()),
                        w.askFamilyFirst(),
                        notifyContacts(w),
                        w.accompaniedBy(),
                        w.note(),
                        w.recordedAt()));
    }

    /** Người cần báo kèm họ tên và số điện thoại lấy qua 012 (chưa có bản cài của 012 thì để trống). */
    public List<NotifyContact> notifyContacts(TreatmentWish wish) {
        return notifyContacts(wish.notifyFamilyMemberIds());
    }

    /** Người cần báo của bản Nháp (Q-412). */
    public List<NotifyContact> notifyContacts(List<Long> familyMemberIds) {
        FamilyRelationshipApi family = relationships.getIfAvailable();
        return familyMemberIds.stream()
                .map(memberId -> new NotifyContact(
                        memberId,
                        family == null
                                ? null
                                : family.familyMemberName(memberId).orElse(null),
                        family == null
                                ? null
                                : family.registeredPhoneNumber(memberId).orElse(null)))
                .toList();
    }

    /** Mã quan hệ của người cần báo với người cao tuổi (FR-023l); 012 chưa có bản cài hoặc không có quan hệ thì trống. */
    public String relationshipOf(TreatmentWish wish, long familyMemberId) {
        return relationshipOf(wish.residentId(), familyMemberId);
    }

    public String relationshipOf(long residentId, long familyMemberId) {
        FamilyRelationshipApi family = relationships.getIfAvailable();
        return family == null
                ? null
                : family.relationship(familyMemberId, residentId)
                        .map(Enum::name)
                        .orElse(null);
    }

    /** Họ tên người ghi để hiện. */
    public String actorName(long accountId) {
        return names.of(accountId);
    }

    private void validate(long residentId, RecordCommand command, List<Long> notify, List<WishOpinion> opinions) {
        List<PreferredHospital> hospitals = command.preferredHospitals();
        if (hospitals == null
                || hospitals.isEmpty()
                || hospitals.stream().anyMatch(h -> h == null || isBlank(h.name()) || isBlank(h.address()))) {
            throw unprocessable(
                    "PREFERRED_HOSPITAL_REQUIRED", "Cần ít nhất một bệnh viện ưu tiên, đủ tên và địa chỉ (FR-023j)");
        }
        if (hospitals.size() > MAX_HOSPITALS) {
            throw unprocessable("TOO_MANY_PREFERRED_HOSPITALS", "Tối đa hai bệnh viện ưu tiên (FR-023j)");
        }
        if (command.accompaniedBy() == null || command.respondent() == null) {
            throw unprocessable("INCOMPLETE_TREATMENT_WISH", "Cần ghi người đi kèm và người trả lời");
        }
        opinionSources.validate(residentId, command.respondent());
        for (WishOpinion opinion : opinions) {
            opinionSources.validate(residentId, opinion.source());
            if (isBlank(opinion.content())) {
                throw unprocessable("OPINION_CONTENT_REQUIRED", "Ý kiến khác cần có nội dung");
            }
        }
        FamilyRelationshipApi family = relationships.getIfAvailable();
        for (Long memberId : notify) {
            if (memberId == null || family == null || !family.belongsToResident(memberId, residentId)) {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "INVALID_FAMILY_MEMBER",
                        "Người cần báo phải là người thân có quan hệ Hiệu lực với người cao tuổi");
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static BusinessException unprocessable(String code, String message) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, code, message);
    }

    private static Map<String, Object> snapshot(TreatmentWish wish) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("preferredHospitals", wish.preferredHospitals());
        if (wish.healthInsurance() != null) {
            map.put("insuranceRegistrationPlace", wish.healthInsurance().initialRegistrationPlace());
            map.put("insuranceCardNumber", wish.healthInsurance().cardNumber());
            map.put("insuranceCardExpiry", String.valueOf(wish.healthInsurance().cardExpiry()));
        }
        map.put("askFamilyFirst", wish.askFamilyFirst());
        map.put("notifyFamilyMemberIds", wish.notifyFamilyMemberIds());
        map.put("accompaniedBy", wish.accompaniedBy().name());
        map.put("note", wish.note());
        map.put("respondentType", wish.respondent().type().name());
        map.put("respondentFamilyMemberId", wish.respondent().familyMemberId());
        map.put("otherOpinionCount", wish.otherOpinions().size());
        map.put("signedCopyFileId", wish.signedCopyFileId());
        return map;
    }
}
