package com.eldercare.backend.belongings.internal.consent.service;

import static com.eldercare.backend.belongings.internal.belonging.domain.BelongingActions.SELF_CUSTODY_CONSENT_RECORDED;
import static com.eldercare.backend.belongings.internal.belonging.domain.BelongingActions.SELF_CUSTODY_CONSENT_WITHDRAWN;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.api.dto.BelongingView;
import com.eldercare.backend.belongings.internal.belonging.domain.BelongingNotices;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingResidentLock;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingStore;
import com.eldercare.backend.belongings.internal.catalog.domain.BelongingCatalogs;
import com.eldercare.backend.belongings.internal.consent.domain.SelfCustodyConsent;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.api.FamilyRightsApi;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đồng ý cho tự giữ ({@code DongYTuGiuService}; 013 T010; FR-005, FR-015a, FR-017b, BR-M12-06, Q-155): người đại diện
 * ghi, rút qua cổng; Hành chính ghi, rút theo bản ký scan; ký trong lệnh Tiếp nhận và giao ngay. Người ký phải là người
 * đại diện tại thời điểm ký ({@code FamilyRightsApi.isRepresentative}, research K-06).
 */
@Service
public class SelfCustodyConsentService {

    static final String SIGNED_FORM_OWNER = "M12.SIGNED_FORM";
    private static final String BELONGING_ENTITY = "M12.BELONGING";

    private final SimpleRecordApi records;
    private final BelongingStore belongings;
    private final BelongingResidentLock residentLock;
    private final CatalogApi catalogs;
    private final FamilyRightsApi family;
    private final FileApi files;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> users;

    SelfCustodyConsentService(
            SimpleRecordApi records,
            BelongingStore belongings,
            BelongingResidentLock residentLock,
            CatalogApi catalogs,
            FamilyRightsApi family,
            FileApi files,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            BusinessTime time,
            ObjectProvider<CurrentUser> users) {
        this.records = records;
        this.belongings = belongings;
        this.residentLock = residentLock;
        this.catalogs = catalogs;
        this.family = family;
        this.files = files;
        this.audit = audit;
        this.events = events;
        this.time = time;
        this.users = users;
    }

    /** Ký trong lệnh Tiếp nhận và giao ngay (FR-005); lệnh gọi đã kiểm người đại diện và khóa người cao tuổi. */
    @Transactional(propagation = Propagation.MANDATORY)
    public long recordInIntake(long residentId, long belongingId, long representativeId, long signedFormFileId) {
        return create(
                residentId, belongingId, representativeId, SelfCustodyConsent.IN_INTAKE_COMMAND, signedFormFileId);
    }

    /** Hành chính ghi đồng ý theo bản ký scan của người đại diện (FR-015a). */
    @RequiresPermission(function = Function.BELONGINGS, operation = Operation.UPDATE)
    @Transactional
    public long recordBySignedForm(long belongingId, long representativeId, long signedFormFileId) {
        requireOfficeStaff();
        BelongingView belonging = lockedBelonging(belongingId);
        if (!family.isRepresentative(representativeId, belonging.residentId(), time.now())) {
            throw blocked("GIVER_NOT_REPRESENTATIVE", "Người ký không phải người đại diện tại thời điểm ký");
        }
        return create(
                belonging.residentId(),
                belongingId,
                representativeId,
                SelfCustodyConsent.SIGNED_FORM,
                signedFormFileId);
    }

    /** Người đại diện ghi đồng ý qua cổng (FR-030); người thân khác 403. */
    @Transactional
    public long recordOnPortal(long belongingId) {
        long representativeId = familyMember();
        BelongingView belonging = lockedBelonging(belongingId);
        requireRepresentative(representativeId, belonging.residentId());
        return create(belonging.residentId(), belongingId, representativeId, SelfCustodyConsent.PORTAL, null);
    }

    /** Hành chính ghi bản ký rút đồng ý của người đại diện hiện tại. */
    @RequiresPermission(function = Function.BELONGINGS, operation = Operation.UPDATE)
    @Transactional
    public void withdrawBySignedForm(long consentId, String reason, long representativeId, long signedFormFileId) {
        requireOfficeStaff();
        SimpleRecord<SelfCustodyConsent> consent = lockedConsent(consentId);
        if (!family.isRepresentative(representativeId, consent.residentId(), time.now())) {
            throw blocked("GIVER_NOT_REPRESENTATIVE", "Người ký rút không phải người đại diện hiện tại");
        }
        withdraw(consent, Reason.of(reason), representativeId, signedFormFileId);
    }

    /** Người đại diện hiện tại rút qua cổng (mọi người đại diện hiện tại rút được, FR-015a). */
    @Transactional
    public void withdrawOnPortal(long consentId, String reason) {
        long representativeId = familyMember();
        SimpleRecord<SelfCustodyConsent> consent = lockedConsent(consentId);
        requireRepresentative(representativeId, consent.residentId());
        withdraw(consent, Reason.of(reason), representativeId, null);
    }

    private long create(
            long residentId, long belongingId, long representativeId, String method, Long signedFormFileId) {
        BelongingView belonging = belongings.find(belongingId).orElseThrow(NotFoundException::new);
        if (!EnumSet.of(BelongingStatus.HELD, BelongingStatus.IN_USE).contains(belonging.status())) {
            throw blocked("INVALID_STATE", "Chỉ ghi đồng ý cho đồ Đang giữ hoặc Đang được sử dụng");
        }
        if (!catalogs.require(BelongingCatalogs.BELONGING_TYPE, belonging.typeId())
                .attributes()
                .requiresConsentForUse()) {
            throw blocked("INVALID_STATE", "Loại đồ không cần đồng ý khi giao sử dụng");
        }
        String key = SelfCustodyConsent.effectiveKey(belongingId);
        if (records.findByUniqueKey(SelfCustodyConsent.KIND, key).isPresent()) {
            throw blocked("INVALID_STATE", "Đồ đã có đồng ý cho tự giữ Hiệu lực (BR-M12-06)");
        }
        CurrentUser user = users.getObject();
        Instant now = time.now();
        var created = records.create(
                SelfCustodyConsent.KIND,
                new NewSimpleRecord<>(
                        residentId,
                        new EntityRef(BELONGING_ENTITY, belongingId),
                        SelfCustodyConsent.EFFECTIVE,
                        new SelfCustodyConsent(
                                belongingId,
                                representativeId,
                                method,
                                signedFormFileId,
                                user.accountId(),
                                now,
                                null,
                                null,
                                null,
                                null,
                                null),
                        null,
                        null,
                        key,
                        user.accountId(),
                        false));
        if (signedFormFileId != null) {
            files.attach(signedFormFileId, SIGNED_FORM_OWNER, created.id());
        }
        audit.write(AuditLogEntry.builder()
                .action(SELF_CUSTODY_CONSENT_RECORDED)
                .entity(SelfCustodyConsent.KIND.code(), created.id())
                .resident(residentId)
                .after(Map.of("belongingId", belongingId, "representativeId", representativeId, "method", method))
                .build());
        events.publishEvent(BelongingNotices.notice(
                "SELF_CUSTODY_CONSENT_RECORDED",
                SelfCustodyConsent.KIND.code(),
                created.id(),
                NotificationLevel.LOW,
                residentId,
                List.of(
                        new RecipientGroup.Representative(residentId),
                        new RecipientGroup.FacilityRole(Role.OFFICE_STAFF)),
                typeName(belonging) + ": đã ghi đồng ý cho tự giữ",
                user.accountId(),
                now));
        return created.id();
    }

    private void withdraw(
            SimpleRecord<SelfCustodyConsent> consent, Reason reason, long representativeId, Long signedFormFileId) {
        if (!SelfCustodyConsent.EFFECTIVE.equals(consent.status())) {
            throw blocked("INVALID_STATE", "Đồng ý không còn Hiệu lực");
        }
        CurrentUser user = users.getObject();
        Instant now = time.now();
        records.change(
                SelfCustodyConsent.KIND,
                consent.id(),
                consent.version(),
                new RecordChange<>(
                        SelfCustodyConsent.WITHDRAWN,
                        consent.content()
                                .withdrawn(representativeId, signedFormFileId, reason.value(), user.accountId(), now),
                        null,
                        null,
                        true));
        if (signedFormFileId != null) {
            files.attach(signedFormFileId, SIGNED_FORM_OWNER, consent.id());
        }
        long residentId = consent.residentId();
        BelongingView belonging =
                belongings.find(consent.content().belongingId()).orElseThrow(NotFoundException::new);
        audit.write(AuditLogEntry.builder()
                .action(SELF_CUSTODY_CONSENT_WITHDRAWN)
                .entity(SelfCustodyConsent.KIND.code(), consent.id())
                .resident(residentId)
                .before(Map.of("status", SelfCustodyConsent.EFFECTIVE))
                .after(Map.of("status", SelfCustodyConsent.WITHDRAWN))
                .reason(reason)
                .build());
        String type = typeName(belonging);
        events.publishEvent(BelongingNotices.notice(
                "SELF_CUSTODY_CONSENT_WITHDRAWN",
                SelfCustodyConsent.KIND.code(),
                consent.id(),
                NotificationLevel.LOW,
                residentId,
                List.of(
                        new RecipientGroup.Representative(residentId),
                        new RecipientGroup.FacilityRole(Role.OFFICE_STAFF)),
                type + ": đồng ý cho tự giữ đã bị rút",
                user.accountId(),
                now));
        if (belonging.status() == BelongingStatus.IN_USE) {
            events.publishEvent(BelongingNotices.notice(
                    "SELF_CUSTODY_CONSENT_WITHDRAWN_IN_USE",
                    SelfCustodyConsent.KIND.code(),
                    consent.id(),
                    NotificationLevel.MEDIUM,
                    residentId,
                    List.of(
                            new RecipientGroup.FacilityRole(Role.OFFICE_STAFF),
                            new RecipientGroup.AssignedNurse(residentId, new Substitution.Default())),
                    type + ": đồng ý đã rút khi đồ đang được sử dụng, cần thu lại",
                    user.accountId(),
                    now));
        }
    }

    private BelongingView lockedBelonging(long belongingId) {
        BelongingView belonging = belongings.find(belongingId).orElseThrow(NotFoundException::new);
        residentLock.lock(belonging.residentId());
        return belongings.find(belongingId).orElseThrow(NotFoundException::new);
    }

    private SimpleRecord<SelfCustodyConsent> lockedConsent(long consentId) {
        SimpleRecord<SelfCustodyConsent> consent = records.require(SelfCustodyConsent.KIND, consentId);
        residentLock.lock(consent.residentId());
        return records.lockForUpdate(SelfCustodyConsent.KIND, consentId);
    }

    private String typeName(BelongingView belonging) {
        return catalogs.require(BelongingCatalogs.BELONGING_TYPE, belonging.typeId())
                .name();
    }

    private void requireRepresentative(long familyMemberId, long residentId) {
        if (!family.isRepresentative(familyMemberId, residentId, time.now())) {
            throw new PermissionDeniedException("Chỉ người đại diện hiện tại ghi, rút đồng ý cho tự giữ (FR-015a)");
        }
    }

    private long familyMember() {
        CurrentUser user = users.getObject();
        if (user.type() != AccountType.FAMILY_MEMBER || user.familyMemberId().isEmpty()) {
            throw new PermissionDeniedException("Chỉ tài khoản người thân dùng cổng");
        }
        return user.familyMemberId().get();
    }

    private void requireOfficeStaff() {
        if (!users.getObject().roles().contains(Role.OFFICE_STAFF)) {
            throw new PermissionDeniedException("Chỉ Hành chính ghi bản ký đồng ý cho tự giữ (FR-015a)");
        }
    }

    private static BusinessException blocked(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }
}
