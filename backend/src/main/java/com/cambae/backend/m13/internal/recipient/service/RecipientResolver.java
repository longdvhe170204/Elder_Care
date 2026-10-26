package com.cambae.backend.m13.internal.recipient.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.notification.ContentPart;
import com.cambae.backend.common.notification.FamilyRight;
import com.cambae.backend.common.notification.InformationType;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.Q354Case;
import com.cambae.backend.common.notification.RecipientGroup;
import com.cambae.backend.common.notification.Substitution;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m09.api.ShiftApi;
import com.cambae.backend.m13.api.dto.RecipientType;
import com.cambae.backend.m13.api.spi.FamilyContactView;
import com.cambae.backend.m13.api.spi.FamilyNotificationPort;
import com.cambae.backend.m13.api.spi.NotifiableRelationshipView;
import com.cambae.backend.m13.internal.intake.domain.ValidatedRequest;
import com.cambae.backend.m13.internal.recipient.domain.ConsentScope;
import com.cambae.backend.m13.internal.recipient.domain.ContentFilter;
import com.cambae.backend.m13.internal.recipient.domain.ResolvedRecipient;
import com.cambae.backend.m13.internal.recipient.domain.SubstitutionChain;
import com.cambae.backend.m13.internal.recipient.domain.SubstitutionStep;
import com.cambae.backend.m15.account.api.AccountApi;
import com.cambae.backend.m15.account.api.dto.AccountStatus;
import com.cambae.backend.m15.account.api.dto.AccountView;
import com.cambae.backend.m15.account.api.spi.AssignmentApi;
import com.cambae.backend.m15.account.api.spi.DutyApi;
import com.cambae.backend.m15.account.api.spi.LicenseApi;
import com.cambae.backend.m15.account.api.spi.StaffProfileApi;
import com.cambae.backend.m15.account.api.spi.StaffStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * Xác định người nhận tại thời điểm gửi ({@code XacDinhNguoiNhan}; FR-007 → FR-014a, BR-M13-04, research B-04): từng nhóm
 * qua phân công, nhiệm vụ (008 qua SPI của 002), tài khoản (002), quan hệ người thân (012); cách thay (FR-010), loại trừ
 * (FR-008), không có người nhận (FR-011), gộp và người gây ra (FR-012), lọc người thân (FR-013, FR-014).
 *
 * Bản đồng ý và qua đời đọc từ 001 qua {@link ResidentFacts} (FR-013, FR-014, FR-015).
 *
 * <p>Nhóm {@code OnShiftStaff}, {@code OnDutyDoctor} qua {@link ShiftApi} của 008 ({@code CaTrucApi}): người có mặt trong ca
 * đang diễn ra, nên người đã Ghi nhận vắng ca không có (FR-008); chứng chỉ phải còn hiệu lực ngày gửi ({@link LicenseApi}).
 */
@Service
public class RecipientResolver {

    /**
     * @param groupOutcomes kết quả từng nhóm theo thứ tự trong yêu cầu
     * @param noRecipient sau cách thay vẫn không có ai (FR-011)
     * @param missingCase có người thân ngoài phạm vi bị bỏ vì yêu cầu Khẩn cấp thiếu mã trường hợp (FR-001a)
     */
    public record Resolution(
            List<ResolvedRecipient> recipients, List<String> groupOutcomes, boolean noRecipient, boolean missingCase) {}

    static final String HAS_RECIPIENTS = "HAS_RECIPIENTS";
    static final String SUBSTITUTED = "SUBSTITUTED";
    static final String SKIPPED_NO_SUBSTITUTE = "SKIPPED_NO_SUBSTITUTE";
    static final String NONE = "NONE";
    static final String NO_RECIPIENT_FALLBACK = "NO_RECIPIENT_FALLBACK";

    private final ObjectProvider<AccountApi> accounts;
    private final ObjectProvider<AssignmentApi> assignments;
    private final ObjectProvider<DutyApi> duties;
    private final ObjectProvider<StaffProfileApi> profiles;
    private final ObjectProvider<FamilyNotificationPort> family;
    private final ObjectProvider<ShiftApi> shifts;
    private final ObjectProvider<LicenseApi> licenses;
    private final ResidentFacts facts;
    private final BusinessTime time;

    RecipientResolver(
            ObjectProvider<AccountApi> accounts,
            ObjectProvider<AssignmentApi> assignments,
            ObjectProvider<DutyApi> duties,
            ObjectProvider<StaffProfileApi> profiles,
            ObjectProvider<FamilyNotificationPort> family,
            ObjectProvider<ShiftApi> shifts,
            ObjectProvider<LicenseApi> licenses,
            ResidentFacts facts,
            BusinessTime time) {
        this.accounts = accounts;
        this.assignments = assignments;
        this.duties = duties;
        this.profiles = profiles;
        this.family = family;
        this.shifts = shifts;
        this.licenses = licenses;
        this.facts = facts;
        this.time = time;
    }

    public Resolution resolve(ValidatedRequest v, Instant at) {
        NotificationRequest r = v.request();
        Set<InformationType> parts =
                v.content().stream().map(ContentPart::type).collect(Collectors.toCollection(LinkedHashSet::new));
        Recipients found = new Recipients(parts);
        List<String> outcomes = new ArrayList<>();
        boolean missingCase = false;
        for (RecipientGroup g : r.recipientGroups() == null ? List.<RecipientGroup>of() : r.recipientGroups()) {
            switch (g) {
                case RecipientGroup.DutyGroup d -> outcomes.add(duty(d, r, at, found));
                case RecipientGroup.PrimaryContact p -> {
                    FamilyResult f = family(g, p.residentId(), v, found);
                    outcomes.add(f.added() ? HAS_RECIPIENTS : NONE);
                    missingCase |= f.missingCase();
                }
                case RecipientGroup.Representative p -> {
                    FamilyResult f = family(g, p.residentId(), v, found);
                    outcomes.add(f.added() ? HAS_RECIPIENTS : NONE);
                    missingCase |= f.missingCase();
                }
                case RecipientGroup.FamilyWithRight p -> {
                    FamilyResult f = family(g, p.residentId(), v, found);
                    outcomes.add(f.added() ? HAS_RECIPIENTS : NONE);
                    missingCase |= f.missingCase();
                }
                case RecipientGroup.FamilyMember m -> {
                    FamilyResult f = family(g, r.residentId().orElse(null), v, found);
                    outcomes.add(f.added() ? HAS_RECIPIENTS : NONE);
                    missingCase |= f.missingCase();
                }
                case RecipientGroup.Resident p -> outcomes.add(resident(p, found) ? HAS_RECIPIENTS : NONE);
                default -> outcomes.add(found.addStaff(direct(g), basis(g)) ? HAS_RECIPIENTS : NONE);
            }
        }
        // FR-007, FR-022a: người cao tuổi không có tài khoản không phải lỗi gửi
        boolean noRecipient =
                found.isEmpty() && r.recipientGroups().stream().anyMatch(g -> !(g instanceof RecipientGroup.Resident));
        if (noRecipient && v.level() == NotificationLevel.URGENT) {
            fallback(r, at, found);
        }
        if (v.level() == NotificationLevel.LOW && !r.notifyCauser()) {
            r.causedBy().ifPresent(found::removeAccount);
        }
        return new Resolution(found.list(), outcomes, noRecipient, missingCase);
    }

    /** Nhóm loại (a): người theo nhiệm vụ, không có thì theo cách thay (FR-010). */
    private String duty(RecipientGroup.DutyGroup d, NotificationRequest r, Instant at, Recipients found) {
        if (found.addStaff(eligible(dutyStaff(d, at), roles(d)), basis(d))) {
            return HAS_RECIPIENTS;
        }
        String substitute = "SUBSTITUTE:" + basis(d);
        Substitution substitution = d.substitution() == null ? new Substitution.Default() : d.substitution();
        return switch (substitution) {
            case Substitution.NoSubstitute n -> SKIPPED_NO_SUBSTITUTE;
            case Substitution.Designated des ->
                found.addStaff(designated(des.substitute(), at), substitute) ? SUBSTITUTED : NONE;
            case Substitution.Default def -> {
                Optional<Long> floor = floorOf(d).or(r::floorAreaId);
                for (SubstitutionStep step : SubstitutionChain.steps(d)) {
                    if (found.addStaff(step(step, floor, at), substitute)) {
                        yield SUBSTITUTED;
                    }
                }
                yield NONE;
            }
        };
    }

    private List<Long> dutyStaff(RecipientGroup.DutyGroup d, Instant at) {
        Optional<AssignmentApi> a = Optional.ofNullable(assignments.getIfAvailable());
        Optional<DutyApi> du = Optional.ofNullable(duties.getIfAvailable());
        return switch (d) {
            case RecipientGroup.AssignedNurse n ->
                a.map(x -> x.staffAssignedToResident(n.residentId(), at)).orElse(List.of());
            case RecipientGroup.AssignedStaff s ->
                Stream.concat(
                                s.residentId().stream()
                                        .flatMap(id ->
                                                a.stream().flatMap(x -> x.staffAssignedToResident(id, at).stream())),
                                s.roomId().stream()
                                        .flatMap(id -> a.stream().flatMap(x -> x.staffAssignedToRoom(id, at).stream())))
                        .distinct()
                        .toList();
            case RecipientGroup.ShiftLead l ->
                du.flatMap(x -> x.shiftLead(l.floorAreaId(), at)).stream().toList();
            case RecipientGroup.FloorManager f ->
                du.flatMap(x -> x.assignedFloorManager(f.floorAreaId(), at)).stream()
                        .toList();
            case RecipientGroup.DepartmentLead dl ->
                du.flatMap(x -> x.departmentLead(dl.departmentId())).stream().toList();
            case RecipientGroup.TripLeader t ->
                du.flatMap(x -> x.groupLeader(t.tripId())).stream().toList();
            case RecipientGroup.OnShiftStaff o ->
                certified(
                        Optional.ofNullable(shifts.getIfAvailable())
                                .map(x -> x.staffOnShift(o.floorAreaId(), at))
                                .orElse(List.of()),
                        o.certificates(),
                        at);
            case RecipientGroup.OnDutyDoctor o ->
                Optional.ofNullable(shifts.getIfAvailable())
                        .map(x -> x.onDutyDoctors(at))
                        .orElse(List.of());
        };
    }

    /** Chứng chỉ nguồn yêu cầu (005 FR-043): mọi loại còn hiệu lực ngày gửi; không có nguồn giấy phép thì không ai đủ. */
    private List<Long> certified(List<Long> staffIds, Set<String> certificates, Instant at) {
        if (certificates.isEmpty()) {
            return staffIds;
        }
        LicenseApi api = licenses.getIfAvailable();
        LocalDate date = LocalDate.ofInstant(at, BusinessTime.ZONE);
        return staffIds.stream()
                .filter(id -> api != null
                        && certificates.stream()
                                .allMatch(type -> api.valid(id, type, date).isPresent()))
                .toList();
    }

    /** Vai trò giới hạn của nhóm; rỗng là không giới hạn. */
    private static Set<Role> roles(RecipientGroup.DutyGroup d) {
        return switch (d) {
            case RecipientGroup.AssignedNurse n -> Set.of(Role.NURSING_STAFF);
            case RecipientGroup.AssignedStaff s -> s.role().map(Set::of).orElse(Set.of());
            case RecipientGroup.OnShiftStaff o -> o.roles();
            default -> Set.of();
        };
    }

    private static Optional<Long> floorOf(RecipientGroup.DutyGroup d) {
        return switch (d) {
            case RecipientGroup.ShiftLead l -> Optional.of(l.floorAreaId());
            case RecipientGroup.FloorManager f -> Optional.of(f.floorAreaId());
            case RecipientGroup.OnShiftStaff o -> Optional.of(o.floorAreaId());
            default -> Optional.empty();
        };
    }

    private List<AccountView> step(SubstitutionStep step, Optional<Long> floor, Instant at) {
        if (step == SubstitutionStep.MANAGERS) {
            return accounts.getObject().activeWithRole(Role.NURSING_HOME_MANAGER);
        }
        DutyApi du = duties.getIfAvailable();
        if (floor.isEmpty() || du == null) {
            return List.of();
        }
        Optional<Long> staffId = switch (step) {
            case SHIFT_LEAD -> du.shiftLead(floor.get(), at);
            case FLOOR_MANAGER -> du.assignedFloorManager(floor.get(), at);
            case FLOOR_MANAGER_SUBSTITUTE -> du.floorManagerSubstitute(floor.get(), at);
            case MANAGERS -> Optional.empty();
        };
        return eligible(staffId.stream().toList(), Set.of());
    }

    /** Người thay chỉ định: dùng thẳng nhóm đó, không theo chuỗi thay tiếp (FR-010). */
    private List<AccountView> designated(RecipientGroup g, Instant at) {
        return g instanceof RecipientGroup.DutyGroup d ? eligible(dutyStaff(d, at), roles(d)) : direct(g);
    }

    /** Nhóm (b), (c), (d) nhân viên: tài khoản theo vai trò, theo danh sách 000 truyền, theo hồ sơ nhân viên. */
    private List<AccountView> direct(RecipientGroup g) {
        AccountApi api = accounts.getObject();
        return switch (g) {
            case RecipientGroup.FacilityRole f -> api.activeWithRole(f.role());
            case RecipientGroup.Approvers a ->
                a.accountIds().stream().flatMap(id -> api.byId(id).stream()).toList();
            case RecipientGroup.Staff s -> api.byStaff(s.staffId()).stream().toList();
            default -> List.of();
        };
    }

    /**
     * FR-008: nhóm loại (a) không gồm nhân viên Nghỉ việc, tài khoản Đã khóa, Không hoạt động; Khóa tạm vẫn nhận. Nhân viên
     * không có tài khoản không nhận được thông báo trong ứng dụng.
     */
    private List<AccountView> eligible(List<Long> staffIds, Set<Role> roles) {
        AccountApi api = accounts.getObject();
        StaffProfileApi staff = profiles.getIfAvailable();
        return staffIds.stream()
                .filter(id -> staff == null || staff.status(id, time.today()) != StaffStatus.RESIGNED)
                .flatMap(id -> api.byStaff(id).stream())
                .filter(a -> a.status() == AccountStatus.ACTIVE || a.status() == AccountStatus.TEMPORARILY_LOCKED)
                .filter(a -> roles.isEmpty() || !Collections.disjoint(a.roles(), roles))
                .toList();
    }

    /**
     * FR-011: Khẩn cấp không có ai thì mọi Quản lý viện và Trưởng tầng nhận thay; Trưởng tầng không được giao tầng của người
     * cao tuổi chỉ thấy thông tin nhận dạng.
     */
    private void fallback(NotificationRequest r, Instant at, Recipients found) {
        found.addStaff(accounts.getObject().activeWithRole(Role.NURSING_HOME_MANAGER), NO_RECIPIENT_FALLBACK);
        DutyApi du = duties.getIfAvailable();
        Optional<Long> ownFloorManager =
                du == null ? Optional.empty() : r.floorAreaId().flatMap(f -> du.assignedFloorManager(f, at));
        for (AccountView fm : accounts.getObject().activeWithRole(Role.FLOOR_MANAGER)) {
            boolean outOfScope =
                    r.residentId().isPresent() && !ownFloorManager.equals(Optional.ofNullable(fm.staffId()));
            found.addStaff(List.of(fm), NO_RECIPIENT_FALLBACK, outOfScope);
        }
    }

    /** Nhóm (f): tài khoản người cao tuổi Hoạt động hoặc Khóa tạm (như FR-008); không có thì không có người nhận. */
    private boolean resident(RecipientGroup.Resident g, Recipients found) {
        return accounts.getObject()
                .byResident(g.residentId())
                .filter(a -> a.status() == AccountStatus.ACTIVE || a.status() == AccountStatus.TEMPORARILY_LOCKED)
                .map(a -> {
                    found.addResident(g.residentId(), a.accountId(), basis(g));
                    return true;
                })
                .orElse(false);
    }

    private record FamilyResult(boolean added, boolean missingCase) {}

    /**
     * Nhóm người thân (d), (e): chặn sau qua đời (FR-015), lọc khẩn cấp (FR-014, Q-96), phạm vi đồng ý (FR-014, FR-014a),
     * phần nội dung (FR-013).
     */
    private FamilyResult family(RecipientGroup g, Long residentId, ValidatedRequest v, Recipients found) {
        FamilyNotificationPort port = family.getIfAvailable();
        if (port == null || afterDeath(v.request(), residentId)) {
            return new FamilyResult(false, false);
        }
        List<NotifiableRelationshipView> relations =
                residentId == null ? List.of() : port.activeRelationships(residentId);
        List<Candidate> candidates = switch (g) {
            case RecipientGroup.PrimaryContact p -> related(relations, NotifiableRelationshipView::primaryContact);
            case RecipientGroup.Representative p -> related(relations, NotifiableRelationshipView::representative);
            case RecipientGroup.FamilyWithRight p ->
                related(relations, rel -> rel.rights().contains(p.right()));
            case RecipientGroup.FamilyMember m ->
                List.of(relations.stream()
                        .filter(rel -> rel.familyMemberId() == m.familyMemberId())
                        .findFirst()
                        .map(rel -> new Candidate(rel, true))
                        .orElseGet(() -> unrelated(port.contact(m.familyMemberId()), m.familyMemberId())));
            default -> List.of();
        };
        NotificationRequest r = v.request();
        boolean added = false;
        boolean missingCase = false;
        boolean notificationScope = residentId != null && facts.notificationScope(residentId);
        for (Candidate c : candidates) {
            NotifiableRelationshipView rel = c.relation();
            if (v.level() == NotificationLevel.URGENT
                    && !rel.primaryContact()
                    && !rel.rights().contains(FamilyRight.RECEIVE_URGENT)) {
                continue;
            }
            ConsentScope.Decision decision = ConsentScope.decide(
                    v.informationSubject().orElse(null),
                    c.related() && notificationScope,
                    r.q354Case().isPresent(),
                    v.level());
            if (decision == ConsentScope.Decision.MISSING_CASE) {
                missingCase = true;
                continue;
            }
            if (decision == ConsentScope.Decision.SKIP) {
                continue;
            }
            boolean healthConsent = c.related() && facts.healthConsent(residentId, rel.familyMemberId());
            Set<InformationType> visible = ContentFilter.visibleParts(
                    found.parts, rel.rights(), rel.representative(), c.related(), healthConsent);
            if (visible.isEmpty()) {
                continue;
            }
            Q354Case q354 = decision == ConsentScope.Decision.SEND_WITH_CASE
                    ? r.q354Case().orElseThrow()
                    : null;
            Long account = rel.portalAccountActive() ? rel.portalAccountId().orElse(null) : null;
            found.addFamily(rel.familyMemberId(), account, basis(g), visible, q354);
            added = true;
        }
        return new FamilyResult(added, missingCase);
    }

    /**
     * FR-015: yêu cầu phát sinh sau thời điểm qua đời không gửi người thân, trừ việc sau qua đời của 004; tin báo qua đời
     * phát sinh cùng thời điểm nên vẫn gửi.
     */
    private boolean afterDeath(NotificationRequest r, Long residentId) {
        return residentId != null
                && !r.afterDeath()
                && facts.deathAt(residentId).filter(r.occurredAt()::isAfter).isPresent();
    }

    private record Candidate(NotifiableRelationshipView relation, boolean related) {}

    private static List<Candidate> related(
            List<NotifiableRelationshipView> relations,
            java.util.function.Predicate<NotifiableRelationshipView> match) {
        return relations.stream()
                .filter(match)
                .map(rel -> new Candidate(rel, true))
                .toList();
    }

    /** Người thân gắn bản ghi nguồn nhưng không có quan hệ với người cao tuổi: chỉ phần chung (Q-112). */
    private static Candidate unrelated(Optional<FamilyContactView> contact, long familyMemberId) {
        return new Candidate(
                new NotifiableRelationshipView(
                        familyMemberId,
                        false,
                        false,
                        Set.of(),
                        contact.flatMap(FamilyContactView::portalAccountId),
                        contact.map(FamilyContactView::portalAccountActive).orElse(false),
                        contact.flatMap(FamilyContactView::phoneNumber)),
                false);
    }

    /** Căn cứ ghi vào thông báo: tên nhóm, kèm căn cứ của module nguồn với nhóm (d). */
    private static String basis(RecipientGroup g) {
        return switch (g) {
            case RecipientGroup.FacilityRole f -> "FacilityRole:" + f.role();
            case RecipientGroup.Approvers a -> "Approvers:" + a.requestType();
            case RecipientGroup.Staff s -> "Staff:" + s.basis();
            case RecipientGroup.FamilyMember m -> "FamilyMember:" + m.basis();
            case RecipientGroup.FamilyWithRight f -> "FamilyWithRight:" + f.right();
            default -> g.getClass().getSimpleName();
        };
    }

    /** Người nhận đang gộp theo (loại, id) (FR-012). */
    private static final class Recipients {

        private final Set<InformationType> parts;
        private final Map<String, Builder> byKey = new LinkedHashMap<>();

        Recipients(Set<InformationType> parts) {
            this.parts = parts;
        }

        boolean addStaff(List<AccountView> staffAccounts, String basis) {
            return addStaff(staffAccounts, basis, false);
        }

        boolean addStaff(List<AccountView> staffAccounts, String basis, boolean identityOnly) {
            boolean added = false;
            for (AccountView a : staffAccounts) {
                if (a.staffId() == null) {
                    continue;
                }
                Builder b = byKey.computeIfAbsent(
                        "STAFF:" + a.staffId(),
                        k -> new Builder(RecipientType.STAFF, a.staffId(), a.accountId(), identityOnly));
                b.merge(basis, parts, identityOnly, null);
                added = true;
            }
            return added;
        }

        void addFamily(long familyMemberId, Long accountId, String basis, Set<InformationType> visible, Q354Case q354) {
            Builder b = byKey.computeIfAbsent(
                    "FAMILY_MEMBER:" + familyMemberId,
                    k -> new Builder(RecipientType.FAMILY_MEMBER, familyMemberId, accountId, false));
            b.merge(basis, visible, false, q354);
        }

        void addResident(long residentId, long accountId, String basis) {
            byKey.computeIfAbsent(
                            "RESIDENT:" + residentId,
                            k -> new Builder(RecipientType.RESIDENT, residentId, accountId, false))
                    .merge(basis, parts, false, null);
        }

        void removeAccount(long accountId) {
            byKey.values().removeIf(b -> b.accountId != null && b.accountId == accountId);
        }

        boolean isEmpty() {
            return byKey.isEmpty();
        }

        List<ResolvedRecipient> list() {
            return byKey.values().stream().map(Builder::build).toList();
        }
    }

    private static final class Builder {

        private final RecipientType type;
        private final long id;
        private final Long accountId;
        private final Set<String> basis = new LinkedHashSet<>();
        private final Set<InformationType> visible = EnumSet.noneOf(InformationType.class);
        private boolean identityOnly;
        private Q354Case q354;

        Builder(RecipientType type, long id, Long accountId, boolean identityOnly) {
            this.type = type;
            this.id = id;
            this.accountId = accountId;
            this.identityOnly = identityOnly;
        }

        void merge(String basis, Set<InformationType> parts, boolean identityOnly, Q354Case q354) {
            this.basis.add(basis);
            this.visible.addAll(parts);
            this.identityOnly &= identityOnly;
            if (q354 != null) {
                this.q354 = q354;
            }
        }

        ResolvedRecipient build() {
            return new ResolvedRecipient(
                    type, id, accountId, List.copyOf(basis), Set.copyOf(visible), identityOnly, q354);
        }
    }
}
