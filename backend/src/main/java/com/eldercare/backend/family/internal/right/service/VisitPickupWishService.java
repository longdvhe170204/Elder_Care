package com.eldercare.backend.family.internal.right.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.internal.relationship.domain.FamilyMember;
import com.eldercare.backend.family.internal.relationship.domain.FamilyRelationship;
import com.eldercare.backend.family.internal.relationship.persistence.FamilyRelationshipStore;
import com.eldercare.backend.family.internal.relationship.persistence.FamilyResidentLock;
import com.eldercare.backend.family.internal.right.domain.RightRequestCheck;
import com.eldercare.backend.family.internal.right.domain.RightRequestCheck.ActiveWish;
import com.eldercare.backend.family.internal.right.domain.RightRequestCheck.ConflictingRight;
import com.eldercare.backend.family.internal.right.domain.RightRequestCheck.HeldRights;
import com.eldercare.backend.family.internal.right.domain.VisitPickupWish;
import com.eldercare.backend.resident.api.OpinionSourceApi;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.OpinionSource;
import com.eldercare.backend.resident.api.dto.OpinionSourceType;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mong muốn của người cao tuổi về khách thăm, người đón ({@code MongMuonService}; 012 T016a, FR-020a, research F-06a):
 * Hành chính, Trưởng tầng, Điều dưỡng có phạm vi ghi (dòng Phụ lục 27 "Mong muốn về khách thăm, người đón"), Quản lý
 * viện xem. Ghi mới cho cùng (người được nói tới, loại) thì bản Hiệu lực cũ chuyển Được thay thế trong cùng lệnh; không
 * tự đổi quyền (Q-307), quyền trái mong muốn chỉ cảnh báo lúc đọc (FR-020b).
 */
@Service
public class VisitPickupWishService {

    private static final AuditAction WISH_RECORDED = new AuditAction("VISIT_PICKUP_WISH_RECORDED", false);

    public record WishData(
            Long relationshipId,
            String fullName,
            String idDocumentNumber,
            VisitPickupWish.Type type,
            boolean wanted,
            OpinionSource source) {}

    public record WishView(SimpleRecord<VisitPickupWish> record, String recordedByName) {}

    public record Wishes(List<WishView> effective, List<WishView> history, List<ConflictingRight> conflictingRights) {}

    private final SimpleRecordApi records;
    private final FamilyRelationshipStore store;
    private final FamilyResidentLock lock;
    private final ResidentApi residents;
    private final OpinionSourceApi opinionSources;
    private final AccountApi accounts;
    private final AuditLogApi auditLog;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> currentUser;

    VisitPickupWishService(
            SimpleRecordApi records,
            FamilyRelationshipStore store,
            FamilyResidentLock lock,
            ResidentApi residents,
            OpinionSourceApi opinionSources,
            AccountApi accounts,
            AuditLogApi auditLog,
            BusinessTime time,
            ObjectProvider<CurrentUser> currentUser) {
        this.records = records;
        this.store = store;
        this.lock = lock;
        this.residents = residents;
        this.opinionSources = opinionSources;
        this.accounts = accounts;
        this.auditLog = auditLog;
        this.time = time;
        this.currentUser = currentUser;
    }

    @RequiresPermission(function = Function.VISITOR_PREFERENCE, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public Wishes wishes(Target target) {
        long residentId = target.residentId();
        List<SimpleRecord<VisitPickupWish>> all = records.listByResident(VisitPickupWish.KIND, residentId, Set.of());
        Map<Long, String> names = new LinkedHashMap<>();
        List<WishView> effective = all.stream()
                .filter(r -> r.status().equals(VisitPickupWish.Status.ACTIVE.name()))
                .map(r -> view(r, names))
                .toList();
        List<WishView> history = all.stream()
                .filter(r -> !r.status().equals(VisitPickupWish.Status.ACTIVE.name()))
                .map(r -> view(r, names))
                .toList();
        return new Wishes(effective, history, conflictingRights(residentId));
    }

    /** Ghi mong muốn; bản Hiệu lực cùng khóa chuyển Được thay thế và khóa dòng (nội dung bất biến). */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.VISITOR_PREFERENCE, operation = Operation.CREATE)
    @Transactional
    public WishView record(Target target, WishData data) {
        CurrentUser user = currentUser.getObject();
        long residentId = target.residentId();
        lock.lock(residentId);
        residents.status(residentId);
        String fullName = data.fullName();
        String idDocumentNumber = blankToNull(data.idDocumentNumber());
        if (data.relationshipId() != null) {
            FamilyRelationship relationship =
                    store.relationship(data.relationshipId()).orElseThrow(NotFoundException::new);
            if (relationship.residentId() != residentId) {
                throw new NotFoundException();
            }
            fullName = store.familyMember(relationship.familyMemberId())
                    .map(FamilyMember::fullName)
                    .orElse(null);
            idDocumentNumber = null;
        } else if (blankToNull(fullName) == null || idDocumentNumber == null) {
            throw new InvalidValueException(
                    "Cần người thân có quan hệ, hoặc họ tên và số giấy tờ của người được nói tới");
        }
        opinionSources.validate(residentId, data.source());
        Instant now = time.now();
        VisitPickupWish wish = new VisitPickupWish(
                data.relationshipId(),
                fullName,
                idDocumentNumber,
                data.type(),
                data.wanted(),
                data.source(),
                user.accountId(),
                now,
                null);
        String key = wish.activeKey(residentId);
        Optional<SimpleRecord<VisitPickupWish>> previous = records.findByUniqueKey(VisitPickupWish.KIND, key);
        int previousVersion = previous.map(p -> records.change(
                                VisitPickupWish.KIND,
                                p.id(),
                                p.version(),
                                new RecordChange<>(
                                        VisitPickupWish.Status.SUPERSEDED.name(), p.content(), null, null, false))
                        .after()
                        .version())
                .orElse(0);
        SimpleRecord<VisitPickupWish> created = records.create(
                VisitPickupWish.KIND,
                new NewSimpleRecord<>(
                        residentId,
                        new EntityRef("M01.RESIDENT", residentId),
                        VisitPickupWish.Status.ACTIVE.name(),
                        wish,
                        null,
                        null,
                        key,
                        user.accountId(),
                        false));
        previous.ifPresent(p -> records.change(
                VisitPickupWish.KIND,
                p.id(),
                previousVersion,
                new RecordChange<>(
                        VisitPickupWish.Status.SUPERSEDED.name(),
                        p.content().supersededBy(created.id()),
                        null,
                        null,
                        true)));
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("relationshipId", wish.relationshipId());
        after.put("fullName", wish.fullName());
        after.put("idDocumentNumber", wish.idDocumentNumber());
        after.put("type", wish.type().name());
        after.put("wanted", wish.wanted());
        after.put("source", wish.source().type().name());
        after.put("supersedes", previous.map(SimpleRecord::id).orElse(null));
        auditLog.write(AuditLogEntry.builder()
                .action(WISH_RECORDED)
                .entity(VisitPickupWish.KIND.code(), created.id())
                .resident(residentId)
                .after(after)
                .build());
        return new WishView(created, accounts.fullName(user.accountId()).orElse(""));
    }

    /** Mong muốn Hiệu lực cho kiểm trái mong muốn (FR-020b). */
    public List<ActiveWish> active(long residentId) {
        return records
                .listByResident(VisitPickupWish.KIND, residentId, Set.of(VisitPickupWish.Status.ACTIVE.name()))
                .stream()
                .map(r -> new ActiveWish(
                        r.id(),
                        r.content().relationshipId(),
                        r.content().idDocumentNumber(),
                        r.content().type(),
                        r.content().wanted(),
                        r.content().source().type() == OpinionSourceType.RESIDENT))
                .toList();
    }

    /** Cảnh báo "quyền trái mong muốn đang hiệu lực" tính lúc đọc (FR-020b câu cuối). */
    public List<ConflictingRight> conflictingRights(long residentId) {
        List<HeldRights> held = store.activeOfResident(residentId).stream()
                .map(r -> new HeldRights(
                        r.id(),
                        store.familyMember(r.familyMemberId())
                                .map(FamilyMember::idDocumentNumber)
                                .orElse(null),
                        r.enabledRights()))
                .toList();
        return RightRequestCheck.conflictingRights(held, active(residentId));
    }

    private WishView view(SimpleRecord<VisitPickupWish> record, Map<Long, String> names) {
        String name = names.computeIfAbsent(
                record.content().recordedBy(), id -> accounts.fullName(id).orElse(""));
        return new WishView(record, name);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
