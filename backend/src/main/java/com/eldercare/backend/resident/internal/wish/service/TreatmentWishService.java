package com.eldercare.backend.resident.internal.wish.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipApi;
import com.eldercare.backend.administration.auditlog.api.AuditAction;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.AuditLogEntry;
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
import com.eldercare.backend.resident.internal.profile.persistence.ResidentRepository;
import com.eldercare.backend.resident.internal.profile.service.ResidentProfileService;
import com.eldercare.backend.resident.internal.status.service.ProfileOpenGuard;
import com.eldercare.backend.resident.internal.wish.domain.TreatmentWish;
import com.eldercare.backend.resident.internal.wish.persistence.TreatmentWishStore;
import com.eldercare.backend.resident.internal.wish.persistence.TreatmentWishStore.NewWish;
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
    public Target targetOf(long wishId) {
        return ResidentProfileService.target(store.residentIdOf(wishId).orElseThrow(NotFoundException::new));
    }

    @RequiresPermission(function = Function.ALLERGY_CONDITION, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public Summary get(Target target) {
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
        long residentId = target.residentId();
        residents.lockById(residentId).orElseThrow(NotFoundException::new);
        guard.requireOpen(residentId);
        List<Long> notify = new ArrayList<>(new LinkedHashSet<>(
                command.notifyFamilyMemberIds() == null ? List.of() : command.notifyFamilyMemberIds()));
        List<WishOpinion> opinions = command.otherOpinions() == null ? List.of() : command.otherOpinions();
        validate(residentId, command, notify, opinions);
        long accountId = currentUser.getObject().accountId();
        Instant now = time.now();

        Optional<TreatmentWish> previous = store.findActive(residentId);
        previous.ifPresent(old -> store.expire(old.id()));
        long id = store.insert(
                new NewWish(
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
                        accountId,
                        now),
                opinions);
        previous.ifPresent(old -> store.supersededBy(old.id(), id));
        if (command.signedCopyFileId() != null) {
            files.attach(command.signedCopyFileId(), ENTITY_TYPE, id);
        }
        TreatmentWish saved = store.findActive(residentId).orElseThrow();

        auditLog.write(AuditLogEntry.builder()
                .action(RECORDED)
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
        FamilyRelationshipApi family = relationships.getIfAvailable();
        return wish.notifyFamilyMemberIds().stream()
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
