package com.eldercare.backend.family.internal.right.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipType;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.api.FamilyRightsApi;
import com.eldercare.backend.family.api.dto.FamilyMemberView;
import com.eldercare.backend.family.api.events.FamilyRightChanged;
import com.eldercare.backend.family.api.events.RelationshipChange;
import com.eldercare.backend.family.api.events.RelationshipChanged;
import com.eldercare.backend.family.api.events.RightChangeRequestNotice;
import com.eldercare.backend.family.api.events.RightChangesTookEffect;
import com.eldercare.backend.family.internal.relationship.domain.Dbr02Check;
import com.eldercare.backend.family.internal.relationship.domain.FamilyMember;
import com.eldercare.backend.family.internal.relationship.domain.FamilyRelationship;
import com.eldercare.backend.family.internal.relationship.domain.RelationshipAttribute;
import com.eldercare.backend.family.internal.relationship.persistence.FamilyRelationshipStore;
import com.eldercare.backend.family.internal.relationship.persistence.FamilyResidentLock;
import com.eldercare.backend.family.internal.relationship.persistence.RelationshipHistoryStore;
import com.eldercare.backend.family.internal.right.domain.RightChangeRequest;
import com.eldercare.backend.family.internal.right.domain.RightChangeRequest.ConfirmationMethod;
import com.eldercare.backend.family.internal.right.domain.RightChangeRequest.NewPicker;
import com.eldercare.backend.family.internal.right.domain.RightChangeRequest.Source;
import com.eldercare.backend.family.internal.right.domain.RightChangeRequest.Status;
import com.eldercare.backend.family.internal.right.domain.RightChangeRequest.Type;
import com.eldercare.backend.family.internal.right.domain.RightRequestCheck;
import com.eldercare.backend.family.internal.right.domain.RightRequestCheck.ActiveWish;
import com.eldercare.backend.family.internal.right.domain.SelfReturnFlag;
import com.eldercare.backend.family.internal.right.domain.SelfReturnTurnedOff;
import com.eldercare.backend.resident.api.ConsentApi;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ConsenterType;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Yêu cầu thay đổi quyền, danh sách đón, dấu "được tự về", đặt/thôi người đại diện ({@code YeuCauQuyenService}; 012
 * T016, T016b; BR-M10-07, FR-005, FR-013 → FR-020c; research F-06, F-06a). Vòng đời riêng, không dùng phê duyệt chung
 * của 000 (FR-020). Mọi lệnh khóa người cao tuổi ({@link FamilyResidentLock}) rồi khóa dòng yêu cầu, nên hai người đại
 * diện xác nhận cùng lúc thì người sau nhận {@code STATE_CHANGED}; quyền đổi ghi cột và lịch sử cùng lúc
 * ({@link RelationshipHistoryStore}). Tắt "được phép đón", tắt "được tự về" áp ngay khi lập; Từ chối, Hủy thì khôi phục từ
 * lúc có kết quả, không hồi tố (FR-017). Quyền kiểm ở đây thay cho {@code @RequiresPermission} của lệnh lập, xác nhận, từ
 * chối, hủy vì chú thích ¹⁸ của dòng "Người thân và quyền" cho mọi người thân tự bỏ mình (FR-014) và lệnh xác nhận có hai
 * đường (cổng của người đại diện, bản ký do Hành chính ghi).
 */
@Service
public class RightChangeRequestService {

    private static final String RESIDENT_ENTITY = "M01.RESIDENT";
    private static final AuditAction CREATED = new AuditAction("RIGHT_CHANGE_REQUEST_CREATED", false);
    private static final AuditAction CONFIRMED = new AuditAction("RIGHT_CHANGE_REQUEST_CONFIRMED", false);
    private static final AuditAction APPROVED = new AuditAction("RIGHT_CHANGE_REQUEST_APPROVED", true);
    private static final AuditAction REJECTED = new AuditAction("RIGHT_CHANGE_REQUEST_REJECTED", true);
    private static final AuditAction CANCELLED = new AuditAction("RIGHT_CHANGE_REQUEST_CANCELLED", true);
    private static final AuditAction MEMBER_CREATED = new AuditAction("FAMILY_MEMBER_CREATED", false);
    private static final String REPLACED_BY_REMOVAL = "Được thay bằng yêu cầu bỏ";
    private static final Set<ResidentStatus> FINAL =
            EnumSet.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED);

    public record CreateCommand(
            long residentId,
            Type type,
            Long relationshipId,
            FamilyRight right,
            Boolean newValue,
            NewPicker newPicker,
            boolean withSignedForm,
            Long signedByFamilyMemberId,
            Long evidenceFileId) {}

    /** Thân lệnh xác nhận (bản ký: bản scan, người ký) và duyệt (nội dung đã trao đổi với người cao tuổi). */
    public record DecisionData(Long evidenceFileId, Long signedByFamilyMemberId, String discussedWithResident) {}

    private final SimpleRecordApi records;
    private final FamilyRelationshipStore store;
    private final RelationshipHistoryStore history;
    private final FamilyResidentLock lock;
    private final RightGrantRules grants;
    private final VisitPickupWishService wishes;
    private final FamilyRightsApi rights;
    private final ResidentApi residents;
    private final ConsentApi consents;
    private final PermissionApi permissions;
    private final AuditLogApi auditLog;
    private final FileApi files;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> currentUser;
    private final ApplicationEventPublisher events;

    RightChangeRequestService(
            SimpleRecordApi records,
            FamilyRelationshipStore store,
            RelationshipHistoryStore history,
            FamilyResidentLock lock,
            RightGrantRules grants,
            VisitPickupWishService wishes,
            FamilyRightsApi rights,
            ResidentApi residents,
            ConsentApi consents,
            PermissionApi permissions,
            AuditLogApi auditLog,
            FileApi files,
            BusinessTime time,
            ObjectProvider<CurrentUser> currentUser,
            ApplicationEventPublisher events) {
        this.records = records;
        this.store = store;
        this.history = history;
        this.lock = lock;
        this.grants = grants;
        this.wishes = wishes;
        this.rights = rights;
        this.residents = residents;
        this.consents = consents;
        this.permissions = permissions;
        this.auditLog = auditLog;
        this.files = files;
        this.time = time;
        this.currentUser = currentUser;
        this.events = events;
    }

    /** Đối tượng kiểm quyền của yêu cầu: người cao tuổi của nó. */
    @Transactional(readOnly = true)
    public Target requestTarget(long requestId) {
        SimpleRecord<RightChangeRequest> request = records.require(RightChangeRequest.KIND, requestId);
        return new Target(RightChangeRequest.KIND.code(), String.valueOf(requestId), request.residentId(), null);
    }

    /**
     * Danh sách yêu cầu; người thân thấy mọi yêu cầu của người cao tuổi mình làm người đại diện, còn lại chỉ yêu cầu về
     * chính mình hoặc do mình lập (FR-050).
     */
    @Transactional(readOnly = true)
    public List<SimpleRecord<RightChangeRequest>> list(Long residentId, Status status) {
        CurrentUser user = currentUser.getObject();
        Set<String> statuses = status == null
                ? EnumSet.allOf(Status.class).stream().map(Enum::name).collect(Collectors.toSet())
                : Set.of(status.name());
        if (user.type() == AccountType.FAMILY_MEMBER) {
            long self = user.familyMemberId().orElseThrow();
            List<SimpleRecord<RightChangeRequest>> found = new ArrayList<>();
            for (FamilyRelationship own : store.activeOfFamilyMember(self)) {
                if (residentId != null && own.residentId() != residentId) {
                    continue;
                }
                for (SimpleRecord<RightChangeRequest> r :
                        records.listByResident(RightChangeRequest.KIND, own.residentId(), statuses)) {
                    RightChangeRequest c = r.content();
                    if (own.representative()
                            || Long.valueOf(own.id()).equals(c.relationshipId())
                            || Long.valueOf(self).equals(c.createdByFamilyMemberId())) {
                        found.add(r);
                    }
                }
            }
            return found;
        }
        requireStaff(user);
        if (residentId == null) {
            authorize(user, Operation.VIEW, Target.UNSPECIFIED);
            return records.listByStatus(RightChangeRequest.KIND, statuses);
        }
        authorize(user, Operation.VIEW, residentTarget(residentId));
        return records.listByResident(RightChangeRequest.KIND, residentId, statuses);
    }

    /** Danh sách được phép đón tại thời điểm (FR-007, FR-068). */
    @RequiresPermission(function = Function.FAMILY_MEMBER_PERMISSION, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<FamilyMemberView> pickupList(Target target, Instant at) {
        return rights.allowedPickersAt(target.residentId(), at == null ? time.now() : at);
    }

    /**
     * Lập yêu cầu (FR-013 → FR-018, FR-020b): người đại diện qua cổng, Hành chính kèm bản ký → Hiệu lực ngay, trừ khi trái
     * mong muốn; tắt đón, tắt tự về áp ngay; người đón mới chưa có hồ sơ thì lập hồ sơ và quan hệ kèm.
     */
    @BusinessCommand
    @Transactional
    public SimpleRecord<RightChangeRequest> create(CreateCommand command, Reason reason) {
        CurrentUser user = currentUser.getObject();
        long residentId = command.residentId();
        Target target = residentTarget(residentId);
        lock.lock(residentId);
        residents.status(residentId);
        List<FamilyRelationship> active = store.activeOfResident(residentId);
        Optional<FamilyRelationship> own = Optional.empty();
        if (user.type() == AccountType.FAMILY_MEMBER) {
            long self = user.familyMemberId().orElseThrow();
            own = active.stream().filter(r -> r.familyMemberId() == self).findFirst();
            if (own.isEmpty()) {
                throw new NotFoundException();
            }
            if (command.withSignedForm() || command.signedByFamilyMemberId() != null) {
                throw forbidden("Bản ký do Hành chính ghi nhận tại quầy (FR-015 b)");
            }
        } else {
            requireStaff(user);
            authorize(user, Operation.CREATE, target);
        }

        Type type = required(command.type(), "Cần loại yêu cầu");
        FamilyRelationship relationship = null;
        boolean newValue;
        Instant now = time.now();
        switch (type) {
            case RIGHT -> {
                required(command.right(), "Cần quyền");
                newValue = required(command.newValue(), "Cần giá trị mới");
                if (command.newPicker() != null) {
                    if (command.right() != FamilyRight.PICKUP_ALLOWED || !newValue) {
                        throw new InvalidValueException("Người đón mới chỉ đi kèm yêu cầu bật được phép đón");
                    }
                    if (own.isPresent() && !own.get().representative()) {
                        throw forbidden("Chỉ người đại diện được đề nghị thêm người đón (FR-014)");
                    }
                    relationship = createPicker(residentId, command.newPicker(), user.accountId(), now);
                } else {
                    relationship = activeRelationship(active, command.relationshipId());
                }
                if (relationship.enabledRights().contains(command.right()) == newValue
                        && !replacesPendingGrant(residentId, relationship, command.right(), newValue)) {
                    throw stateChanged("Quyền đã ở giá trị được yêu cầu");
                }
                if (newValue) {
                    grants.check(residentId, relationship, command.right());
                }
            }
            case SELF_RETURN -> {
                if (command.relationshipId() != null) {
                    throw new InvalidValueException("Dấu được tự về không gắn người thân");
                }
                newValue = required(command.newValue(), "Cần giá trị mới");
                if (selfReturn(residentId) == newValue) {
                    throw stateChanged("Dấu được tự về đã ở giá trị được yêu cầu");
                }
            }
            case SET_REPRESENTATIVE, REMOVE_REPRESENTATIVE -> {
                relationship = activeRelationship(active, command.relationshipId());
                newValue = type == Type.SET_REPRESENTATIVE;
                if (relationship.representative() == newValue) {
                    throw stateChanged(
                            newValue ? "Người thân đã là người đại diện" : "Người thân không phải người đại diện");
                }
                if (newValue && active.stream().noneMatch(FamilyRelationship::representative)) {
                    throw stateChanged("Người cao tuổi chưa có người đại diện; dùng lệnh đặt người đại diện đầu tiên");
                }
                if (command.evidenceFileId() == null) {
                    throw new InvalidValueException("Cần bằng chứng đặt, thôi người đại diện (FR-005)");
                }
                if (!newValue) {
                    checkRemoval(residentId, relationship);
                }
            }
            default -> throw new IllegalStateException(type.name());
        }

        boolean selfRestriction = own.isPresent()
                && relationship != null
                && RightRequestCheck.selfRestriction(
                        type,
                        command.right(),
                        newValue,
                        relationship.id() == own.get().id());
        if (own.isPresent() && !own.get().representative() && !selfRestriction) {
            throw forbidden("Người thân không phải người đại diện chỉ được tự bỏ mình khỏi danh sách đón hoặc tắt quyền"
                    + " của chính mình (FR-014)");
        }

        // Chỉ ghi "áp ngay khi lập" khi giá trị thật sự đổi, để Từ chối, Hủy không bật một quyền chưa từng có (Q12)
        boolean applied = RightRequestCheck.appliesOnCreation(type, command.right(), newValue)
                && (type == Type.SELF_RETURN || relationship.enabledRights().contains(command.right()));
        Long creatorFamilyMemberId = own.map(FamilyRelationship::familyMemberId).orElse(null);
        RightChangeRequest request = new RightChangeRequest(
                type,
                relationship == null ? null : relationship.id(),
                type == Type.RIGHT ? command.right() : null,
                newValue,
                own.isPresent() ? Source.PORTAL : Source.COUNTER,
                null,
                null,
                null,
                reason.value(),
                command.newPicker(),
                command.evidenceFileId(),
                creatorFamilyMemberId,
                applied,
                null,
                null,
                null,
                null,
                null);
        String key = request.pendingKey(residentId);
        Optional<SimpleRecord<RightChangeRequest>> pending = records.findByUniqueKey(RightChangeRequest.KIND, key);
        if (pending.isPresent()) {
            RightChangeRequest existing = pending.get().content();
            if (type == Type.RIGHT
                    && command.right() == FamilyRight.PICKUP_ALLOWED
                    && !newValue
                    && existing.newValue()) {
                close(pending.get(), Status.CANCELLED, null, REPLACED_BY_REMOVAL, now, false);
            } else {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "DUPLICATE_PENDING_REQUEST",
                        "Đã có yêu cầu Chờ xác nhận cho quyền này (FR-018)");
            }
        }

        Optional<ActiveWish> conflict = conflict(residentId, request, relationship, creatorFamilyMemberId);
        request = request.withConflict(conflict.map(ActiveWish::id).orElse(null));
        boolean needsManager = RightRequestCheck.needsManagerApproval(type, command.right(), newValue, conflict);

        ConfirmationMethod method = null;
        Long confirmer = null;
        if (own.isPresent() && own.get().representative()) {
            if (!needsManager && RightRequestCheck.mayConfirm(type, own.get().id(), request.relationshipId())) {
                method = ConfirmationMethod.PORTAL;
                confirmer = own.get().familyMemberId();
            }
        } else if (command.withSignedForm()) {
            FamilyRelationship signer = signer(active, command.signedByFamilyMemberId(), command.evidenceFileId());
            if (!needsManager && RightRequestCheck.mayConfirm(type, signer.id(), request.relationshipId())) {
                method = ConfirmationMethod.SIGNED_FORM;
                confirmer = signer.familyMemberId();
            }
        }
        boolean effective = method != null;
        if (effective) {
            request = request.confirmed(method, confirmer, null, user.accountId(), now);
        }

        SimpleRecord<RightChangeRequest> created = records.create(
                RightChangeRequest.KIND,
                new NewSimpleRecord<>(
                        residentId,
                        new EntityRef(RESIDENT_ENTITY, residentId),
                        (effective ? Status.EFFECTIVE : Status.PENDING_CONFIRMATION).name(),
                        request,
                        null,
                        null,
                        effective ? null : key,
                        user.accountId(),
                        effective));
        if (command.evidenceFileId() != null) {
            files.attach(command.evidenceFileId(), RightChangeRequest.KIND.code(), created.id());
        }
        if (applied || effective) {
            apply(residentId, created.id(), request, now, user.accountId(), reason);
        }
        auditLog.write(AuditLogEntry.builder()
                .action(CREATED)
                .entity(RightChangeRequest.KIND.code(), created.id())
                .resident(residentId)
                .after(snapshot(created.status(), request))
                .reason(reason)
                .build());
        if (effective) {
            tookEffect(residentId, created.id(), request, now);
        } else {
            events.publishEvent(new RightChangeRequestNotice(
                    RightChangeRequestNotice.Kind.NEEDS_CONFIRMATION,
                    residentId,
                    created.id(),
                    summary(request, "cần xác nhận"),
                    confirmers(residentId, request),
                    null,
                    null,
                    0,
                    now));
            if (applied) {
                otherRepresentatives(residentId, created.id(), request, now);
            }
        }
        return created;
    }

    /** Người đại diện xác nhận qua cổng, hoặc Hành chính ghi nhận bản ký kèm bản scan (FR-015 a, b; FR-016). */
    @BusinessCommand(requiresReason = false)
    @Transactional
    public SimpleRecord<RightChangeRequest> confirm(Target target, long requestId, DecisionData data) {
        CurrentUser user = currentUser.getObject();
        long residentId = target.residentId();
        lock.lock(residentId);
        SimpleRecord<RightChangeRequest> record = pending(requestId);
        RightChangeRequest request = record.content();
        List<FamilyRelationship> active = store.activeOfResident(residentId);
        ConfirmationMethod method;
        FamilyRelationship confirmer;
        if (user.type() == AccountType.FAMILY_MEMBER) {
            long self = user.familyMemberId().orElseThrow();
            confirmer = active.stream()
                    .filter(r -> r.familyMemberId() == self && r.representative())
                    .findFirst()
                    .orElseThrow(() -> forbidden("Chỉ người đại diện xác nhận được yêu cầu (FR-015)"));
            method = ConfirmationMethod.PORTAL;
        } else {
            requireStaff(user);
            authorize(user, Operation.UPDATE, target);
            confirmer = signer(active, data.signedByFamilyMemberId(), data.evidenceFileId());
            method = ConfirmationMethod.SIGNED_FORM;
        }
        if (!RightRequestCheck.mayConfirm(request.type(), confirmer.id(), request.relationshipId())) {
            throw new PermissionDeniedException(
                    "NOMINEE_NOT_CONFIRMED",
                    "Người bị tác động không xác nhận được yêu cầu đặt, thôi người đại diện của mình (FR-005)",
                    Map.of());
        }
        Optional<ActiveWish> conflict = recheck(residentId, request, record.createdBy());
        if (RightRequestCheck.needsManagerApproval(request.type(), request.right(), request.newValue(), conflict)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "WISH_CONFLICT_REQUIRES_MANAGER",
                    "Yêu cầu trái mong muốn của người cao tuổi chỉ Hiệu lực khi Quản lý viện duyệt (FR-020b)");
        }
        Instant now = time.now();
        RightChangeRequest confirmed = request.withConflict(
                        conflict.map(ActiveWish::id).orElse(null))
                .confirmed(method, confirmer.familyMemberId(), data.evidenceFileId(), user.accountId(), now);
        if (data.evidenceFileId() != null) {
            files.attach(data.evidenceFileId(), RightChangeRequest.KIND.code(), requestId);
        }
        return takeEffect(record, confirmed, now, user, CONFIRMED, null);
    }

    /** Quản lý viện duyệt kèm lý do; trái mong muốn thì kèm nội dung đã trao đổi với người cao tuổi (FR-015 c, FR-020b). */
    @BusinessCommand
    @RequiresPermission(function = Function.FAMILY_MEMBER_PERMISSION, operation = Operation.APPROVE)
    @Transactional
    public SimpleRecord<RightChangeRequest> approve(Target target, long requestId, Reason reason, DecisionData data) {
        CurrentUser user = currentUser.getObject();
        requireStaff(user);
        lock.lock(target.residentId());
        SimpleRecord<RightChangeRequest> record = pending(requestId);
        RightChangeRequest request = record.content();
        Optional<ActiveWish> conflict = recheck(target.residentId(), request, record.createdBy());
        String discussed = data == null ? null : data.discussedWithResident();
        if (RightRequestCheck.needsManagerApproval(request.type(), request.right(), request.newValue(), conflict)
                && (discussed == null || discussed.isBlank())) {
            throw new InvalidValueException(
                    "Yêu cầu trái mong muốn của người cao tuổi cần ghi nội dung đã trao đổi với người cao tuổi (FR-020b)");
        }
        Instant now = time.now();
        RightChangeRequest approved = request.withConflict(
                        conflict.map(ActiveWish::id).orElse(null))
                .approved(user.accountId(), now, reason.value(), discussed);
        return takeEffect(record, approved, now, user, APPROVED, reason);
    }

    /** Người đại diện hoặc Quản lý viện từ chối kèm lý do; quyền đã áp ngay được khôi phục (FR-015, FR-017). */
    @BusinessCommand
    @Transactional
    public SimpleRecord<RightChangeRequest> reject(Target target, long requestId, Reason reason) {
        CurrentUser user = currentUser.getObject();
        long residentId = target.residentId();
        lock.lock(residentId);
        SimpleRecord<RightChangeRequest> record = pending(requestId);
        if (user.type() == AccountType.FAMILY_MEMBER) {
            long self = user.familyMemberId().orElseThrow();
            FamilyRelationship representative = store.activeOfResident(residentId).stream()
                    .filter(r -> r.familyMemberId() == self && r.representative())
                    .findFirst()
                    .orElseThrow(() -> forbidden("Chỉ người đại diện hoặc Quản lý viện từ chối được yêu cầu (FR-015)"));
            if (!RightRequestCheck.mayConfirm(
                    record.content().type(),
                    representative.id(),
                    record.content().relationshipId())) {
                throw forbidden(
                        "Người bị tác động không quyết định yêu cầu đặt, thôi người đại diện của mình (FR-005)");
            }
        } else {
            requireStaff(user);
            authorize(user, Operation.APPROVE, target);
        }
        return close(record, Status.REJECTED, user.accountId(), reason.value(), time.now(), true);
    }

    /** Người lập hủy kèm lý do (data-model mục 3). */
    @BusinessCommand
    @Transactional
    public SimpleRecord<RightChangeRequest> cancel(Target target, long requestId, Reason reason) {
        CurrentUser user = currentUser.getObject();
        lock.lock(target.residentId());
        SimpleRecord<RightChangeRequest> record = pending(requestId);
        if (record.createdBy() != user.accountId()) {
            throw forbidden("Chỉ người lập hủy được yêu cầu");
        }
        return close(record, Status.CANCELLED, user.accountId(), reason.value(), time.now(), true);
    }

    /** Kết thúc quan hệ hủy yêu cầu Chờ xác nhận của người đó, không khôi phục (FR-006); gọi trong lệnh kết thúc. */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void cancelForEndedRelationship(long residentId, long relationshipId) {
        Instant now = time.now();
        for (SimpleRecord<RightChangeRequest> record : pendingOf(residentId)) {
            if (Long.valueOf(relationshipId).equals(record.content().relationshipId())) {
                close(record, Status.CANCELLED, null, "Quan hệ đã kết thúc (FR-006)", now, false);
            }
        }
    }

    /** Trạng thái cuối hủy mọi yêu cầu Chờ xác nhận (FR-075); quyền đã áp ngay được khôi phục như Hủy (FR-017). */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void cancelOnFinalStatus(long residentId) {
        lock.lock(residentId);
        Instant now = time.now();
        for (SimpleRecord<RightChangeRequest> record : pendingOf(residentId)) {
            close(record, Status.CANCELLED, null, "Người cao tuổi chuyển trạng thái cuối (FR-075)", now, true);
        }
    }

    public static boolean isFinal(ResidentStatus status) {
        return FINAL.contains(status);
    }

    /** Dòng phiếu đăng ký trái mong muốn thành yêu cầu Chờ xác nhận (FR-020b, FR-008); rỗng khi không trái. */
    public Optional<Long> registrationFormConflict(
            long residentId, FamilyRelationship relationship, FamilyRight right, long signerFamilyMemberId) {
        RightChangeRequest probe = RightChangeRequest.ofRegistrationForm(relationship.id(), right, 0, 0, null);
        return conflict(residentId, probe, relationship, signerFamilyMemberId).map(ActiveWish::id);
    }

    /** Khóa {@code khoa_cho} của dòng phiếu Chờ xác nhận. */
    public static String pendingKey(long residentId, long relationshipId, FamilyRight right) {
        return RightChangeRequest.ofRegistrationForm(relationshipId, right, 0, 0, null)
                .pendingKey(residentId);
    }

    /** Thông báo "cần xác nhận/duyệt" của dòng phiếu Chờ xác nhận. */
    public void registrationLinePending(long residentId, long requestId, RightChangeRequest request, Instant at) {
        events.publishEvent(new RightChangeRequestNotice(
                RightChangeRequestNotice.Kind.NEEDS_CONFIRMATION,
                residentId,
                requestId,
                summary(request, "cần Quản lý viện duyệt"),
                confirmers(residentId, request),
                null,
                null,
                0,
                at));
    }

    /** Nhắc yêu cầu Chờ xác nhận quá mốc của CFG-M10-11 (FR-019); không tự Từ chối. */
    public void remind(long residentId, long requestId, RightChangeRequest request, boolean managerNotice, Instant at) {
        events.publishEvent(new RightChangeRequestNotice(
                managerNotice ? RightChangeRequestNotice.Kind.MANAGER_NOTICE : RightChangeRequestNotice.Kind.REMINDER,
                residentId,
                requestId,
                summary(request, "đang chờ xác nhận quá hạn"),
                managerNotice ? List.of() : confirmers(residentId, request),
                null,
                null,
                1,
                at));
    }

    private SimpleRecord<RightChangeRequest> takeEffect(
            SimpleRecord<RightChangeRequest> record,
            RightChangeRequest request,
            Instant now,
            CurrentUser user,
            AuditAction action,
            Reason reason) {
        long residentId = record.residentId();
        SimpleRecord<RightChangeRequest> after = records.change(
                        RightChangeRequest.KIND,
                        record.id(),
                        record.version(),
                        new RecordChange<>(Status.EFFECTIVE.name(), request, null, null, true))
                .after();
        if (!request.appliedOnCreation()) {
            apply(residentId, record.id(), request, now, user.accountId(), reason);
        }
        auditLog.write(AuditLogEntry.builder()
                .action(action)
                .entity(RightChangeRequest.KIND.code(), record.id())
                .resident(residentId)
                .before(Map.of("status", record.status()))
                .after(snapshot(after.status(), request))
                .reason(reason)
                .build());
        tookEffect(residentId, record.id(), request, now);
        return after;
    }

    private SimpleRecord<RightChangeRequest> close(
            SimpleRecord<RightChangeRequest> record,
            Status status,
            Long by,
            String reason,
            Instant now,
            boolean restore) {
        RightChangeRequest closed = record.content().closed(by, now, reason);
        SimpleRecord<RightChangeRequest> after = records.change(
                        RightChangeRequest.KIND,
                        record.id(),
                        record.version(),
                        new RecordChange<>(status.name(), closed, null, null, true))
                .after();
        if (restore && closed.appliedOnCreation()) {
            restore(record.residentId(), record.id(), closed, now, by);
        }
        auditLog.write(AuditLogEntry.builder()
                .action(status == Status.REJECTED ? REJECTED : CANCELLED)
                .entity(RightChangeRequest.KIND.code(), record.id())
                .resident(record.residentId())
                .before(Map.of("status", record.status()))
                .after(Map.of("status", status.name()))
                .reason(new Reason(reason))
                .build());
        events.publishEvent(new RightChangeRequestNotice(
                RightChangeRequestNotice.Kind.DECIDED,
                record.residentId(),
                record.id(),
                summary(closed, status == Status.REJECTED ? "đã bị từ chối" : "đã hủy"),
                List.of(),
                closed.createdByFamilyMemberId(),
                affectedFamilyMember(closed),
                0,
                now));
        return after;
    }

    /** Áp kết quả yêu cầu (cột, lịch sử, sự kiện); tắt đón, tắt tự về áp ngay khi lập (FR-017). */
    private void apply(
            long residentId, long requestId, RightChangeRequest request, Instant at, Long actor, Reason reason) {
        switch (request.type()) {
            case RIGHT -> setRight(relationship(request), request.right(), request.newValue(), at, actor, reason);
            case SELF_RETURN -> setSelfReturn(residentId, requestId, request.newValue(), at, actor);
            case SET_REPRESENTATIVE -> {
                FamilyRelationship relationship = relationship(request);
                requireActive(relationship);
                store.setRepresentative(relationship.id(), true);
                history.record(
                        relationship.id(), RelationshipAttribute.REPRESENTATIVE, true, at, command(), actor, reason);
                setRight(relationship, FamilyRight.REQUEST_SERVICES, true, at, actor, reason);
                changed(relationship, at);
            }
            case REMOVE_REPRESENTATIVE -> {
                FamilyRelationship relationship = relationship(request);
                requireActive(relationship);
                checkRemoval(residentId, relationship);
                store.setRepresentative(relationship.id(), false);
                history.record(
                        relationship.id(), RelationshipAttribute.REPRESENTATIVE, false, at, command(), actor, reason);
                setRight(relationship, FamilyRight.REQUEST_SERVICES, false, at, actor, reason);
                changed(relationship, at);
            }
        }
    }

    /** Từ chối, Hủy yêu cầu đã áp ngay: khôi phục từ lúc có kết quả (FR-017). */
    private void restore(long residentId, long requestId, RightChangeRequest request, Instant at, Long actor) {
        if (request.type() == Type.SELF_RETURN) {
            setSelfReturn(residentId, requestId, !request.newValue(), at, actor);
            return;
        }
        FamilyRelationship relationship = relationship(request);
        if (relationship.active()) {
            setRight(relationship, request.right(), !request.newValue(), at, actor, null);
        }
    }

    private void setRight(
            FamilyRelationship relationship, FamilyRight right, boolean value, Instant at, Long actor, Reason reason) {
        FamilyRelationship current = store.relationship(relationship.id()).orElseThrow();
        if (current.enabledRights().contains(right) == value) {
            return;
        }
        store.setRight(current.id(), right, value);
        history.record(current.id(), RelationshipAttribute.of(right), value, at, command(), actor, reason);
        events.publishEvent(
                new FamilyRightChanged(current.id(), current.familyMemberId(), current.residentId(), right, value, at));
    }

    private void setSelfReturn(long residentId, long requestId, boolean value, Instant at, Long actor) {
        SelfReturnFlag flag = new SelfReturnFlag(value, requestId, at);
        Optional<SimpleRecord<SelfReturnFlag>> current =
                records.findByUniqueKey(SelfReturnFlag.KIND, SelfReturnFlag.key(residentId));
        if (current.isPresent()) {
            records.change(
                    SelfReturnFlag.KIND,
                    current.get().id(),
                    current.get().version(),
                    new RecordChange<>(value ? "ON" : "OFF", flag, null, SelfReturnFlag.key(residentId), false));
        } else {
            records.create(
                    SelfReturnFlag.KIND,
                    new NewSimpleRecord<>(
                            residentId,
                            new EntityRef(RESIDENT_ENTITY, residentId),
                            value ? "ON" : "OFF",
                            flag,
                            null,
                            null,
                            SelfReturnFlag.key(residentId),
                            actor == null ? 0 : actor,
                            false));
        }
        if (!value) {
            events.publishEvent(new SelfReturnTurnedOff(residentId, at));
        }
    }

    /** Yêu cầu bỏ "được phép đón" khi người đó còn yêu cầu bật đang chờ: được lập để thay yêu cầu đó (FR-018, Q12). */
    private boolean replacesPendingGrant(
            long residentId, FamilyRelationship relationship, FamilyRight right, boolean newValue) {
        return right == FamilyRight.PICKUP_ALLOWED
                && !newValue
                && records.findByUniqueKey(RightChangeRequest.KIND, pendingKey(residentId, relationship.id(), right))
                        .filter(p -> p.content().newValue())
                        .isPresent();
    }

    private boolean selfReturn(long residentId) {
        return records.findByUniqueKey(SelfReturnFlag.KIND, SelfReturnFlag.key(residentId))
                .map(r -> r.content().enabled())
                .orElse(false);
    }

    /** Thôi người đại diện bị từ chối khi làm mất người đại diện cuối (DBR-02) hoặc người đó là người đồng ý (FR-005). */
    private void checkRemoval(long residentId, FamilyRelationship relationship) {
        ResidentStatus status = residents.status(residentId);
        List<FamilyRelationship> active = store.activeOfResident(residentId);
        List<Dbr02Check.Roles> before = roles(active, null);
        List<Dbr02Check.Roles> after = roles(active, relationship.id());
        List<Dbr02Check.Violation> found = new ArrayList<>(Dbr02Check.violations(status, after));
        found.removeAll(Dbr02Check.violations(status, before));
        if (!found.isEmpty()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "DBR_02_VIOLATED",
                    "Thôi người đại diện làm người cao tuổi vi phạm DBR-02: " + found);
        }
        boolean consenter = consents.current(residentId)
                .filter(c -> c.consenterType() == ConsenterType.LEGAL_REPRESENTATIVE)
                .flatMap(c -> c.consenterFamilyMemberId())
                .filter(id -> id == relationship.familyMemberId())
                .isPresent();
        if (consenter) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "IS_CONSENTER",
                    "Người đại diện là người đồng ý trên bản đồng ý Hiệu lực; cần bản đồng ý thay thế trước (FR-005)");
        }
    }

    private static List<Dbr02Check.Roles> roles(List<FamilyRelationship> active, Long removedRepresentative) {
        return active.stream()
                .map(r -> new Dbr02Check.Roles(
                        r.representative() && !Long.valueOf(r.id()).equals(removedRepresentative), r.primaryContact()))
                .toList();
    }

    /** Kiểm lại lúc xác nhận, duyệt (FR-013: xem sức khỏe; FR-007; FR-020b), trả mong muốn bị trái nếu có. */
    private Optional<ActiveWish> recheck(long residentId, RightChangeRequest request, long createdBy) {
        FamilyRelationship relationship = null;
        if (request.relationshipId() != null) {
            relationship = relationship(request);
            requireActive(relationship);
        }
        if (request.type() == Type.RIGHT && request.newValue()) {
            grants.check(residentId, relationship, request.right());
        }
        if (request.type() == Type.REMOVE_REPRESENTATIVE) {
            checkRemoval(residentId, relationship);
        }
        return conflict(residentId, request, relationship, request.createdByFamilyMemberId());
    }

    private Optional<ActiveWish> conflict(
            long residentId, RightChangeRequest request, FamilyRelationship relationship, Long creatorFamilyMemberId) {
        if (relationship == null) {
            return Optional.empty();
        }
        String idDocument = store.familyMember(relationship.familyMemberId())
                .map(FamilyMember::idDocumentNumber)
                .orElse(null);
        boolean byLegalRepresentative = creatorFamilyMemberId != null
                && consents.legalRepresentative(residentId)
                        .filter(creatorFamilyMemberId::equals)
                        .isPresent();
        return RightRequestCheck.conflictingWish(
                request.type(),
                request.right(),
                request.newValue(),
                new RightRequestCheck.Person(relationship.id(), idDocument),
                wishes.active(residentId),
                byLegalRepresentative);
    }

    /** Người đón mới chưa có hồ sơ: lập hồ sơ và quan hệ (mọi quyền tắt) kèm lệnh lập (FR-013, data-model mục 3). */
    private FamilyRelationship createPicker(long residentId, NewPicker picker, long actor, Instant now) {
        if (blank(picker.fullName())
                || blank(picker.phoneNumber())
                || blank(picker.idDocumentType())
                || blank(picker.idDocumentNumber())
                || blank(picker.relationship())) {
            throw new InvalidValueException("Người đón mới cần họ tên, số điện thoại, loại và số giấy tờ, quan hệ");
        }
        FamilyRelationshipType type;
        try {
            type = FamilyRelationshipType.valueOf(picker.relationship());
        } catch (IllegalArgumentException e) {
            throw new InvalidValueException("Quan hệ không hợp lệ");
        }
        long memberId = store.insertFamilyMember(
                picker.fullName().trim(),
                picker.phoneNumber().trim(),
                picker.idDocumentType().trim(),
                picker.idDocumentNumber().trim(),
                null,
                actor,
                now);
        auditLog.write(AuditLogEntry.builder()
                .action(MEMBER_CREATED)
                .entity(FamilyMember.ENTITY_TYPE, memberId)
                .after(Map.of(
                        "fullName", picker.fullName(),
                        "phoneNumber", picker.phoneNumber(),
                        "idDocumentType", picker.idDocumentType(),
                        "idDocumentNumber", picker.idDocumentNumber()))
                .build());
        long id = store.insertRelationship(
                memberId, residentId, type, false, false, store.nextContactOrder(residentId), Set.of(), actor, now);
        FamilyRelationship created = store.relationship(id).orElseThrow();
        events.publishEvent(new RelationshipChanged(memberId, residentId, id, RelationshipChange.CREATED, now));
        return created;
    }

    private FamilyRelationship signer(List<FamilyRelationship> active, Long signedBy, Long evidenceFileId) {
        if (signedBy == null || evidenceFileId == null) {
            throw new InvalidValueException("Bản ký cần người đại diện ký và bản scan (FR-015 b)");
        }
        return active.stream()
                .filter(r -> r.familyMemberId() == signedBy && r.representative())
                .findFirst()
                .orElseThrow(() -> RightGrantRules.requestNotAllowed(
                        HttpStatus.CONFLICT, "Người ký phải là người đại diện Hiệu lực của người cao tuổi"));
    }

    private SimpleRecord<RightChangeRequest> pending(long requestId) {
        SimpleRecord<RightChangeRequest> record = records.lockForUpdate(RightChangeRequest.KIND, requestId);
        if (!record.status().equals(Status.PENDING_CONFIRMATION.name())) {
            throw stateChanged("Yêu cầu đã có kết quả");
        }
        return record;
    }

    private List<SimpleRecord<RightChangeRequest>> pendingOf(long residentId) {
        return records.listByResident(RightChangeRequest.KIND, residentId, Set.of(Status.PENDING_CONFIRMATION.name()));
    }

    /** Hiệu lực: báo kết quả (FR-069) và báo người đại diện khác (Q-249, Q-267). */
    private void tookEffect(long residentId, long requestId, RightChangeRequest request, Instant at) {
        events.publishEvent(new RightChangeRequestNotice(
                RightChangeRequestNotice.Kind.DECIDED,
                residentId,
                requestId,
                summary(request, "đã có hiệu lực"),
                List.of(),
                request.createdByFamilyMemberId(),
                affectedFamilyMember(request),
                0,
                at));
        otherRepresentatives(residentId, requestId, request, at);
    }

    /** Người đại diện Hiệu lực khác, xét sau khi áp; trừ người lập, người xác nhận, người bị thôi hoặc vừa thêm. */
    private void otherRepresentatives(long residentId, long requestId, RightChangeRequest request, Instant at) {
        Long affected = request.type() == Type.SET_REPRESENTATIVE || request.type() == Type.REMOVE_REPRESENTATIVE
                ? affectedFamilyMember(request)
                : null;
        List<Long> others = store.activeOfResident(residentId).stream()
                .filter(FamilyRelationship::representative)
                .map(FamilyRelationship::familyMemberId)
                .filter(id -> !id.equals(request.createdByFamilyMemberId()))
                .filter(id -> !id.equals(request.confirmedByFamilyMemberId()))
                .filter(id -> !id.equals(affected))
                .distinct()
                .toList();
        events.publishEvent(new RightChangesTookEffect(
                residentId,
                new SourceRecord(RightChangeRequest.KIND.code(), String.valueOf(requestId)),
                summary(request, "đã có hiệu lực"),
                others,
                at));
    }

    /** Người được xác nhận: người đại diện Hiệu lực, trừ người bị tác động của yêu cầu đặt/thôi người đại diện. */
    private List<Long> confirmers(long residentId, RightChangeRequest request) {
        return store.activeOfResident(residentId).stream()
                .filter(FamilyRelationship::representative)
                .filter(r -> RightRequestCheck.mayConfirm(request.type(), r.id(), request.relationshipId()))
                .map(FamilyRelationship::familyMemberId)
                .distinct()
                .toList();
    }

    private Long affectedFamilyMember(RightChangeRequest request) {
        return request.relationshipId() == null
                ? null
                : store.relationship(request.relationshipId())
                        .map(FamilyRelationship::familyMemberId)
                        .orElse(null);
    }

    private String summary(RightChangeRequest request, String outcome) {
        String who = request.relationshipId() == null
                ? ""
                : store.relationship(request.relationshipId())
                        .flatMap(r -> store.familyMember(r.familyMemberId()))
                        .map(m -> " cho " + m.fullName())
                        .orElse("");
        String change = switch (request.type()) {
            case RIGHT -> (request.newValue() ? "Bật quyền " : "Tắt quyền ") + label(request.right()) + who;
            case SELF_RETURN -> (request.newValue() ? "Bật" : "Tắt") + " dấu được tự về";
            case SET_REPRESENTATIVE -> "Đặt người đại diện" + who;
            case REMOVE_REPRESENTATIVE -> "Thôi người đại diện" + who;
        };
        return "Yêu cầu: " + change + " " + outcome + ".";
    }

    private static String label(FamilyRight right) {
        return switch (right) {
            case VIEW_HEALTH -> "xem sức khỏe";
            case VIEW_COSTS -> "xem chi phí";
            case RECEIVE_URGENT -> "nhận thông báo khẩn";
            case REGISTER_VISITS -> "đăng ký thăm";
            case PICKUP_ALLOWED -> "được phép đón";
            case REQUEST_SERVICES -> "được yêu cầu thay đổi dịch vụ";
        };
    }

    private static Map<String, Object> snapshot(String status, RightChangeRequest request) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("status", status);
        values.put("type", request.type().name());
        values.put("relationshipId", request.relationshipId());
        values.put("right", request.right() == null ? null : request.right().name());
        values.put("newValue", request.newValue());
        values.put("source", request.source().name());
        values.put(
                "confirmationMethod",
                request.confirmationMethod() == null
                        ? null
                        : request.confirmationMethod().name());
        values.put("appliedOnCreation", request.appliedOnCreation());
        values.put("conflictingWishId", request.conflictingWishId());
        values.put("discussedWithResident", request.discussedWithResident());
        return values;
    }

    private FamilyRelationship relationship(RightChangeRequest request) {
        return store.relationship(request.relationshipId()).orElseThrow();
    }

    private FamilyRelationship activeRelationship(List<FamilyRelationship> active, Long relationshipId) {
        if (relationshipId == null) {
            throw new InvalidValueException("Cần quan hệ của người thân bị tác động");
        }
        Optional<FamilyRelationship> found =
                active.stream().filter(r -> r.id() == relationshipId).findFirst();
        if (found.isPresent()) {
            return found.get();
        }
        store.relationship(relationshipId).orElseThrow(NotFoundException::new);
        throw stateChanged("Quan hệ đã kết thúc (FR-006)");
    }

    private static void requireActive(FamilyRelationship relationship) {
        if (!relationship.active()) {
            throw stateChanged("Quan hệ đã kết thúc (FR-006)");
        }
    }

    private void changed(FamilyRelationship relationship, Instant at) {
        events.publishEvent(new RelationshipChanged(
                relationship.familyMemberId(),
                relationship.residentId(),
                relationship.id(),
                RelationshipChange.REPRESENTATIVE_CHANGED,
                at));
    }

    private void authorize(CurrentUser user, Operation operation, Target target) {
        PermissionResult result = permissions.hasPermission(
                user, new PermissionKey(Function.FAMILY_MEMBER_PERMISSION, operation), target);
        if (result.allowed()) {
            return;
        }
        FailedLayer layer = result.failedLayer().orElse(FailedLayer.ROLE);
        if (layer == FailedLayer.SCOPE) {
            throw new NotFoundException();
        }
        throw new PermissionDeniedException("PERMISSION_DENIED", "Không có quyền", Map.of("failedLayer", layer.name()));
    }

    private static void requireStaff(CurrentUser user) {
        if (user.type() != AccountType.STAFF) {
            throw new PermissionDeniedException("PERMISSION_DENIED", "Không có quyền", Map.of("failedLayer", "ROLE"));
        }
    }

    private static Target residentTarget(long residentId) {
        return new Target(RESIDENT_ENTITY, String.valueOf(residentId), residentId, null);
    }

    private static String command() {
        return "RIGHT_CHANGE_REQUEST";
    }

    private static <T> T required(T value, String message) {
        if (value == null) {
            throw new InvalidValueException(message);
        }
        return value;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static BusinessException forbidden(String message) {
        return RightGrantRules.requestNotAllowed(HttpStatus.FORBIDDEN, message);
    }

    private static BusinessException stateChanged(String message) {
        return new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", message);
    }
}
