package com.eldercare.backend.family.internal.visit.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.administration.account.api.dto.Target;
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
import com.eldercare.backend.common.data.TechnicalLocks;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.ReasonRequiredException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.internal.relationship.service.RelationshipQueries;
import com.eldercare.backend.family.internal.visit.domain.Visit;
import com.eldercare.backend.family.internal.visit.domain.Visit.Canceller;
import com.eldercare.backend.family.internal.visit.domain.Visit.Companion;
import com.eldercare.backend.family.internal.visit.domain.Visit.Source;
import com.eldercare.backend.family.internal.visit.domain.Visit.Status;
import com.eldercare.backend.family.internal.visit.domain.VisitRegistrationCheck;
import com.eldercare.backend.family.internal.visit.domain.VisitSlot;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Lượt thăm (012 T024; US4; FR-029 → FR-035, FR-039; research F-08). Đăng ký đạt mọi điều kiện thì Đã duyệt ngay và giữ
 * chỗ trong khung (khóa {@code technical_lock} theo ngày, khung; số người đã giữ đếm khi đã khóa); không đạt thì 422 kèm
 * mọi lý do, không tạo lượt (FR-031). Người thân đăng ký cho chính mình khi có quyền đăng ký thăm; Hành chính đăng ký
 * thay người thân có quan hệ Hiệu lực, ghi vào, ghi ra (FR-067).
 */
@Service
public class VisitService {

    static final String RESIDENT_ENTITY = "M01.RESIDENT";
    static final Set<String> HOLDING = Set.of(Status.APPROVED.name(), Status.CHECKED_IN.name());
    private static final Set<String> ALL = Set.of("APPROVED", "CHECKED_IN", "CHECKED_OUT", "NO_SHOW", "CANCELLED");
    private static final AuditAction REGISTERED = new AuditAction("VISIT_REGISTERED", false);
    private static final AuditAction CHANGED = new AuditAction("VISIT_STATUS_CHANGED", false);

    /** Thân đăng ký ({@code VisitRegistration}). */
    public record Registration(
            long residentId,
            Long familyMemberId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            List<Companion> companions) {}

    /** Khung của ngày và số chỗ còn (FR-030; US4 kịch bản 2). */
    public record SlotView(String startTime, String endTime, int capacity, int seatsLeft) {}

    private final SimpleRecordApi records;
    private final TechnicalLocks locks;
    private final VisitRules rules;
    private final VisitPresence presence;
    private final RelationshipQueries relationships;
    private final PermissionApi permissions;
    private final AuditLogApi auditLog;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> currentUser;
    private final TransactionTemplate tx;

    VisitService(
            SimpleRecordApi records,
            TechnicalLocks locks,
            VisitRules rules,
            VisitPresence presence,
            RelationshipQueries relationships,
            PermissionApi permissions,
            AuditLogApi auditLog,
            BusinessTime time,
            ObjectProvider<CurrentUser> currentUser,
            PlatformTransactionManager transactions) {
        this.records = records;
        this.locks = locks;
        this.rules = rules;
        this.presence = presence;
        this.relationships = relationships;
        this.permissions = permissions;
        this.auditLog = auditLog;
        this.time = time;
        this.currentUser = currentUser;
        this.tx = new TransactionTemplate(transactions);
    }

    /** Khung thăm của ngày và số chỗ còn; sức chứa đọc từ CFG-M10-04 hiện hành. */
    @Transactional(readOnly = true)
    public List<SlotView> slots(LocalDate date) {
        CurrentUser user = currentUser.getObject();
        // Khung, số chỗ là của toàn viện; người thân có quyền Xem theo quan hệ, không gắn người cao tuổi nên xét lớp
        // vai trò
        if (user.type() == AccountType.FAMILY_MEMBER) {
            if (!permissions
                    .permissions(user)
                    .contains(new PermissionKey(Function.VISIT_REGISTRATION, Operation.VIEW))) {
                throw denied();
            }
        } else {
            authorize(user, Operation.VIEW, Target.UNSPECIFIED);
        }
        VisitRules.Slots slots = rules.slots();
        List<SimpleRecord<Visit>> holding = holding(date);
        return slots.slots().stream()
                .map(s -> new SlotView(
                        s.start().toString(),
                        s.end().toString(),
                        slots.capacity(),
                        slots.capacity() - reserved(holding, s)))
                .toList();
    }

    /** Đăng ký thăm (FR-029 → FR-031, BR-M10-02). */
    @BusinessCommand(requiresReason = false)
    public SimpleRecord<Visit> register(Registration registration) {
        CurrentUser user = currentUser.getObject();
        long residentId = registration.residentId();
        Target target = residentTarget(residentId);
        authorize(user, Operation.CREATE, target);
        Instant now = time.now();
        long familyMemberId;
        boolean counter = user.type() == AccountType.STAFF;
        if (counter) {
            if (registration.familyMemberId() == null) {
                throw new InvalidValueException("Chọn người thân đăng ký thăm (familyMemberId)");
            }
            familyMemberId = registration.familyMemberId();
            if (relationships.active(familyMemberId, residentId).isEmpty()) {
                throw new InvalidValueException("Người thân không có quan hệ Hiệu lực với người cao tuổi");
            }
        } else if (user.type() == AccountType.FAMILY_MEMBER) {
            familyMemberId = user.familyMemberId().orElseThrow();
            boolean other = registration.familyMemberId() != null && registration.familyMemberId() != familyMemberId;
            if (other || !relationships.rightEnabledAt(familyMemberId, residentId, FamilyRight.REGISTER_VISITS, now)) {
                throw denied();
            }
        } else {
            throw denied();
        }
        List<Companion> companions = companions(registration.companions(), false);
        VisitSlot slot = new VisitSlot(registration.startTime(), registration.endTime());
        VisitRules.Slots slots = rules.slots();
        long member = familyMemberId;
        // Transaction riêng mở đầu bằng khóa khung: MySQL REPEATABLE READ lập ảnh chụp ở lần đọc đầu tiên, đọc trước
        // khi
        // có khóa thì không thấy lượt vừa giữ chỗ của lệnh trước (SC-004)
        return tx.execute(s -> book(user, registration, member, companions, slot, slots, counter));
    }

    private SimpleRecord<Visit> book(
            CurrentUser user,
            Registration registration,
            long familyMemberId,
            List<Companion> companions,
            VisitSlot slot,
            VisitRules.Slots slots,
            boolean counter) {
        if (slots.slots().contains(slot)) {
            locks.lock(slot.lockKey(registration.date()));
        }
        long residentId = registration.residentId();
        int visitorCount = 1 + companions.size();
        Instant now = time.now();
        VisitRules.WindowConfig window = rules.window();
        List<VisitRegistrationCheck.Reason> reasons = VisitRegistrationCheck.check(
                new VisitRegistrationCheck.Request(
                        registration.date(), slot.start(), slot.end(), visitorCount, counter),
                new VisitRegistrationCheck.Facts(
                        slots.slots(),
                        slots.capacity(),
                        reserved(holding(registration.date()), slot),
                        rules.maxVisitors(),
                        window.min().duration(),
                        window.max().duration(),
                        presence.isolated(residentId, now),
                        presence.present(residentId, registration.date(), slot),
                        now));
        if (!reasons.isEmpty()) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    reasons.getFirst().name(),
                    "Đăng ký thăm không đạt điều kiện",
                    Map.of(
                            "unmetConditions",
                            reasons.stream()
                                    .map(r -> new UnmetCondition(r.name(), r.description()))
                                    .toList()));
        }
        SimpleRecord<Visit> created = records.create(
                Visit.KIND,
                new NewSimpleRecord<>(
                        residentId,
                        new EntityRef(RESIDENT_ENTITY, residentId),
                        Status.APPROVED.name(),
                        Visit.registered(
                                familyMemberId,
                                registration.date(),
                                slot.start(),
                                slot.end(),
                                visitorCount,
                                counter ? Source.COUNTER : Source.PORTAL,
                                user.accountId(),
                                companions),
                        registration.date(),
                        registration.date(),
                        null,
                        user.accountId(),
                        false));
        auditLog.write(AuditLogEntry.builder()
                .action(REGISTERED)
                .entity(Visit.KIND.code(), created.id())
                .resident(residentId)
                .after(Map.of("status", created.status(), "visitorCount", visitorCount))
                .build());
        return created;
    }

    /** Lượt thăm theo người cao tuổi, ngày; người thân chỉ thấy lượt của mình (FR-046). */
    @Transactional(readOnly = true)
    public List<SimpleRecord<Visit>> list(Long residentId, LocalDate date) {
        CurrentUser user = currentUser.getObject();
        authorize(user, Operation.VIEW, residentId == null ? Target.UNSPECIFIED : residentTarget(residentId));
        List<SimpleRecord<Visit>> rows;
        if (residentId != null) {
            rows = records.listByResident(Visit.KIND, residentId, Set.of());
        } else if (user.type() == AccountType.FAMILY_MEMBER) {
            rows = relationships.activeOfFamilyMember(user.familyMemberId().orElseThrow()).stream()
                    .flatMap(r -> records.listByResident(Visit.KIND, r.residentId(), Set.of()).stream())
                    .toList();
        } else {
            rows = records.listByStatus(Visit.KIND, ALL);
        }
        Long self =
                user.type() == AccountType.FAMILY_MEMBER ? user.familyMemberId().orElseThrow() : null;
        return rows.stream()
                .filter(v -> date == null || v.content().date().equals(date))
                .filter(v -> self == null || v.content().familyMemberId() == self)
                .sorted(Comparator.comparing(
                                (SimpleRecord<Visit> v) -> v.content().slotStart())
                        .thenComparing(SimpleRecord::id))
                .toList();
    }

    /** Hủy (FR-035): người đăng ký hoặc Hành chính (lý do), trước giờ bắt đầu khung; trả chỗ. */
    @BusinessCommand(requiresReason = false)
    @Transactional
    public SimpleRecord<Visit> cancel(Target target, long visitId, Reason reason) {
        CurrentUser user = currentUser.getObject();
        authorize(user, Operation.UPDATE, target);
        SimpleRecord<Visit> current = records.lockForUpdate(Visit.KIND, visitId);
        Canceller by;
        if (user.type() == AccountType.FAMILY_MEMBER) {
            if (current.content().familyMemberId() != user.familyMemberId().orElseThrow()) {
                throw denied();
            }
            by = Canceller.REGISTRANT;
        } else if (user.type() == AccountType.STAFF) {
            if (reason == null) {
                throw new ReasonRequiredException();
            }
            by = Canceller.OFFICE;
        } else {
            throw denied();
        }
        requireStatus(current, Status.APPROVED);
        if (!time.now().isBefore(current.content().slotStart())) {
            throw new BusinessException(HttpStatus.CONFLICT, "START_TIME_PASSED", "Đã qua giờ bắt đầu khung thăm");
        }
        locks.lock(current.content().slot().lockKey(current.content().date()));
        SimpleRecord<Visit> after = close(
                current,
                Status.CANCELLED,
                current.content().cancelled(by, null, reason == null ? null : reason.value(), user.accountId()));
        audit(current, after, reason);
        return after;
    }

    /** Ghi vào (FR-033): kiểm lại (c), (d); người thực tế vào có họ tên, tổng không quá số đã đăng ký. */
    @BusinessCommand(requiresReason = false)
    @Transactional
    public SimpleRecord<Visit> checkIn(Target target, long visitId, List<Companion> actualVisitors) {
        CurrentUser user = requireStaff();
        authorize(user, Operation.UPDATE, target);
        List<Companion> actual = companions(actualVisitors, true);
        SimpleRecord<Visit> current = records.lockForUpdate(Visit.KIND, visitId);
        requireStatus(current, Status.APPROVED);
        Visit visit = current.content();
        if (1 + actual.size() > visit.visitorCount()) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "TOO_MANY_VISITORS",
                    "Số người thực tế vào vượt số đã đăng ký (" + visit.visitorCount() + ")");
        }
        Instant now = time.now();
        if (presence.isolated(current.residentId(), now)) {
            throw blocked(VisitRegistrationCheck.Reason.ISOLATION_ACTIVE);
        }
        if (!presence.present(current.residentId(), visit.date(), visit.slot())) {
            throw blocked(VisitRegistrationCheck.Reason.RESIDENT_NOT_PRESENT);
        }
        SimpleRecord<Visit> after = records.change(
                        Visit.KIND,
                        visitId,
                        current.version(),
                        new RecordChange<>(
                                Status.CHECKED_IN.name(),
                                visit.checkedIn(entered(visit.companions(), actual), user.accountId(), now),
                                current.effectiveTo(),
                                null,
                                false))
                .after();
        audit(current, after, null);
        return after;
    }

    /** Ghi ra (FR-034): giờ ra thực tế không muộn hơn lúc ghi, không sớm hơn giờ vào. */
    @BusinessCommand(requiresReason = false)
    @Transactional
    public SimpleRecord<Visit> checkOut(Target target, long visitId, Instant checkOutAt) {
        CurrentUser user = requireStaff();
        authorize(user, Operation.UPDATE, target);
        SimpleRecord<Visit> current = records.lockForUpdate(Visit.KIND, visitId);
        requireStatus(current, Status.CHECKED_IN);
        Instant now = time.now();
        Instant at = checkOutAt == null ? now : checkOutAt;
        if (at.isAfter(now) || at.isBefore(current.content().checkedInAt())) {
            throw new InvalidValueException("Giờ ra phải từ giờ vào tới thời điểm ghi");
        }
        SimpleRecord<Visit> after =
                close(current, Status.CHECKED_OUT, current.content().checkedOut(user.accountId(), at));
        audit(current, after, null);
        return after;
    }

    /** Đối tượng kiểm quyền của lượt thăm: người cao tuổi của nó. */
    @Transactional(readOnly = true)
    public Target visitTarget(long visitId) {
        SimpleRecord<Visit> visit = records.require(Visit.KIND, visitId);
        return new Target(Visit.KIND.code(), String.valueOf(visitId), visit.residentId(), null);
    }

    static Target residentTarget(long residentId) {
        return new Target(RESIDENT_ENTITY, String.valueOf(residentId), residentId, null);
    }

    /** Lượt giữ chỗ (Đã duyệt, Đã vào) của ngày. */
    List<SimpleRecord<Visit>> holding(LocalDate date) {
        return records.listByStatus(Visit.KIND, HOLDING).stream()
                .filter(v -> v.content().date().equals(date))
                .toList();
    }

    static int reserved(List<SimpleRecord<Visit>> holding, VisitSlot slot) {
        return holding.stream()
                .filter(v -> v.content().slot().equals(slot))
                .mapToInt(v -> v.content().visitorCount())
                .sum();
    }

    SimpleRecord<Visit> close(SimpleRecord<Visit> current, Status to, Visit content) {
        return records.change(
                        Visit.KIND,
                        current.id(),
                        current.version(),
                        new RecordChange<>(to.name(), content, current.effectiveTo(), null, true))
                .after();
    }

    void audit(SimpleRecord<Visit> before, SimpleRecord<Visit> after, Reason reason) {
        AuditLogEntry.Builder entry = AuditLogEntry.builder()
                .action(CHANGED)
                .entity(Visit.KIND.code(), after.id())
                .resident(after.residentId())
                .before(Map.of("status", before.status()))
                .after(Map.of("status", after.status()));
        if (reason != null) {
            entry.reason(reason);
        }
        auditLog.write(entry.build());
    }

    private static List<Companion> companions(List<Companion> input, boolean entered) {
        List<Companion> result = new ArrayList<>();
        for (Companion c : input == null ? List.<Companion>of() : input) {
            if (c == null || blank(c.fullName()) || blank(c.relationship())) {
                throw new InvalidValueException("Ghi họ tên, quan hệ của từng người đi cùng");
            }
            result.add(new Companion(
                    c.fullName().strip(),
                    c.relationship().strip(),
                    blank(c.phoneNumber()) ? null : c.phoneNumber().strip(),
                    entered,
                    false));
        }
        return result;
    }

    /** Người đã đăng ký đi cùng có trong danh sách thực tế thì vào; người mới được thêm tên lúc vào (FR-033). */
    private static List<Companion> entered(List<Companion> registered, List<Companion> actual) {
        Set<String> actualNames = new HashSet<>();
        actual.forEach(c -> actualNames.add(key(c.fullName())));
        Set<String> registeredNames = new HashSet<>();
        List<Companion> result = new ArrayList<>();
        for (Companion c : registered) {
            registeredNames.add(key(c.fullName()));
            result.add(new Companion(
                    c.fullName(), c.relationship(), c.phoneNumber(), actualNames.contains(key(c.fullName())), false));
        }
        for (Companion c : actual) {
            if (!registeredNames.contains(key(c.fullName()))) {
                result.add(new Companion(c.fullName(), c.relationship(), c.phoneNumber(), true, true));
            }
        }
        return result;
    }

    private static String key(String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static void requireStatus(SimpleRecord<Visit> visit, Status status) {
        if (!visit.status().equals(status.name())) {
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Lượt thăm đã đổi trạng thái");
        }
    }

    private static BusinessException blocked(VisitRegistrationCheck.Reason reason) {
        return new BusinessException(
                HttpStatus.CONFLICT,
                reason.name(),
                reason.description(),
                Map.of("unmetConditions", List.of(new UnmetCondition(reason.name(), reason.description()))));
    }

    private CurrentUser requireStaff() {
        CurrentUser user = currentUser.getObject();
        if (user.type() != AccountType.STAFF) {
            throw denied();
        }
        return user;
    }

    private void authorize(CurrentUser user, Operation operation, Target target) {
        PermissionResult result =
                permissions.hasPermission(user, new PermissionKey(Function.VISIT_REGISTRATION, operation), target);
        if (result.allowed()) {
            return;
        }
        FailedLayer layer = result.failedLayer().orElse(FailedLayer.ROLE);
        if (layer == FailedLayer.SCOPE && !target.equals(Target.UNSPECIFIED)) {
            throw new NotFoundException();
        }
        throw new PermissionDeniedException("PERMISSION_DENIED", "Không có quyền", Map.of("failedLayer", layer.name()));
    }

    private static PermissionDeniedException denied() {
        return new PermissionDeniedException("PERMISSION_DENIED", "Không có quyền", Map.of("failedLayer", "ROLE"));
    }
}
