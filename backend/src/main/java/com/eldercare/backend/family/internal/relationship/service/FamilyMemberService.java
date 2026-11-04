package com.eldercare.backend.family.internal.relationship.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipType;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.ReasonRequiredException;
import com.eldercare.backend.common.error.VersionMismatchException;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.family.api.events.FamilyRightChanged;
import com.eldercare.backend.family.api.events.RelationshipChange;
import com.eldercare.backend.family.api.events.RelationshipChanged;
import com.eldercare.backend.family.api.events.RightChangesTookEffect;
import com.eldercare.backend.family.internal.relationship.domain.Dbr02Check;
import com.eldercare.backend.family.internal.relationship.domain.FamilyMember;
import com.eldercare.backend.family.internal.relationship.domain.FamilyRegistrationForm;
import com.eldercare.backend.family.internal.relationship.domain.FamilyRelationship;
import com.eldercare.backend.family.internal.relationship.domain.RelationshipAttribute;
import com.eldercare.backend.family.internal.relationship.persistence.FamilyRelationshipStore;
import com.eldercare.backend.family.internal.relationship.persistence.FamilyResidentLock;
import com.eldercare.backend.family.internal.relationship.persistence.RelationshipHistoryStore;
import com.eldercare.backend.family.internal.right.domain.EffectiveRights;
import com.eldercare.backend.family.internal.right.domain.RightChangeRequest;
import com.eldercare.backend.family.internal.right.service.RightChangeRequestService;
import com.eldercare.backend.family.internal.right.service.RightGrantRules;
import com.eldercare.backend.resident.api.ConsentApi;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
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
 * Hồ sơ người thân, quan hệ, vai trò và phiếu đăng ký ({@code NguoiThanService}; 012 T010, T011; FR-001 → FR-012,
 * DBR-02, research F-03 → F-05). Mọi lệnh trên quan hệ khóa người cao tuổi ({@link FamilyResidentLock}), ghi cột và lịch
 * sử quyền, vai trò cùng lúc ({@link RelationshipHistoryStore}), rồi kiểm DBR-02: lệnh làm phát sinh vi phạm mới bị từ
 * chối, vi phạm có sẵn không chặn lệnh khác. Đặt, thôi người đại diện khi đã có người đại diện đi qua yêu cầu (US3).
 */
@Service
public class FamilyMemberService {

    public static final String EVIDENCE_OWNER_TYPE = "M10.REPRESENTATIVE_EVIDENCE";

    private static final String RESIDENT_ENTITY = "M01.RESIDENT";
    private static final AuditAction MEMBER_CREATED = new AuditAction("FAMILY_MEMBER_CREATED", false);
    private static final AuditAction MEMBER_UPDATED = new AuditAction("FAMILY_MEMBER_UPDATED", false);
    private static final AuditAction RELATIONSHIP_CREATED = new AuditAction("FAMILY_RELATIONSHIP_CREATED", false);
    private static final AuditAction PRIMARY_CONTACT_CHANGED = new AuditAction("FAMILY_PRIMARY_CONTACT_CHANGED", true);
    private static final AuditAction FIRST_REPRESENTATIVE_SET =
            new AuditAction("FAMILY_FIRST_REPRESENTATIVE_SET", true);
    private static final AuditAction CONTACT_ORDER_CHANGED = new AuditAction("FAMILY_CONTACT_ORDER_CHANGED", true);
    private static final AuditAction RELATIONSHIP_ENDED = new AuditAction("FAMILY_RELATIONSHIP_ENDED", true);
    private static final AuditAction FORM_RECORDED = new AuditAction("FAMILY_REGISTRATION_FORM_RECORDED", false);
    private static final int SEARCH_LIMIT = 50;

    /** Dữ liệu hồ sơ người thân (FR-001). */
    public record FamilyMemberData(
            String fullName, String phoneNumber, String idDocumentType, String idDocumentNumber, String address) {

        boolean hasIdDocument() {
            return idDocumentType != null
                    && !idDocumentType.isBlank()
                    && idDocumentNumber != null
                    && !idDocumentNumber.isBlank();
        }
    }

    public record CreateRelationship(
            long familyMemberId,
            FamilyRelationshipType relationship,
            boolean primaryContact,
            boolean setFirstRepresentative,
            Long evidenceFileId,
            String reason) {}

    public record RegistrationFormLine(long relationshipId, FamilyRight right) {}

    /** Quan hệ kèm hồ sơ người thân, quyền đang bật và quyền có tác dụng (FR-011). */
    public record RelationshipView(
            FamilyRelationship relationship, FamilyMember familyMember, Map<FamilyRight, Boolean> effective) {}

    public record FamilyMemberDetail(FamilyMember familyMember, List<FamilyRelationship> relationships) {}

    private final FamilyRelationshipStore store;
    private final RelationshipHistoryStore history;
    private final FamilyResidentLock lock;
    private final ResidentApi residents;
    private final ConsentApi consents;
    private final AccountApi accounts;
    private final AuditLogApi auditLog;
    private final FileApi files;
    private final RecordEventApi recordEvents;
    private final SimpleRecordApi records;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> currentUser;
    private final ApplicationEventPublisher events;
    private final RightGrantRules grants;
    private final RightChangeRequestService requests;

    FamilyMemberService(
            FamilyRelationshipStore store,
            RelationshipHistoryStore history,
            FamilyResidentLock lock,
            ResidentApi residents,
            ConsentApi consents,
            AccountApi accounts,
            AuditLogApi auditLog,
            FileApi files,
            RecordEventApi recordEvents,
            SimpleRecordApi records,
            BusinessTime time,
            ObjectProvider<CurrentUser> currentUser,
            ApplicationEventPublisher events,
            RightGrantRules grants,
            RightChangeRequestService requests) {
        this.grants = grants;
        this.requests = requests;
        this.store = store;
        this.history = history;
        this.lock = lock;
        this.residents = residents;
        this.consents = consents;
        this.accounts = accounts;
        this.auditLog = auditLog;
        this.files = files;
        this.recordEvents = recordEvents;
        this.records = records;
        this.time = time;
        this.currentUser = currentUser;
        this.events = events;
    }

    public static Target residentTarget(long residentId) {
        return new Target(RESIDENT_ENTITY, String.valueOf(residentId), residentId, null);
    }

    public static Target familyMemberTarget(long familyMemberId) {
        return new Target(FamilyMember.ENTITY_TYPE, String.valueOf(familyMemberId), null, null);
    }

    /** Đối tượng kiểm quyền của quan hệ: người cao tuổi của nó. */
    @Transactional(readOnly = true)
    public Target relationshipTarget(long relationshipId) {
        FamilyRelationship relationship = store.relationship(relationshipId).orElseThrow(NotFoundException::new);
        return new Target(
                RelationshipAttribute.ENTITY_TYPE, String.valueOf(relationshipId), relationship.residentId(), null);
    }

    @RequiresPermission(function = Function.FAMILY_MEMBER_PERMISSION, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<FamilyMember> search(String q) {
        requireStaff();
        return store.searchFamilyMembers(q == null ? "" : q.trim(), SEARCH_LIMIT);
    }

    @RequiresPermission(function = Function.FAMILY_MEMBER_PERMISSION, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public FamilyMemberDetail familyMember(Target target, long familyMemberId) {
        requireStaff();
        FamilyMember member = store.familyMember(familyMemberId).orElseThrow(NotFoundException::new);
        return new FamilyMemberDetail(member, store.ofFamilyMember(familyMemberId));
    }

    /** Quan hệ của người cao tuổi kể cả đã kết thúc; người thân chỉ thấy quan hệ của chính mình (FR-067). */
    @RequiresPermission(function = Function.FAMILY_MEMBER_PERMISSION, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<RelationshipView> relationships(Target target) {
        long residentId = target.residentId();
        Optional<Long> self = currentUser.getObject().familyMemberId();
        List<RelationshipView> views = new ArrayList<>();
        for (FamilyRelationship relationship : store.ofResident(residentId)) {
            if (self.isPresent() && self.get() != relationship.familyMemberId()) {
                continue;
            }
            FamilyMember member =
                    store.familyMember(relationship.familyMemberId()).orElseThrow();
            boolean consent = consents.includes(residentId, relationship.familyMemberId());
            Map<FamilyRight, Boolean> effective = new EnumMap<>(FamilyRight.class);
            for (FamilyRight right : FamilyRight.values()) {
                effective.put(
                        right,
                        relationship.active()
                                && EffectiveRights.effective(
                                        right, relationship.enabledRights().contains(right), consent));
            }
            views.add(new RelationshipView(relationship, member, effective));
        }
        return views;
    }

    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.FAMILY_MEMBER_PERMISSION, operation = Operation.CREATE)
    @Transactional
    public FamilyMember createFamilyMember(FamilyMemberData data) {
        CurrentUser user = requireStaff();
        long id = store.insertFamilyMember(
                data.fullName(),
                data.phoneNumber(),
                blankToNull(data.idDocumentType()),
                blankToNull(data.idDocumentNumber()),
                blankToNull(data.address()),
                user.accountId(),
                time.now());
        FamilyMember created = store.familyMember(id).orElseThrow();
        auditLog.write(AuditLogEntry.builder()
                .action(MEMBER_CREATED)
                .entity(FamilyMember.ENTITY_TYPE, id)
                .after(snapshot(created))
                .build());
        return created;
    }

    /**
     * Sửa thông tin liên hệ có nhật ký trước/sau (FR-001); bỏ giấy tờ khi còn quyền được phép đón bật bị chặn (FR-007).
     * {@code If-Match} không khớp phiên bản thì 412.
     */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.FAMILY_MEMBER_PERMISSION, operation = Operation.UPDATE)
    @Transactional
    public FamilyMember updateFamilyMember(Target target, long familyMemberId, String ifMatch, FamilyMemberData data) {
        requireStaff();
        FamilyMember before = store.familyMember(familyMemberId).orElseThrow(NotFoundException::new);
        VersionEtag.requireMatch(ifMatch, before.version(), before);
        int version = before.version();
        for (FamilyRelationship relationship : store.activeOfFamilyMember(familyMemberId)) {
            lock.lock(relationship.residentId());
        }
        if (!data.hasIdDocument()
                && store.activeOfFamilyMember(familyMemberId).stream()
                        .anyMatch(r -> r.enabledRights().contains(FamilyRight.PICKUP_ALLOWED))) {
            throw idDocumentRequired();
        }
        int updated = store.updateFamilyMember(
                familyMemberId,
                version,
                data.fullName(),
                data.phoneNumber(),
                blankToNull(data.idDocumentType()),
                blankToNull(data.idDocumentNumber()),
                blankToNull(data.address()));
        if (updated == 0) {
            throw new VersionMismatchException(before);
        }
        FamilyMember after = store.familyMember(familyMemberId).orElseThrow();
        auditLog.write(AuditLogEntry.builder()
                .action(MEMBER_UPDATED)
                .entity(FamilyMember.ENTITY_TYPE, familyMemberId)
                .before(snapshot(before))
                .after(snapshot(after))
                .build());
        return after;
    }

    /**
     * Lập quan hệ: mọi quyền tắt trừ FR-009 (liên hệ chính nhận thông báo khẩn), FR-010 (người đại diện được yêu cầu
     * thay đổi dịch vụ); tùy chọn đặt liên hệ chính, người đại diện đầu tiên (lý do, bằng chứng; FR-005).
     */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.FAMILY_MEMBER_PERMISSION, operation = Operation.CREATE)
    @Transactional
    public FamilyRelationship createRelationship(Target target, CreateRelationship command) {
        CurrentUser user = requireStaff();
        long residentId = target.residentId();
        lock.lock(residentId);
        ResidentStatus status = residents.status(residentId);
        store.familyMember(command.familyMemberId()).orElseThrow(NotFoundException::new);
        List<FamilyRelationship> active = store.activeOfResident(residentId);
        if (active.stream().anyMatch(r -> r.familyMemberId() == command.familyMemberId())) {
            throw stateChanged("Người thân đã có quan hệ Hiệu lực với người cao tuổi");
        }
        if (command.primaryContact() && active.stream().anyMatch(FamilyRelationship::primaryContact)) {
            throw dbr02("Người cao tuổi đã có người liên hệ chính; dùng lệnh đổi người liên hệ chính");
        }
        Reason reason = null;
        if (command.setFirstRepresentative()) {
            requireNoRepresentative(active);
            reason = reason(command.reason());
            requireEvidence(command.evidenceFileId());
        }
        Instant now = time.now();
        Set<FamilyRight> rights = EnumSet.noneOf(FamilyRight.class);
        if (command.primaryContact()) {
            rights.add(FamilyRight.RECEIVE_URGENT);
        }
        if (command.setFirstRepresentative()) {
            rights.add(FamilyRight.REQUEST_SERVICES);
        }
        long id = store.insertRelationship(
                command.familyMemberId(),
                residentId,
                command.relationship(),
                command.setFirstRepresentative(),
                command.primaryContact(),
                store.nextContactOrder(residentId),
                rights,
                user.accountId(),
                now);
        if (command.setFirstRepresentative()) {
            history.record(id, RelationshipAttribute.REPRESENTATIVE, true, now, "CREATE", user.accountId(), reason);
            files.attach(command.evidenceFileId(), EVIDENCE_OWNER_TYPE, id);
        }
        if (command.primaryContact()) {
            history.record(id, RelationshipAttribute.PRIMARY_CONTACT, true, now, "CREATE", user.accountId(), null);
        }
        for (FamilyRight right : rights) {
            history.record(id, RelationshipAttribute.of(right), true, now, "CREATE", user.accountId(), null);
        }
        requireDbr02(residentId, status, active);
        FamilyRelationship created = store.relationship(id).orElseThrow();
        auditLog.write(AuditLogEntry.builder()
                .action(RELATIONSHIP_CREATED)
                .entity(RelationshipAttribute.ENTITY_TYPE, id)
                .resident(residentId)
                .after(snapshot(created))
                .build());
        if (command.setFirstRepresentative()) {
            auditLog.write(AuditLogEntry.builder()
                    .action(FIRST_REPRESENTATIVE_SET)
                    .entity(RelationshipAttribute.ENTITY_TYPE, id)
                    .resident(residentId)
                    .before(Map.of("representative", false))
                    .after(Map.of("representative", true, "evidenceFileId", command.evidenceFileId()))
                    .reason(reason)
                    .build());
        }
        changed(created, RelationshipChange.CREATED, now);
        for (FamilyRight right : rights) {
            events.publishEvent(rightChanged(created, right, true, now));
        }
        return created;
    }

    /** Chuyển vai trò liên hệ chính sang quan hệ này và bỏ vai trò của người cũ trong cùng lệnh (FR-004, FR-009). */
    @BusinessCommand
    @RequiresPermission(function = Function.FAMILY_MEMBER_PERMISSION, operation = Operation.UPDATE)
    @Transactional
    public FamilyRelationship changePrimaryContact(Target target, long relationshipId, Reason reason) {
        CurrentUser user = requireStaff();
        long residentId = target.residentId();
        lock.lock(residentId);
        FamilyRelationship relationship = requireActive(relationshipId);
        if (relationship.primaryContact()) {
            throw stateChanged("Người thân đã là người liên hệ chính");
        }
        ResidentStatus status = residents.status(residentId);
        List<FamilyRelationship> active = store.activeOfResident(residentId);
        Instant now = time.now();
        Optional<FamilyRelationship> previous =
                active.stream().filter(FamilyRelationship::primaryContact).findFirst();
        previous.ifPresent(old -> {
            store.setPrimaryContact(old.id(), false);
            history.record(
                    old.id(),
                    RelationshipAttribute.PRIMARY_CONTACT,
                    false,
                    now,
                    "CHANGE_PRIMARY_CONTACT",
                    user.accountId(),
                    reason);
        });
        store.setPrimaryContact(relationshipId, true);
        history.record(
                relationshipId,
                RelationshipAttribute.PRIMARY_CONTACT,
                true,
                now,
                "CHANGE_PRIMARY_CONTACT",
                user.accountId(),
                reason);
        boolean urgentTurnedOn = enableRight(relationship, FamilyRight.RECEIVE_URGENT, now, "CHANGE_PRIMARY_CONTACT");
        requireDbr02(residentId, status, active);
        auditLog.write(AuditLogEntry.builder()
                .action(PRIMARY_CONTACT_CHANGED)
                .entity(RelationshipAttribute.ENTITY_TYPE, relationshipId)
                .resident(residentId)
                .before(Map.of(
                        "primaryContactRelationshipId",
                        previous.map(FamilyRelationship::id).orElse(0L)))
                .after(Map.of("primaryContactRelationshipId", relationshipId))
                .reason(reason)
                .build());
        FamilyRelationship updated = store.relationship(relationshipId).orElseThrow();
        previous.ifPresent(old -> changed(old, RelationshipChange.PRIMARY_CONTACT_CHANGED, now));
        changed(updated, RelationshipChange.PRIMARY_CONTACT_CHANGED, now);
        if (urgentTurnedOn) {
            events.publishEvent(rightChanged(updated, FamilyRight.RECEIVE_URGENT, true, now));
        }
        return updated;
    }

    /** Đặt người đại diện đầu tiên trực tiếp khi người cao tuổi chưa có người đại diện (FR-005, FR-010). */
    @BusinessCommand
    @RequiresPermission(function = Function.FAMILY_MEMBER_PERMISSION, operation = Operation.UPDATE)
    @Transactional
    public FamilyRelationship setFirstRepresentative(
            Target target, long relationshipId, Reason reason, Long evidenceFileId) {
        CurrentUser user = requireStaff();
        long residentId = target.residentId();
        lock.lock(residentId);
        FamilyRelationship relationship = requireActive(relationshipId);
        requireEvidence(evidenceFileId);
        requireNoRepresentative(store.activeOfResident(residentId));
        Instant now = time.now();
        store.setRepresentative(relationshipId, true);
        history.record(
                relationshipId,
                RelationshipAttribute.REPRESENTATIVE,
                true,
                now,
                "SET_FIRST_REPRESENTATIVE",
                user.accountId(),
                reason);
        boolean servicesTurnedOn =
                enableRight(relationship, FamilyRight.REQUEST_SERVICES, now, "SET_FIRST_REPRESENTATIVE");
        files.attach(evidenceFileId, EVIDENCE_OWNER_TYPE, relationshipId);
        auditLog.write(AuditLogEntry.builder()
                .action(FIRST_REPRESENTATIVE_SET)
                .entity(RelationshipAttribute.ENTITY_TYPE, relationshipId)
                .resident(residentId)
                .before(Map.of("representative", false))
                .after(Map.of("representative", true, "evidenceFileId", evidenceFileId))
                .reason(reason)
                .build());
        FamilyRelationship updated = store.relationship(relationshipId).orElseThrow();
        changed(updated, RelationshipChange.REPRESENTATIVE_CHANGED, now);
        if (servicesTurnedOn) {
            events.publishEvent(rightChanged(updated, FamilyRight.REQUEST_SERVICES, true, now));
        }
        return updated;
    }

    /** Đổi thứ tự liên hệ cho chuỗi gọi của 009 (FR-012); không cần xác nhận BR-M10-07. */
    @BusinessCommand
    @RequiresPermission(function = Function.FAMILY_MEMBER_PERMISSION, operation = Operation.UPDATE)
    @Transactional
    public FamilyRelationship changeContactOrder(
            Target target, long relationshipId, Reason reason, Integer contactOrder) {
        requireStaff();
        if (contactOrder == null) {
            throw new InvalidValueException("Cần thứ tự liên hệ mới");
        }
        long residentId = target.residentId();
        lock.lock(residentId);
        FamilyRelationship relationship = requireActive(relationshipId);
        store.setContactOrder(relationshipId, contactOrder);
        auditLog.write(AuditLogEntry.builder()
                .action(CONTACT_ORDER_CHANGED)
                .entity(RelationshipAttribute.ENTITY_TYPE, relationshipId)
                .resident(residentId)
                .before(Map.of("contactOrder", relationship.contactOrder()))
                .after(Map.of("contactOrder", contactOrder))
                .reason(reason)
                .build());
        FamilyRelationship updated = store.relationship(relationshipId).orElseThrow();
        changed(updated, RelationshipChange.CONTACT_ORDER_CHANGED, time.now());
        return updated;
    }

    /**
     * Kết thúc quan hệ (FR-006): bỏ khỏi danh sách được phép đón, xét lại tài khoản người thân trong transaction của lệnh
     * (002 FR-049), hủy yêu cầu Chờ xác nhận của người đó (US3); không kích hoạt lại. Hủy lượt thăm tương lai bổ sung
     * cùng US4.
     */
    @BusinessCommand
    @RequiresPermission(function = Function.FAMILY_MEMBER_PERMISSION, operation = Operation.UPDATE)
    @Transactional
    public FamilyRelationship end(Target target, long relationshipId, Reason reason) {
        CurrentUser user = requireStaff();
        long residentId = target.residentId();
        lock.lock(residentId);
        FamilyRelationship relationship = requireActive(relationshipId);
        ResidentStatus status = residents.status(residentId);
        List<FamilyRelationship> active = store.activeOfResident(residentId);
        Instant now = time.now();
        store.end(relationshipId, reason.value(), now);
        boolean pickupRemoved = relationship.enabledRights().contains(FamilyRight.PICKUP_ALLOWED);
        if (pickupRemoved) {
            store.setRight(relationshipId, FamilyRight.PICKUP_ALLOWED, false);
            history.record(
                    relationshipId, RelationshipAttribute.PICKUP_ALLOWED, false, now, "END", user.accountId(), reason);
        }
        requireDbr02(residentId, status, active);
        requests.cancelForEndedRelationship(residentId, relationshipId);
        accounts.recheckFamilyMember(relationship.familyMemberId(), new Basis("FR-006", null, relationshipId, null));
        auditLog.write(AuditLogEntry.builder()
                .action(RELATIONSHIP_ENDED)
                .entity(RelationshipAttribute.ENTITY_TYPE, relationshipId)
                .resident(residentId)
                .before(Map.of("status", "ACTIVE"))
                .after(Map.of("status", "ENDED"))
                .reason(reason)
                .build());
        FamilyRelationship ended = store.relationship(relationshipId).orElseThrow();
        changed(ended, RelationshipChange.ENDED, now);
        if (pickupRemoved) {
            events.publishEvent(rightChanged(ended, FamilyRight.PICKUP_ALLOWED, false, now));
        }
        return ended;
    }

    /**
     * Phiếu đăng ký người thân do người đại diện ký (FR-008, Q-126): mỗi quyền trên phiếu thành một yêu cầu Hiệu lực ngay
     * cách "bản ký" (FR-015 b) sau kiểm tra FR-007, FR-010, FR-011; báo các người đại diện khác (Q-249). Dòng trái mong
     * muốn của người cao tuổi thành yêu cầu Chờ xác nhận chờ Quản lý viện duyệt, dòng khác vẫn Hiệu lực; lỗi khác chặn cả
     * phiếu (FR-020b, research F-06a).
     */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.FAMILY_MEMBER_PERMISSION, operation = Operation.CREATE)
    @Transactional
    public long recordRegistrationForm(
            Target target, long representativeId, Long scanFileId, List<RegistrationFormLine> lines) {
        CurrentUser user = requireStaff();
        long residentId = target.residentId();
        lock.lock(residentId);
        residents.status(residentId);
        if (scanFileId == null) {
            throw new InvalidValueException("Cần bản scan phiếu đăng ký người thân");
        }
        if (lines == null || lines.isEmpty()) {
            throw new InvalidValueException("Phiếu cần ít nhất một quyền");
        }
        List<FamilyRelationship> active = store.activeOfResident(residentId);
        FamilyRelationship signer = active.stream()
                .filter(r -> r.familyMemberId() == representativeId && r.representative())
                .findFirst()
                .orElseThrow(
                        () -> requestNotAllowed("Người ký phiếu phải là người đại diện Hiệu lực của người cao tuổi"));
        Map<Long, FamilyRelationship> byId = active.stream().collect(Collectors.toMap(FamilyRelationship::id, r -> r));
        for (RegistrationFormLine line : lines) {
            FamilyRelationship relationship = byId.get(line.relationshipId());
            if (relationship == null) {
                throw new NotFoundException();
            }
            grants.check(residentId, relationship, line.right());
        }
        Map<RegistrationFormLine, Long> conflicts = new LinkedHashMap<>();
        for (RegistrationFormLine line : lines) {
            requests.registrationFormConflict(
                            residentId, byId.get(line.relationshipId()), line.right(), representativeId)
                    .ifPresent(wishId -> conflicts.put(line, wishId));
        }
        for (RegistrationFormLine line : conflicts.keySet()) {
            if (records.findByUniqueKey(
                            RightChangeRequest.KIND,
                            RightChangeRequestService.pendingKey(residentId, line.relationshipId(), line.right()))
                    .isPresent()) {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "DUPLICATE_PENDING_REQUEST",
                        "Đã có yêu cầu Chờ xác nhận cho quyền này (FR-018)");
            }
        }

        Instant now = time.now();
        EntityRef parent = new EntityRef(RESIDENT_ENTITY, residentId);
        long formId = recordEvents
                .add(
                        FamilyRegistrationForm.KIND,
                        parent,
                        new NewRecordEvent<>(
                                new FamilyRegistrationForm(representativeId, scanFileId, List.of()),
                                null,
                                user.accountId(),
                                false,
                                now,
                                null,
                                null,
                                null,
                                null))
                .id();
        List<FamilyRegistrationForm.Line> recorded = new ArrayList<>();
        List<FamilyRegistrationForm.Line> effective = new ArrayList<>();
        List<FamilyRightChanged> rightEvents = new ArrayList<>();
        for (RegistrationFormLine line : lines) {
            FamilyRelationship relationship = byId.get(line.relationshipId());
            Long wishId = conflicts.get(line);
            RightChangeRequest request = RightChangeRequest.ofRegistrationForm(
                    relationship.id(), line.right(), representativeId, formId, wishId);
            long requestId = records.create(
                            RightChangeRequest.KIND,
                            new NewSimpleRecord<>(
                                    residentId,
                                    new EntityRef(FamilyRegistrationForm.KIND.code(), formId),
                                    (wishId == null
                                                    ? RightChangeRequest.Status.EFFECTIVE
                                                    : RightChangeRequest.Status.PENDING_CONFIRMATION)
                                            .name(),
                                    request,
                                    null,
                                    null,
                                    wishId == null ? null : request.pendingKey(residentId),
                                    user.accountId(),
                                    wishId == null))
                    .id();
            FamilyRegistrationForm.Line recordedLine =
                    new FamilyRegistrationForm.Line(relationship.id(), line.right(), requestId);
            recorded.add(recordedLine);
            if (wishId != null) {
                requests.registrationLinePending(residentId, requestId, request, now);
                continue;
            }
            effective.add(recordedLine);
            if (enableRight(relationship, line.right(), now, "REGISTRATION_FORM")) {
                rightEvents.add(rightChanged(relationship, line.right(), true, now));
            }
            byId.put(relationship.id(), store.relationship(relationship.id()).orElseThrow());
        }
        FamilyRegistrationForm form = new FamilyRegistrationForm(representativeId, scanFileId, recorded);
        recordEvents.updateDraft(FamilyRegistrationForm.KIND, formId, form);
        recordEvents.confirm(FamilyRegistrationForm.KIND, formId, now);
        files.attach(scanFileId, FamilyRegistrationForm.KIND.code(), formId);
        auditLog.write(AuditLogEntry.builder()
                .action(FORM_RECORDED)
                .entity(FamilyRegistrationForm.KIND.code(), formId)
                .resident(residentId)
                .after(Map.of(
                        "representativeId", representativeId,
                        "scanFileId", scanFileId,
                        "lines",
                                recorded.stream()
                                        .map(l -> l.relationshipId() + ":" + l.right() + ":" + l.requestId())
                                        .toList()))
                .build());
        rightEvents.forEach(events::publishEvent);
        if (effective.isEmpty()) {
            return formId;
        }
        List<Long> others = store.activeOfResident(residentId).stream()
                .filter(FamilyRelationship::representative)
                .map(FamilyRelationship::familyMemberId)
                .filter(id -> id != signer.familyMemberId())
                .distinct()
                .toList();
        events.publishEvent(new RightChangesTookEffect(
                residentId,
                new SourceRecord(FamilyRegistrationForm.KIND.code(), String.valueOf(formId)),
                formSummary(signer, effective),
                others,
                now));
        return formId;
    }

    /** Bật quyền (cột và lịch sử) khi đang tắt; trả {@code true} khi đã đổi. */
    private boolean enableRight(FamilyRelationship relationship, FamilyRight right, Instant at, String command) {
        if (relationship.enabledRights().contains(right)) {
            return false;
        }
        store.setRight(relationship.id(), right, true);
        history.record(
                relationship.id(),
                RelationshipAttribute.of(right),
                true,
                at,
                command,
                currentUser.getObject().accountId(),
                null);
        return true;
    }

    /** Lệnh làm phát sinh vi phạm DBR-02 mới bị từ chối; vi phạm có sẵn trước lệnh không chặn (FR-003). */
    private void requireDbr02(long residentId, ResidentStatus status, List<FamilyRelationship> before) {
        List<Dbr02Check.Violation> found =
                new ArrayList<>(Dbr02Check.violations(status, roles(store.activeOfResident(residentId))));
        found.removeAll(Dbr02Check.violations(status, roles(before)));
        if (!found.isEmpty()) {
            throw dbr02("Lệnh làm người cao tuổi vi phạm DBR-02: " + found);
        }
    }

    static List<Dbr02Check.Roles> roles(List<FamilyRelationship> active) {
        return active.stream()
                .map(r -> new Dbr02Check.Roles(r.representative(), r.primaryContact()))
                .toList();
    }

    private FamilyRelationship requireActive(long relationshipId) {
        FamilyRelationship relationship = store.relationship(relationshipId).orElseThrow(NotFoundException::new);
        if (!relationship.active()) {
            throw stateChanged("Quan hệ đã kết thúc, không kích hoạt lại được (FR-006)");
        }
        return relationship;
    }

    private static void requireNoRepresentative(List<FamilyRelationship> active) {
        if (active.stream().anyMatch(FamilyRelationship::representative)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "REPRESENTATIVE_ALREADY_EXISTS",
                    "Người cao tuổi đã có người đại diện; đặt thêm đi qua yêu cầu thay đổi (FR-005)");
        }
    }

    private static void requireEvidence(Long evidenceFileId) {
        if (evidenceFileId == null) {
            throw new InvalidValueException("Cần bằng chứng đặt người đại diện (FR-005)");
        }
    }

    private static Reason reason(String value) {
        if (value == null) {
            throw new ReasonRequiredException();
        }
        return new Reason(value);
    }

    /** Lệnh trên hồ sơ, quan hệ chỉ của Hành chính; người thân có Tạo, Sửa cho yêu cầu qua cổng (US3). */
    private CurrentUser requireStaff() {
        CurrentUser user = currentUser.getObject();
        if (user.type() != AccountType.STAFF) {
            throw new PermissionDeniedException("PERMISSION_DENIED", "Không có quyền", Map.of("failedLayer", "ROLE"));
        }
        return user;
    }

    private void changed(FamilyRelationship relationship, RelationshipChange change, Instant at) {
        events.publishEvent(new RelationshipChanged(
                relationship.familyMemberId(), relationship.residentId(), relationship.id(), change, at));
    }

    private static FamilyRightChanged rightChanged(
            FamilyRelationship relationship, FamilyRight right, boolean value, Instant at) {
        return new FamilyRightChanged(
                relationship.id(), relationship.familyMemberId(), relationship.residentId(), right, value, at);
    }

    private String formSummary(FamilyRelationship signer, List<FamilyRegistrationForm.Line> lines) {
        String changes = lines.stream()
                .map(l -> name(store.relationship(l.relationshipId())
                                .orElseThrow()
                                .familyMemberId()) + ": " + label(l.right()))
                .collect(Collectors.joining("; "));
        return "Phiếu đăng ký người thân do " + name(signer.familyMemberId()) + " ký đã có hiệu lực. Bật quyền: "
                + changes + ".";
    }

    private String name(long familyMemberId) {
        return store.familyMember(familyMemberId).map(FamilyMember::fullName).orElse("");
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

    private static Map<String, Object> snapshot(FamilyMember member) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("fullName", member.fullName());
        values.put("phoneNumber", member.phoneNumber());
        values.put("idDocumentType", member.idDocumentType());
        values.put("idDocumentNumber", member.idDocumentNumber());
        values.put("address", member.address());
        return values;
    }

    private static Map<String, Object> snapshot(FamilyRelationship relationship) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("familyMemberId", relationship.familyMemberId());
        values.put("relationship", relationship.relationship().name());
        values.put("representative", relationship.representative());
        values.put("primaryContact", relationship.primaryContact());
        values.put("contactOrder", relationship.contactOrder());
        values.put(
                "rights",
                relationship.enabledRights().stream().map(Enum::name).sorted().toList());
        return values;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static BusinessException idDocumentRequired() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "ID_DOCUMENT_REQUIRED",
                "Người được phép đón phải có loại và số giấy tờ tùy thân (FR-007)");
    }

    private static BusinessException requestNotAllowed(String message) {
        return new BusinessException(HttpStatus.CONFLICT, "REQUEST_NOT_ALLOWED", message);
    }

    private static BusinessException stateChanged(String message) {
        return new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", message);
    }

    private static BusinessException dbr02(String message) {
        return new BusinessException(HttpStatus.CONFLICT, "DBR_02_VIOLATED", message);
    }
}
