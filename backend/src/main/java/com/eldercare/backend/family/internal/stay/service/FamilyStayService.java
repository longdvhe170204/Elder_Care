package com.eldercare.backend.family.internal.stay.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.data.TechnicalLocks;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.ReasonRequiredException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.api.events.FamilyStayNotice;
import com.eldercare.backend.family.internal.relationship.service.RelationshipQueries;
import com.eldercare.backend.family.internal.stay.domain.FamilyStay;
import com.eldercare.backend.family.internal.stay.domain.FamilyStay.Status;
import com.eldercare.backend.incident.api.IsolationApi;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.roombedallocation.api.BedStatusApi;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import com.eldercare.backend.roombedallocation.api.dto.BedStatus;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Lượt người thân ở lại (012 T035; US8; FR-040 → FR-045, BR-M10-04; research F-09). Bảng trạng thái FR-045; ai làm lệnh
 * nào theo Phụ lục 27 chú thích 20: Hành chính đăng ký, gia hạn, hủy; Trưởng tầng được giao của tầng xác nhận, từ chối
 * (tầng không có thì Quản lý viện); Hành chính, Trưởng tầng bắt đầu, kết thúc. Lệnh xét chồng thời gian chạy trong
 * transaction riêng mở đầu bằng khóa người cao tuổi và khóa người thân (REPEATABLE READ lập ảnh chụp ở lần đọc đầu).
 * Không tạo khoản chi phí, không tự kết thúc lượt (Q-306).
 */
@Service
public class FamilyStayService {

    static final String RESIDENT_ENTITY = "M01.RESIDENT";
    static final Set<String> HOLDING = Set.of(Status.CONFIRMED.name(), Status.STAYING.name());
    static final Set<String> OPEN =
            Set.of(Status.PENDING_CONFIRMATION.name(), Status.CONFIRMED.name(), Status.STAYING.name());
    private static final AuditAction REGISTERED = new AuditAction("FAMILY_STAY_REGISTERED", false);
    private static final AuditAction CHANGED = new AuditAction("FAMILY_STAY_STATUS_CHANGED", false);

    /** Thân đăng ký ({@code FamilyStayRegistration}). */
    public record Registration(
            long familyMemberId,
            long residentId,
            Instant expectedStartAt,
            Instant expectedEndAt,
            String location,
            String reason,
            boolean mealRegistered) {}

    private final SimpleRecordApi records;
    private final RecordEventApi recordEvents;
    private final TechnicalLocks locks;
    private final RelationshipQueries relationships;
    private final PermissionApi permissions;
    private final ScopeApi scopes;
    private final ParameterApi parameters;
    private final ResidentApi residents;
    private final ResidentLocationApi locations;
    private final ObjectProvider<IsolationApi> isolation;
    private final ObjectProvider<BedStatusApi> beds;
    private final AuditLogApi auditLog;
    private final StayNights nights;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> currentUser;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate tx;

    FamilyStayService(
            SimpleRecordApi records,
            RecordEventApi recordEvents,
            TechnicalLocks locks,
            RelationshipQueries relationships,
            PermissionApi permissions,
            ScopeApi scopes,
            ParameterApi parameters,
            ResidentApi residents,
            ResidentLocationApi locations,
            ObjectProvider<IsolationApi> isolation,
            ObjectProvider<BedStatusApi> beds,
            AuditLogApi auditLog,
            StayNights nights,
            BusinessTime time,
            ObjectProvider<CurrentUser> currentUser,
            ApplicationEventPublisher events,
            PlatformTransactionManager transactions) {
        this.records = records;
        this.recordEvents = recordEvents;
        this.locks = locks;
        this.relationships = relationships;
        this.permissions = permissions;
        this.scopes = scopes;
        this.parameters = parameters;
        this.residents = residents;
        this.locations = locations;
        this.isolation = isolation;
        this.beds = beds;
        this.auditLog = auditLog;
        this.nights = nights;
        this.time = time;
        this.currentUser = currentUser;
        this.events = events;
        this.tx = new TransactionTemplate(transactions);
    }

    /** Đăng ký ở lại (FR-040, FR-041): Hành chính; lượt mới Chờ xác nhận, báo Trưởng tầng được giao của tầng. */
    @BusinessCommand(requiresReason = false)
    public SimpleRecord<FamilyStay> register(Registration r) {
        CurrentUser user = currentUser.getObject();
        authorize(user, Operation.CREATE, residentTarget(r.residentId()));
        requireRole(user, Role.OFFICE_STAFF);
        if (r.expectedStartAt() == null
                || r.expectedEndAt() == null
                || !r.expectedEndAt().isAfter(r.expectedStartAt())) {
            throw new InvalidValueException("Thời gian kết thúc dự kiến phải sau thời gian bắt đầu");
        }
        if (blank(r.location()) || blank(r.reason())) {
            throw new InvalidValueException("Ghi vị trí và lý do ở lại");
        }
        if (relationships.active(r.familyMemberId(), r.residentId()).isEmpty()) {
            throw new InvalidValueException("Người thân không có quan hệ Hiệu lực với người cao tuổi");
        }
        return tx.execute(s -> {
            lock(r.residentId(), r.familyMemberId());
            Instant now = time.now();
            requirePresentAndNotIsolated(r.residentId(), now);
            requireResidentLimit(r.residentId(), null, r.expectedStartAt(), r.expectedEndAt());
            requireMemberFree(r.familyMemberId(), null, r.expectedStartAt(), r.expectedEndAt());
            SimpleRecord<FamilyStay> created = records.create(
                    FamilyStay.KIND,
                    new NewSimpleRecord<>(
                            r.residentId(),
                            new EntityRef(RESIDENT_ENTITY, r.residentId()),
                            Status.PENDING_CONFIRMATION.name(),
                            FamilyStay.registered(
                                    r.familyMemberId(),
                                    r.location().strip(),
                                    r.reason().strip(),
                                    r.mealRegistered(),
                                    r.expectedStartAt(),
                                    r.expectedEndAt()),
                            null,
                            null,
                            null,
                            user.accountId(),
                            false));
            auditLog.write(AuditLogEntry.builder()
                    .action(REGISTERED)
                    .entity(FamilyStay.KIND.code(), created.id())
                    .resident(r.residentId())
                    .after(Map.of("status", created.status()))
                    .build());
            events.publishEvent(notice(created, FamilyStayNotice.Kind.REGISTERED, now));
            return created;
        });
    }

    /** Lượt ở lại theo người cao tuổi; người thân chỉ thấy lượt của mình. */
    @Transactional(readOnly = true)
    public List<SimpleRecord<FamilyStay>> list(Long residentId) {
        CurrentUser user = currentUser.getObject();
        List<SimpleRecord<FamilyStay>> rows;
        if (residentId != null) {
            authorize(user, Operation.VIEW, residentTarget(residentId));
            rows = records.listByResident(FamilyStay.KIND, residentId, Set.of());
        } else if (user.type() == AccountType.FAMILY_MEMBER) {
            authorizeRole(user, Operation.VIEW);
            rows = relationships.activeOfFamilyMember(user.familyMemberId().orElseThrow()).stream()
                    .flatMap(r -> records.listByResident(FamilyStay.KIND, r.residentId(), Set.of()).stream())
                    .toList();
        } else {
            // Lớp vai trò ở đây, phạm vi lọc từng lượt (Target.UNSPECIFIED chỉ phạm vi toàn viện đạt)
            authorizeRole(user, Operation.VIEW);
            rows = records.listByStatus(FamilyStay.KIND, Set.of(enumNames())).stream()
                    .filter(s -> permissions
                            .hasPermission(
                                    user,
                                    new PermissionKey(Function.FAMILY_STAY, Operation.VIEW),
                                    residentTarget(s.residentId()))
                            .allowed())
                    .toList();
        }
        Long self =
                user.type() == AccountType.FAMILY_MEMBER ? user.familyMemberId().orElseThrow() : null;
        return rows.stream()
                .filter(s -> self == null || s.content().familyMemberId() == self)
                .sorted(Comparator.comparing(
                                (SimpleRecord<FamilyStay> s) -> s.content().expectedStartAt())
                        .thenComparing(SimpleRecord::id))
                .toList();
    }

    /** Số đêm của lượt (FR-043): lượt đang mở tính tới hiện tại. */
    public int nightCount(FamilyStay stay) {
        if (stay.actualStartAt() == null) {
            return 0;
        }
        return com.eldercare.backend.family.internal.stay.domain.NightCount.count(
                stay.actualStartAt(), stay.actualEndAt() == null ? time.now() : stay.actualEndAt());
    }

    /**
     * Xác nhận (FR-040, FR-041, FR-045): Trưởng tầng được giao của tầng, tầng không có thì Quản lý viện; kiểm lại FR-041
     * và giới hạn CFG-M10-10; phòng có từ hai giường đang dùng thì bắt buộc ý kiến người cùng phòng.
     */
    @BusinessCommand(requiresReason = false)
    public SimpleRecord<FamilyStay> confirm(long stayId, Map<String, Object> roommateOpinion) {
        CurrentUser user = currentUser.getObject();
        SimpleRecord<FamilyStay> stay = records.require(FamilyStay.KIND, stayId);
        requireConfirmer(user, stay.residentId());
        return tx.execute(s -> {
            lock(stay.residentId(), stay.content().familyMemberId());
            SimpleRecord<FamilyStay> current = records.lockForUpdate(FamilyStay.KIND, stayId);
            requireStatus(current, Status.PENDING_CONFIRMATION);
            Instant now = time.now();
            FamilyStay content = current.content();
            requirePresentAndNotIsolated(current.residentId(), now);
            requireResidentLimit(current.residentId(), stayId, content.from(), content.to());
            boolean opinionGiven = roommateOpinion != null && !roommateOpinion.isEmpty();
            if (!opinionGiven && sharedRoom(current.residentId(), now)) {
                throw conflict(
                        "ROOMMATE_OPINION_REQUIRED",
                        "Phòng có từ hai giường đang dùng: ghi đã hỏi ý kiến người cùng phòng và kết quả");
            }
            SimpleRecord<FamilyStay> after = change(
                    current,
                    Status.CONFIRMED,
                    content.confirmed(user.accountId(), now, opinionGiven ? roommateOpinion : null),
                    false);
            audit(current, after, null);
            events.publishEvent(notice(after, FamilyStayNotice.Kind.CONFIRMED, now));
            return after;
        });
    }

    /** Từ chối (FR-045): như người xác nhận, bắt buộc lý do. */
    @BusinessCommand(requiresReason = false)
    @Transactional
    public SimpleRecord<FamilyStay> reject(long stayId, Reason reason) {
        CurrentUser user = currentUser.getObject();
        SimpleRecord<FamilyStay> current = records.lockForUpdate(FamilyStay.KIND, stayId);
        requireConfirmer(user, current.residentId());
        if (reason == null) {
            throw new ReasonRequiredException();
        }
        requireStatus(current, Status.PENDING_CONFIRMATION);
        Instant now = time.now();
        SimpleRecord<FamilyStay> after = change(
                current, Status.REJECTED, current.content().rejected(user.accountId(), now, reason.value()), true);
        audit(current, after, reason);
        events.publishEvent(notice(after, FamilyStayNotice.Kind.REJECTED, now));
        return after;
    }

    /** Bắt đầu ở lại (FR-041, FR-042): Hành chính, Trưởng tầng; chỉ lượt Đã xác nhận; kiểm lại FR-041. */
    @BusinessCommand(requiresReason = false)
    @Transactional
    public SimpleRecord<FamilyStay> start(Target target, long stayId, Instant at) {
        CurrentUser user = currentUser.getObject();
        authorize(user, Operation.UPDATE, target);
        requireRole(user, Role.OFFICE_STAFF, Role.FLOOR_MANAGER);
        SimpleRecord<FamilyStay> current = records.lockForUpdate(FamilyStay.KIND, stayId);
        if (current.status().equals(Status.PENDING_CONFIRMATION.name())) {
            throw conflict("STATE_CHANGED", "Lượt ở lại chưa được Trưởng tầng xác nhận");
        }
        requireStatus(current, Status.CONFIRMED);
        Instant now = time.now();
        Instant startAt = pastOrNow(at, now);
        requirePresentAndNotIsolated(current.residentId(), now);
        SimpleRecord<FamilyStay> after =
                change(current, Status.STAYING, current.content().started(startAt), false);
        audit(current, after, null);
        return after;
    }

    /** Hủy (FR-045): Hành chính, lượt Chờ xác nhận hoặc Đã xác nhận, bắt buộc lý do. */
    @BusinessCommand(requiresReason = false)
    @Transactional
    public SimpleRecord<FamilyStay> cancel(Target target, long stayId, Reason reason) {
        CurrentUser user = currentUser.getObject();
        authorize(user, Operation.UPDATE, target);
        requireRole(user, Role.OFFICE_STAFF);
        if (reason == null) {
            throw new ReasonRequiredException();
        }
        SimpleRecord<FamilyStay> current = records.lockForUpdate(FamilyStay.KIND, stayId);
        if (!Set.of(Status.PENDING_CONFIRMATION.name(), Status.CONFIRMED.name()).contains(current.status())) {
            throw conflict("STATE_CHANGED", "Lượt ở lại đã đổi trạng thái");
        }
        SimpleRecord<FamilyStay> after =
                change(current, Status.CANCELLED, current.content().cancelled(reason.value()), true);
        audit(current, after, reason);
        return after;
    }

    /** Gia hạn (FR-045): Hành chính, lượt Đang ở lại, bắt buộc lý do; lưu lịch sử dự kiến kết thúc; xét lại chồng. */
    @BusinessCommand(requiresReason = false)
    public SimpleRecord<FamilyStay> extend(Target target, long stayId, Instant newExpectedEndAt, Reason reason) {
        CurrentUser user = currentUser.getObject();
        authorize(user, Operation.UPDATE, target);
        requireRole(user, Role.OFFICE_STAFF);
        if (reason == null) {
            throw new ReasonRequiredException();
        }
        if (newExpectedEndAt == null) {
            throw new InvalidValueException("Ghi thời gian kết thúc dự kiến mới");
        }
        SimpleRecord<FamilyStay> stay = records.require(FamilyStay.KIND, stayId);
        return tx.execute(s -> {
            lock(stay.residentId(), stay.content().familyMemberId());
            SimpleRecord<FamilyStay> current = records.lockForUpdate(FamilyStay.KIND, stayId);
            requireStatus(current, Status.STAYING);
            FamilyStay content = current.content();
            if (!newExpectedEndAt.isAfter(content.actualStartAt())) {
                throw new InvalidValueException("Thời gian kết thúc dự kiến mới phải sau lúc bắt đầu ở lại");
            }
            requireResidentLimit(current.residentId(), stayId, content.actualStartAt(), newExpectedEndAt);
            requireMemberFree(content.familyMemberId(), stayId, content.actualStartAt(), newExpectedEndAt);
            Instant now = time.now();
            recordEvents.add(
                    FamilyStay.Extension.KIND,
                    new EntityRef(FamilyStay.KIND.code(), stayId),
                    new NewRecordEvent<>(
                            new FamilyStay.Extension(content.expectedEndAt(), newExpectedEndAt, reason.value()),
                            null,
                            user.accountId(),
                            false,
                            now,
                            null,
                            now,
                            null,
                            null));
            SimpleRecord<FamilyStay> after = change(current, Status.STAYING, content.extended(newExpectedEndAt), false);
            audit(current, after, reason);
            return after;
        });
    }

    /**
     * Kết thúc ở lại (FR-043, FR-045): Hành chính, Trưởng tầng; thời điểm kết thúc thực tế không trước lúc bắt đầu, không
     * sau lúc ghi. Đêm đã phát mà mốc 00:00 sau thời điểm kết thúc thì báo hủy cho 010.
     */
    @BusinessCommand(requiresReason = false)
    @Transactional
    public SimpleRecord<FamilyStay> end(Target target, long stayId, Instant at) {
        CurrentUser user = currentUser.getObject();
        authorize(user, Operation.UPDATE, target);
        requireRole(user, Role.OFFICE_STAFF, Role.FLOOR_MANAGER);
        SimpleRecord<FamilyStay> current = records.lockForUpdate(FamilyStay.KIND, stayId);
        requireStatus(current, Status.STAYING);
        Instant now = time.now();
        Instant endAt = pastOrNow(at, now);
        if (endAt.isBefore(current.content().actualStartAt())) {
            throw new InvalidValueException("Thời điểm kết thúc phải từ lúc bắt đầu ở lại tới lúc ghi");
        }
        SimpleRecord<FamilyStay> after =
                change(current, Status.ENDED, current.content().ended(endAt), true);
        nights.cancelAfter(after, endAt);
        audit(current, after, null);
        return after;
    }

    /** Đối tượng kiểm quyền của lượt: người cao tuổi của nó. */
    @Transactional(readOnly = true)
    public Target stayTarget(long stayId) {
        SimpleRecord<FamilyStay> stay = records.require(FamilyStay.KIND, stayId);
        return new Target(FamilyStay.KIND.code(), String.valueOf(stayId), stay.residentId(), null);
    }

    static Target residentTarget(long residentId) {
        return new Target(RESIDENT_ENTITY, String.valueOf(residentId), residentId, null);
    }

    FamilyStayNotice notice(SimpleRecord<FamilyStay> stay, FamilyStayNotice.Kind kind, Instant at) {
        return new FamilyStayNotice(
                stay.id(),
                stay.residentId(),
                stay.content().familyMemberId(),
                kind,
                floorOf(stay.residentId(), at),
                at);
    }

    Long floorOf(long residentId, Instant at) {
        return locations.at(residentId, at).map(LocationView::floorId).orElse(null);
    }

    private void lock(long residentId, long familyMemberId) {
        locks.lock("M10.FAMILY_STAY:R:" + residentId);
        locks.lock("M10.FAMILY_STAY:F:" + familyMemberId);
    }

    private void requirePresentAndNotIsolated(long residentId, Instant now) {
        IsolationApi zones = isolation.getIfAvailable();
        if (zones != null && !zones.activeByResident(residentId, now).isEmpty()) {
            throw conflict("ISOLATION_ACTIVE", "Khu của người cao tuổi đang khoanh vùng hoặc cách ly");
        }
        if (residents.status(residentId) != ResidentStatus.IN_RESIDENCE) {
            throw conflict("RESIDENT_NOT_PRESENT", "Người cao tuổi không ở trạng thái Đang lưu trú");
        }
    }

    /** CFG-M10-10: số lượt Đã xác nhận, Đang ở lại chồng thời gian của người cao tuổi (FR-040). */
    private void requireResidentLimit(long residentId, Long self, Instant start, Instant end) {
        long holding = records.listByResident(FamilyStay.KIND, residentId, HOLDING).stream()
                .filter(s -> self == null || s.id() != self)
                .filter(s -> s.content().overlaps(start, end))
                .count();
        if (holding >= parameters.read(ParameterCode.CFG_M10_10).asInt()) {
            throw conflict(
                    "FAMILY_STAY_OVERLAP", "Người cao tuổi đã đủ số lượt người thân ở lại cùng lúc (CFG-M10-10)");
        }
    }

    /** Người thân không có hai lượt Chờ xác nhận, Đã xác nhận, Đang ở lại chồng thời gian (FR-040). */
    private void requireMemberFree(long familyMemberId, Long self, Instant start, Instant end) {
        boolean busy = records.listByStatus(FamilyStay.KIND, OPEN).stream()
                .filter(s -> self == null || s.id() != self)
                .filter(s -> s.content().familyMemberId() == familyMemberId)
                .anyMatch(s -> s.content().overlaps(start, end));
        if (busy) {
            throw conflict("FAMILY_STAY_OVERLAP", "Người thân đã có lượt ở lại khác chồng thời gian");
        }
    }

    private boolean sharedRoom(long residentId, Instant now) {
        BedStatusApi api = beds.getIfAvailable();
        Optional<Long> room = locations.at(residentId, now).map(LocationView::roomId);
        if (api == null || room.isEmpty()) {
            return false;
        }
        Map<BedStatus, Integer> counts =
                api.countByState(new ScopeFilter(false, Set.of(), Set.of(room.get()), Set.of()), now);
        return counts.getOrDefault(BedStatus.OCCUPIED, 0) >= 2;
    }

    /**
     * Người xác nhận, từ chối (Phụ lục 27 chú thích 20): Trưởng tầng được giao của tầng người cao tuổi; tầng không có
     * Trưởng tầng được giao thì Quản lý viện.
     */
    private void requireConfirmer(CurrentUser user, long residentId) {
        authorize(user, Operation.VIEW, residentTarget(residentId));
        Optional<Long> floorManager =
                Optional.ofNullable(floorOf(residentId, time.now())).flatMap(scopes::assignedFloorManager);
        boolean allowed = floorManager
                .map(account -> user.roles().contains(Role.FLOOR_MANAGER) && account == user.accountId())
                .orElseGet(() -> user.roles().contains(Role.NURSING_HOME_MANAGER));
        if (!allowed) {
            throw denied();
        }
    }

    private SimpleRecord<FamilyStay> change(
            SimpleRecord<FamilyStay> current, Status to, FamilyStay content, boolean close) {
        return records.change(
                        FamilyStay.KIND,
                        current.id(),
                        current.version(),
                        new RecordChange<>(to.name(), content, current.effectiveTo(), null, close))
                .after();
    }

    private void audit(SimpleRecord<FamilyStay> before, SimpleRecord<FamilyStay> after, Reason reason) {
        Map<String, Object> afterValues = new HashMap<>();
        afterValues.put("status", after.status());
        AuditLogEntry.Builder entry = AuditLogEntry.builder()
                .action(CHANGED)
                .entity(FamilyStay.KIND.code(), after.id())
                .resident(after.residentId())
                .before(Map.of("status", before.status()))
                .after(afterValues);
        if (reason != null) {
            entry.reason(reason);
        }
        auditLog.write(entry.build());
    }

    private static Instant pastOrNow(Instant at, Instant now) {
        Instant value = at == null ? now : at;
        if (value.isAfter(now)) {
            throw new InvalidValueException("Thời điểm không được sau lúc ghi");
        }
        return value;
    }

    private static String[] enumNames() {
        return java.util.Arrays.stream(Status.values()).map(Enum::name).toArray(String[]::new);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static void requireStatus(SimpleRecord<FamilyStay> stay, Status status) {
        if (!stay.status().equals(status.name())) {
            throw conflict("STATE_CHANGED", "Lượt ở lại đã đổi trạng thái");
        }
    }

    private static BusinessException conflict(String code, String message) {
        return new BusinessException(
                HttpStatus.CONFLICT,
                code,
                message,
                Map.of("unmetConditions", List.of(new UnmetCondition(code, message))));
    }

    private static void requireRole(CurrentUser user, Role... roles) {
        for (Role role : roles) {
            if (user.roles().contains(role)) {
                return;
            }
        }
        throw denied();
    }

    private void authorizeRole(CurrentUser user, Operation operation) {
        if (!permissions.permissions(user).contains(new PermissionKey(Function.FAMILY_STAY, operation))) {
            throw denied();
        }
    }

    private void authorize(CurrentUser user, Operation operation, Target target) {
        PermissionResult result =
                permissions.hasPermission(user, new PermissionKey(Function.FAMILY_STAY, operation), target);
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
