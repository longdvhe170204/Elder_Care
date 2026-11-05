package com.eldercare.backend.family.internal.pickup.service;

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
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.care.api.DayCareAttendanceApi;
import com.eldercare.backend.care.api.dto.CheckoutType;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.ReasonRequiredException;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.api.events.PickupBlockNotice;
import com.eldercare.backend.family.api.events.PickupExceptionNotice;
import com.eldercare.backend.family.internal.pickup.domain.PickupBlock;
import com.eldercare.backend.family.internal.pickup.domain.PickupException;
import com.eldercare.backend.family.internal.pickup.domain.PickupException.Method;
import com.eldercare.backend.family.internal.pickup.domain.PickupException.Status;
import com.eldercare.backend.family.internal.pickup.domain.PickupRecord;
import com.eldercare.backend.family.internal.pickup.domain.PickupRecord.Basis;
import com.eldercare.backend.family.internal.pickup.domain.PickupType;
import com.eldercare.backend.family.internal.pickup.persistence.PickupRecordStore;
import com.eldercare.backend.family.internal.relationship.domain.FamilyMember;
import com.eldercare.backend.family.internal.relationship.domain.FamilyRelationship;
import com.eldercare.backend.family.internal.relationship.persistence.FamilyResidentLock;
import com.eldercare.backend.family.internal.relationship.service.RelationshipQueries;
import com.eldercare.backend.family.internal.right.domain.SelfReturnFlag;
import com.eldercare.backend.family.internal.right.service.RightChangeRequestService;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
 * Quy trình đón (012 US2; FR-021 → FR-028, BR-M10-03; research F-07): ghi đón chỉ trực tuyến (không có xử lý ngoại
 * tuyến), lần chặn đón, yêu cầu ngoại lệ đón. Mọi lệnh khóa {@link FamilyResidentLock} của người cao tuổi như lệnh đổi
 * quyền, nên bỏ người đón và ghi đón bởi người đó không chạy xen nhau.
 *
 * <p>Ghi đón không đạt vẫn phải để lại lần chặn (FR-023) và trạng thái Hết hạn của ngoại lệ, nên lệnh chạy trong
 * transaction riêng ({@link TransactionTemplate}) rồi mới trả 409; quyền kiểm trước, ngoài transaction. Người thực hiện
 * phải có quyền Tạo của lệnh nguồn theo loại đón (FR-022, chú thích ¹⁹ Phụ lục 27) và quyền Tạo của "Quy trình đón".
 */
@Service
public class PickupService {

    private static final String RESIDENT_ENTITY = "M01.RESIDENT";
    private static final AuditAction RECORDED = new AuditAction("PICKUP_RECORDED", false);
    private static final AuditAction EXCEPTION_CREATED = new AuditAction("PICKUP_EXCEPTION_CREATED", false);
    private static final AuditAction EXCEPTION_DECIDED = new AuditAction("PICKUP_EXCEPTION_DECIDED", false);

    /** Thân lệnh ghi đón ({@code RecordPickup}). */
    public record RecordCommand(
            PickupType type,
            Long familyMemberId,
            Long exceptionId,
            String presentedIdDocumentType,
            String presentedIdDocumentNumber,
            long handoverStaffId) {}

    /** Bản ghi đón kèm trạng thái suy ra. */
    public record PickupView(PickupRecord record, PickupRecord.Status status) {}

    private record Attempt(PickupView created, PickupBlock.Reason blocked) {}

    private final PickupRecordStore store;
    private final PickupOutcomes outcomes;
    private final RecordEventApi recordEvents;
    private final SimpleRecordApi records;
    private final FamilyResidentLock lock;
    private final RelationshipQueries relationships;
    private final ResidentApi residents;
    private final ResidentLocationApi locations;
    private final ObjectProvider<DayCareAttendanceApi> attendance;
    private final PermissionApi permissions;
    private final ParameterApi parameters;
    private final AuditLogApi auditLog;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> currentUser;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate tx;

    PickupService(
            PickupRecordStore store,
            PickupOutcomes outcomes,
            RecordEventApi recordEvents,
            SimpleRecordApi records,
            FamilyResidentLock lock,
            RelationshipQueries relationships,
            ResidentApi residents,
            ResidentLocationApi locations,
            ObjectProvider<DayCareAttendanceApi> attendance,
            PermissionApi permissions,
            ParameterApi parameters,
            AuditLogApi auditLog,
            BusinessTime time,
            ObjectProvider<CurrentUser> currentUser,
            ApplicationEventPublisher events,
            PlatformTransactionManager transactions) {
        this.store = store;
        this.outcomes = outcomes;
        this.recordEvents = recordEvents;
        this.records = records;
        this.lock = lock;
        this.relationships = relationships;
        this.residents = residents;
        this.locations = locations;
        this.attendance = attendance;
        this.permissions = permissions;
        this.parameters = parameters;
        this.auditLog = auditLog;
        this.time = time;
        this.currentUser = currentUser;
        this.events = events;
        this.tx = new TransactionTemplate(transactions);
    }

    /** Bản ghi đón của người cao tuổi, mới nhất trước (nhân viên có quyền xem "Quy trình đón"). */
    @RequiresPermission(function = Function.PICKUP, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<PickupView> list(Target target) {
        return store.ofResident(target.residentId()).stream()
                .map(r -> new PickupView(r, outcomes.status(r.id())))
                .toList();
    }

    /**
     * Ghi đón (FR-021, FR-026): người đón thuộc danh sách tại thời điểm hoặc có ngoại lệ Hiệu lực, giấy tờ xuất trình khớp
     * số đã đăng ký; Tự về cần dấu "được tự về" đang bật. Viện đưa về ghi điểm danh về của 005 trong cùng lệnh (Q-295).
     * Không đạt: ghi lần chặn, báo Trưởng tầng, trả 409 {@code PICKUP_BLOCKED} ({@code PICKUP_EXCEPTION_EXPIRED} khi
     * ngoại lệ đã hết hạn) kèm {@code blockReason}.
     */
    @BusinessCommand(requiresReason = false)
    public PickupView record(long residentId, RecordCommand command) {
        CurrentUser user = currentUser.getObject();
        requireStaff(user);
        validate(command);
        Target target = residentTarget(residentId);
        PermissionResult source = permissions.hasPermission(
                user, new PermissionKey(command.type().sourceFunction(), Operation.CREATE), target);
        if (!source.allowed()) {
            throw sourceDenied(source);
        }
        authorize(user, Operation.CREATE, target);
        Attempt attempt = tx.execute(s -> attempt(user, residentId, command));
        if (attempt.blocked() != null) {
            PickupBlock.Reason reason = attempt.blocked();
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    reason == PickupBlock.Reason.EXCEPTION_EXPIRED ? "PICKUP_EXCEPTION_EXPIRED" : "PICKUP_BLOCKED",
                    "Lượt đón bị chặn",
                    Map.of("blockReason", reason.name()));
        }
        return attempt.created();
    }

    private Attempt attempt(CurrentUser user, long residentId, RecordCommand command) {
        lock.lock(residentId);
        if (RightChangeRequestService.isFinal(residents.status(residentId))) {
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Người cao tuổi đã ở trạng thái cuối");
        }
        // DATETIME(6) làm tròn phần dưới micro giây: cắt trước để lệnh nguồn gọi cùng thời điểm vẫn thấy bản ghi
        Instant now = time.now().truncatedTo(ChronoUnit.MICROS);
        Basis basis;
        String pickerName = null;
        SimpleRecord<PickupException> exception = null;
        if (command.type() == PickupType.SELF_RETURN) {
            if (!selfReturnOn(residentId)) {
                return block(user, residentId, command, PickupBlock.Reason.SELF_RETURN_OFF, now);
            }
            basis = Basis.SELF_RETURN;
        } else if (command.exceptionId() != null) {
            exception = records.lockForUpdate(PickupException.KIND, command.exceptionId());
            if (exception.residentId() == null || exception.residentId() != residentId) {
                throw new NotFoundException();
            }
            PickupException content = exception.content();
            Status status = Status.valueOf(exception.status());
            if (status == Status.EFFECTIVE && !now.isBefore(content.expiresAt())) {
                close(exception, Status.EXPIRED, content);
                status = Status.EXPIRED;
            }
            if (status == Status.EXPIRED) {
                return block(user, residentId, command, PickupBlock.Reason.EXCEPTION_EXPIRED, now);
            }
            if (status != Status.EFFECTIVE) {
                return block(user, residentId, command, PickupBlock.Reason.NOT_ON_LIST, now);
            }
            if (!content.idDocumentNumber().equals(command.presentedIdDocumentNumber())) {
                return block(user, residentId, command, PickupBlock.Reason.ID_DOCUMENT_MISMATCH, now);
            }
            basis = content.basis();
            pickerName = content.fullName();
        } else {
            if (!relationships.rightEnabledAt(command.familyMemberId(), residentId, FamilyRight.PICKUP_ALLOWED, now)) {
                return block(user, residentId, command, PickupBlock.Reason.NOT_ON_LIST, now);
            }
            FamilyMember member =
                    relationships.familyMember(command.familyMemberId()).orElseThrow(NotFoundException::new);
            if (member.idDocumentNumber() == null
                    || !member.idDocumentNumber().equals(command.presentedIdDocumentNumber())) {
                return block(user, residentId, command, PickupBlock.Reason.ID_DOCUMENT_MISMATCH, now);
            }
            basis = Basis.PICKUP_LIST;
            pickerName = member.fullName();
        }
        boolean selfReturn = basis == Basis.SELF_RETURN;
        Instant expiresAt = command.type() == PickupType.INSTITUTE_RETURN
                ? null
                : now.plus(parameters.read(ParameterCode.CFG_M10_09).asDuration());
        PickupRecord record = new PickupRecord(
                0,
                residentId,
                command.type(),
                selfReturn || exception != null ? null : command.familyMemberId(),
                basis,
                exception == null ? null : exception.id(),
                pickerName,
                selfReturn ? null : command.presentedIdDocumentType(),
                selfReturn ? null : command.presentedIdDocumentNumber(),
                now,
                command.handoverStaffId(),
                user.accountId(),
                expiresAt);
        long id = store.insert(record, now);
        if (exception != null) {
            records.change(
                    PickupException.KIND,
                    exception.id(),
                    exception.version(),
                    new RecordChange<>(Status.USED.name(), exception.content().usedBy(id), null, null, true));
        }
        if (command.type() == PickupType.INSTITUTE_RETURN) {
            attendance
                    .getObject()
                    .checkOut(
                            residentId,
                            LocalDate.ofInstant(now, BusinessTime.ZONE),
                            id,
                            CheckoutType.INSTITUTE_RETURN,
                            now,
                            user);
        }
        auditLog.write(AuditLogEntry.builder()
                .action(RECORDED)
                .entity(PickupRecord.ENTITY_TYPE, id)
                .resident(residentId)
                .after(Map.of("type", command.type().name(), "basis", basis.name()))
                .build());
        PickupRecord saved = store.find(id).orElseThrow();
        return new Attempt(new PickupView(saved, outcomes.status(id)), null);
    }

    private Attempt block(
            CurrentUser user, long residentId, RecordCommand command, PickupBlock.Reason reason, Instant now) {
        RecordEvent<PickupBlock> block = recordEvents.add(
                PickupBlock.KIND,
                new EntityRef(RESIDENT_ENTITY, residentId),
                new NewRecordEvent<>(
                        new PickupBlock(
                                reason,
                                command.type(),
                                command.familyMemberId(),
                                command.exceptionId(),
                                command.presentedIdDocumentNumber()),
                        null,
                        user.accountId(),
                        false,
                        now,
                        null,
                        now,
                        null,
                        null));
        events.publishEvent(new PickupBlockNotice(
                block.id(),
                residentId,
                locations.floorOn(residentId, LocalDate.ofInstant(now, BusinessTime.ZONE)),
                reason.name(),
                now));
        return new Attempt(null, reason);
    }

    /** Lập yêu cầu ngoại lệ đón (FR-024): người có quyền một lệnh nguồn của lượt đón (FR-022). */
    @BusinessCommand
    @Transactional
    public SimpleRecord<PickupException> createException(
            long residentId, String fullName, String idDocumentNumber, String relationship, Reason reason) {
        CurrentUser user = currentUser.getObject();
        requireStaff(user);
        Target target = residentTarget(residentId);
        authorize(user, Operation.CREATE, target);
        PermissionResult source = null;
        for (Function function : PickupType.SOURCE_FUNCTIONS) {
            source = permissions.hasPermission(user, new PermissionKey(function, Operation.CREATE), target);
            if (source.allowed()) {
                break;
            }
        }
        if (!source.allowed()) {
            throw sourceDenied(source);
        }
        lock.lock(residentId);
        if (RightChangeRequestService.isFinal(residents.status(residentId))) {
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Người cao tuổi đã ở trạng thái cuối");
        }
        Instant now = time.now();
        PickupException content =
                PickupException.requested(fullName.strip(), idDocumentNumber.strip(), relationship, reason.value());
        SimpleRecord<PickupException> created = records.create(
                PickupException.KIND,
                new NewSimpleRecord<>(
                        residentId,
                        new EntityRef(RESIDENT_ENTITY, residentId),
                        Status.PENDING_CONFIRMATION.name(),
                        content,
                        null,
                        null,
                        null,
                        user.accountId(),
                        false));
        auditLog.write(AuditLogEntry.builder()
                .action(EXCEPTION_CREATED)
                .entity(PickupException.KIND.code(), created.id())
                .resident(residentId)
                .after(Map.of("status", created.status()))
                .reason(reason)
                .build());
        List<Long> representatives = relationships.activeOfResident(residentId).stream()
                .filter(FamilyRelationship::representative)
                .map(FamilyRelationship::familyMemberId)
                .toList();
        events.publishEvent(new PickupExceptionNotice(
                PickupExceptionNotice.Kind.NEEDS_CONFIRMATION,
                residentId,
                created.id(),
                content.fullName(),
                representatives,
                now));
        return created;
    }

    /** Đối tượng kiểm quyền của ngoại lệ: người cao tuổi của nó. */
    @Transactional(readOnly = true)
    public Target exceptionTarget(long exceptionId) {
        SimpleRecord<PickupException> exception = records.require(PickupException.KIND, exceptionId);
        return new Target(PickupException.KIND.code(), String.valueOf(exceptionId), exception.residentId(), null);
    }

    /** Người đại diện xác nhận qua cổng (FR-024; chú thích ²): Hiệu lực trong CFG-M10-08. */
    @BusinessCommand(requiresReason = false)
    @Transactional
    public SimpleRecord<PickupException> confirm(Target target, long exceptionId) {
        CurrentUser user = currentUser.getObject();
        requireRepresentative(user, target);
        return makeEffective(target, exceptionId, user, Method.PORTAL, null);
    }

    /** Quản lý viện duyệt kèm lý do (FR-024). */
    @BusinessCommand
    @RequiresPermission(function = Function.PICKUP, operation = Operation.APPROVE)
    @Transactional
    public SimpleRecord<PickupException> approve(Target target, long exceptionId, Reason reason) {
        return makeEffective(target, exceptionId, currentUser.getObject(), Method.MANAGER_APPROVAL, reason);
    }

    /** Người đại diện hoặc Quản lý viện từ chối yêu cầu Chờ xác nhận. */
    @BusinessCommand
    @Transactional
    public SimpleRecord<PickupException> reject(Target target, long exceptionId, Reason reason) {
        CurrentUser user = currentUser.getObject();
        requireDecider(user, target);
        return decide(target, exceptionId, Set.of(Status.PENDING_CONFIRMATION), Status.REJECTED, user, reason);
    }

    /**
     * Hủy (FR-024): yêu cầu Chờ xác nhận do người lập; ngoại lệ Hiệu lực chưa dùng do người đại diện hoặc Quản lý viện,
     * bắt buộc lý do.
     */
    @BusinessCommand(requiresReason = false)
    @Transactional
    public SimpleRecord<PickupException> cancel(Target target, long exceptionId, Reason reason) {
        CurrentUser user = currentUser.getObject();
        SimpleRecord<PickupException> current = records.require(PickupException.KIND, exceptionId);
        if (Status.valueOf(current.status()) == Status.EFFECTIVE) {
            requireDecider(user, target);
            if (reason == null) {
                throw new ReasonRequiredException();
            }
            return decide(target, exceptionId, Set.of(Status.EFFECTIVE), Status.CANCELLED, user, reason);
        }
        if (user.type() != AccountType.STAFF || current.createdBy() != user.accountId()) {
            throw denied();
        }
        return decide(target, exceptionId, Set.of(Status.PENDING_CONFIRMATION), Status.CANCELLED, user, reason);
    }

    /** Việc theo phút: ngoại lệ Hiệu lực quá CFG-M10-08 chuyển Hết hạn (FR-024). */
    @Transactional
    public int expireExceptions(Instant at) {
        int done = 0;
        for (SimpleRecord<PickupException> exception :
                records.listByStatus(PickupException.KIND, Set.of(Status.EFFECTIVE.name()))) {
            if (at.isBefore(exception.content().expiresAt())) {
                continue;
            }
            lock.lock(exception.residentId());
            SimpleRecord<PickupException> current = records.lockForUpdate(PickupException.KIND, exception.id());
            if (Status.valueOf(current.status()) == Status.EFFECTIVE) {
                close(current, Status.EXPIRED, current.content());
                done++;
            }
        }
        return done;
    }

    private SimpleRecord<PickupException> makeEffective(
            Target target, long exceptionId, CurrentUser user, Method method, Reason reason) {
        lock.lock(target.residentId());
        SimpleRecord<PickupException> current = records.lockForUpdate(PickupException.KIND, exceptionId);
        if (Status.valueOf(current.status()) != Status.PENDING_CONFIRMATION) {
            throw stateChanged();
        }
        Instant now = time.now();
        PickupException content = current.content()
                .effective(
                        user.accountId(),
                        method,
                        reason == null ? null : reason.value(),
                        now,
                        now.plus(parameters.read(ParameterCode.CFG_M10_08).asDuration()));
        SimpleRecord<PickupException> after = records.change(
                        PickupException.KIND,
                        exceptionId,
                        current.version(),
                        new RecordChange<>(Status.EFFECTIVE.name(), content, null, null, false))
                .after();
        audit(target.residentId(), current, after, reason);
        return after;
    }

    private SimpleRecord<PickupException> decide(
            Target target, long exceptionId, Set<Status> from, Status to, CurrentUser user, Reason reason) {
        lock.lock(target.residentId());
        SimpleRecord<PickupException> current = records.lockForUpdate(PickupException.KIND, exceptionId);
        if (!from.contains(Status.valueOf(current.status()))) {
            throw stateChanged();
        }
        SimpleRecord<PickupException> after =
                close(current, to, current.content().decided(user.accountId(), reason == null ? null : reason.value()));
        audit(target.residentId(), current, after, reason);
        return after;
    }

    private SimpleRecord<PickupException> close(
            SimpleRecord<PickupException> current, Status to, PickupException content) {
        return records.change(
                        PickupException.KIND,
                        current.id(),
                        current.version(),
                        new RecordChange<>(to.name(), content, null, null, true))
                .after();
    }

    private void audit(
            long residentId, SimpleRecord<PickupException> before, SimpleRecord<PickupException> after, Reason reason) {
        auditLog.write(AuditLogEntry.builder()
                .action(EXCEPTION_DECIDED)
                .entity(PickupException.KIND.code(), after.id())
                .resident(residentId)
                .before(Map.of("status", before.status()))
                .after(Map.of("status", after.status()))
                .reason(reason)
                .build());
    }

    private boolean selfReturnOn(long residentId) {
        return records.findByUniqueKey(SelfReturnFlag.KIND, SelfReturnFlag.key(residentId))
                .map(r -> r.content().enabled())
                .orElse(false);
    }

    /** Người đại diện có quan hệ Hiệu lực: quyền Sửa "Quy trình đón" của người thân, chú thích ². */
    private void requireRepresentative(CurrentUser user, Target target) {
        if (user.type() != AccountType.FAMILY_MEMBER) {
            throw denied();
        }
        authorize(user, Operation.UPDATE, target);
    }

    private void requireDecider(CurrentUser user, Target target) {
        if (user.type() == AccountType.FAMILY_MEMBER) {
            authorize(user, Operation.UPDATE, target);
            return;
        }
        authorize(user, Operation.APPROVE, target);
    }

    private void authorize(CurrentUser user, Operation operation, Target target) {
        PermissionResult result =
                permissions.hasPermission(user, new PermissionKey(Function.PICKUP, operation), target);
        if (result.allowed()) {
            return;
        }
        FailedLayer layer = result.failedLayer().orElse(FailedLayer.ROLE);
        if (layer == FailedLayer.SCOPE) {
            throw new NotFoundException();
        }
        throw new PermissionDeniedException("PERMISSION_DENIED", "Không có quyền", Map.of("failedLayer", layer.name()));
    }

    private static RuntimeException sourceDenied(PermissionResult result) {
        FailedLayer layer = result.failedLayer().orElse(FailedLayer.ROLE);
        if (layer == FailedLayer.SCOPE) {
            return new NotFoundException();
        }
        return new PermissionDeniedException(
                "SOURCE_COMMAND_PERMISSION_DENIED",
                "Không có quyền thực hiện lệnh nguồn của lượt đón (FR-022)",
                Map.of("failedLayer", layer.name()));
    }

    private static void requireStaff(CurrentUser user) {
        if (user.type() != AccountType.STAFF) {
            throw denied();
        }
    }

    private static PermissionDeniedException denied() {
        return new PermissionDeniedException("PERMISSION_DENIED", "Không có quyền", Map.of("failedLayer", "ROLE"));
    }

    private static BusinessException stateChanged() {
        return new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Ngoại lệ đón đã đổi trạng thái");
    }

    private static void validate(RecordCommand command) {
        if (command.type() == PickupType.SELF_RETURN) {
            if (command.familyMemberId() != null || command.exceptionId() != null) {
                throw new InvalidValueException("Tự về không có người đón");
            }
            return;
        }
        if ((command.familyMemberId() == null) == (command.exceptionId() == null)) {
            throw new InvalidValueException(
                    "Chọn đúng một trong người đón (familyMemberId) hoặc ngoại lệ (exceptionId)");
        }
        if (command.presentedIdDocumentNumber() == null
                || command.presentedIdDocumentNumber().isBlank()) {
            throw new InvalidValueException("Thiếu giấy tờ xuất trình (FR-021 bước 3)");
        }
    }

    static Target residentTarget(long residentId) {
        return new Target(RESIDENT_ENTITY, String.valueOf(residentId), residentId, null);
    }

    Optional<PickupRecord> find(long id) {
        return store.find(id);
    }
}
