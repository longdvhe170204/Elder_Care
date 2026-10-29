package com.eldercare.backend.roombedallocation.internal.isolation.service;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.common.*;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.dto.BedNotificationContext;
import com.eldercare.backend.roombedallocation.api.events.*;
import com.eldercare.backend.roombedallocation.api.spi.AllocationResidencySource;
import com.eldercare.backend.roombedallocation.internal.allocation.domain.*;
import com.eldercare.backend.roombedallocation.internal.allocation.persistence.*;
import com.eldercare.backend.roombedallocation.internal.allocation.service.*;
import com.eldercare.backend.roombedallocation.internal.bed.domain.*;
import com.eldercare.backend.roombedallocation.internal.bed.service.BedStateService;
import com.eldercare.backend.roombedallocation.internal.isolation.domain.ControlledTransferResult;
import com.eldercare.backend.roombedallocation.internal.isolation.persistence.ControlledTransferStore;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** T055: ngoại lệ duy nhất là điều kiện cách ly; đóng/mở và hai giường cùng rollback. */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class ControlledTransferService {
    private final AllocationCommandStore allocations;
    private final ControlledTransferStore store;
    private final AllocationReadStore read;
    private final AllocationCheckStore beds;
    private final AllocationLocks locks;
    private final AllocationChecker checker;
    private final ResidentAllocationFacts facts;
    private final BedStateService states;
    private final AccountApi accounts;
    private final CurrentUser user;
    private final PermissionApi permissions;
    private final BusinessTime time;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final ObjectProvider<AllocationResidencySource> residency;

    public ControlledTransferService(
            AllocationCommandStore allocations,
            ControlledTransferStore store,
            AllocationReadStore read,
            AllocationCheckStore beds,
            AllocationLocks locks,
            AllocationChecker checker,
            ResidentAllocationFacts facts,
            BedStateService states,
            AccountApi accounts,
            CurrentUser user,
            PermissionApi permissions,
            BusinessTime time,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            ObjectProvider<AllocationResidencySource> residency) {
        this.allocations = allocations;
        this.store = store;
        this.read = read;
        this.beds = beds;
        this.locks = locks;
        this.checker = checker;
        this.facts = facts;
        this.states = states;
        this.accounts = accounts;
        this.user = user;
        this.permissions = permissions;
        this.time = time;
        this.audit = audit;
        this.events = events;
        this.residency = residency;
    }

    @RequiresPermission(function = Function.BED_ALLOCATION, operation = Operation.CREATE)
    @BusinessCommand
    public ControlledTransferResult transfer(long id, long target, long doctor, Reason reason) {
        var snapshot = allocations.get(id);
        locks.lock(snapshot.residentId(), List.of(snapshot.bedId(), target));
        var old = allocations.current(id);
        var from = beds.currentCandidate(old.bedId());
        var to = beds.currentCandidate(target);
        scope(from.bedId(), from.floorId());
        scope(to.bedId(), to.floorId());
        if (old.status() != AllocationStatus.ACTIVE || old.bedId() == target)
            throw conflict("ALLOCATION_NOT_TRANSFERABLE", "Cần phân bổ đang Hiệu lực và giường đích khác");
        if (!accounts.hasRole(doctor, Role.DOCTOR)) throw invalidDoctor();
        Long staff = accounts.byId(doctor)
                .filter(a -> a.status() == AccountStatus.ACTIVE)
                .map(AccountView::staffId)
                .orElse(null);
        if (staff == null) throw invalidDoctor();
        var at = time.now();
        if (!at.isAfter(old.startAt())
                || (old.expectedEnd() != null && !old.expectedEnd().isAfter(at)))
            throw conflict("ALLOCATION_TIME_OVERLAP", "Khoảng chuyển không hợp lệ");
        var conditions = checker.checkCurrent(old.residentId(), target, at, old.expectedEnd(), id, false);
        var violations = conditions.stream()
                .filter(x -> !x.code().equals("ALLOCATION_ISOLATION"))
                .toList();
        if (!violations.isEmpty())
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "UNMET_CONDITIONS",
                    "Điều kiện giường đích chưa thỏa",
                    Map.of("unmetConditions", violations));
        var source = residency.getIfAvailable();
        if (source == null)
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_UNAVAILABLE", "Chưa có nguồn lưu trú004");
        boolean targetBlocked = conditions.stream().anyMatch(x -> x.code().equals("ALLOCATION_ISOLATION"));
        boolean pending = store.pending(id) && targetBlocked;
        var hold = store.absentHold(old.bedId());
        store.clearPending(id);
        allocations.close(id, at, "BED_TRANSFER", user.accountId());
        long next = allocations.create(
                old.residentId(),
                target,
                AllocationStatus.ACTIVE,
                at,
                old.expectedEnd(),
                reason.value(),
                user.accountId(),
                facts.careLevel(old.residentId()).orElseThrow());
        store.metadata(id, next, doctor, pending);
        states.transition(old.bedId(), transition(BedEvent.TRANSFERRED_OUT, id, reason, null, id));
        states.transition(target, transition(BedEvent.ALLOCATION_STARTED, next, reason, null, null));
        if (hold != null) states.transition(target, transition(BedEvent.ABSENT_KEEP_BED, next, reason, hold, null));
        source.allocated(old.residentId(), next);
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("M03_INFECTION_CONTROL_TRANSFER", true))
                .entity("M03.BED_ALLOCATION", next)
                .resident(old.residentId())
                .before(Map.of("allocationId", id, "bedId", old.bedId()))
                .after(Map.of("allocationId", next, "bedId", target, "prescribingDoctorId", doctor))
                .reason(reason)
                .build());
        events.publishEvent(new BedTransferred(old.residentId(), id, next, from.floorId(), to.floorId(), at));
        events.publishEvent(new DoctorAssignedToAllocation(
                next,
                doctor,
                new BedNotificationContext(
                        at, to.floorId(), List.of(new RecipientGroup.Staff(staff, "PRESCRIBING_DOCTOR")))));
        return new ControlledTransferResult(detail(id), detail(next));
    }

    @RequiresPermission(function = Function.ROOM_ISOLATION, operation = Operation.UPDATE)
    @BusinessCommand
    public AllocationDetails confirm(long id, Reason reason) {
        if (!activeDoctor()) throw new PermissionDeniedException("Không có quyền thực hiện lệnh");
        var snapshot = allocations.get(id);
        locks.lock(snapshot.residentId(), List.of(snapshot.bedId()));
        var value = allocations.current(id);
        if (value.status() != AllocationStatus.ACTIVE || !store.pending(id))
            throw conflict("DOCTOR_CONFIRMATION_NOT_PENDING", "Không có dấu chờ xác nhận");
        store.confirm(id, user.accountId(), time.now(), reason.value());
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("M03_ISOLATED_STAY_CONFIRMED", true))
                .entity("M03.BED_ALLOCATION", id)
                .resident(value.residentId())
                .before(Map.of("awaitingDoctorConfirmation", true))
                .after(Map.of("awaitingDoctorConfirmation", false, "confirmingDoctorId", user.accountId()))
                .reason(reason)
                .build());
        return detail(id);
    }

    private AllocationDetails detail(long id) {
        return read.list(null, null, null).stream()
                .filter(x -> x.id() == id)
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    @RequiresPermission(function = Function.BED_ALLOCATION, operation = Operation.CREATE)
    @Transactional(readOnly = true)
    public List<com.eldercare.backend.roombedallocation.internal.isolation.domain.PrescribingDoctor> doctors() {
        return accounts.activeWithRole(Role.DOCTOR).stream()
                .filter(a -> a.status() == AccountStatus.ACTIVE && a.staffId() != null)
                .map(a -> new com.eldercare.backend.roombedallocation.internal.isolation.domain.PrescribingDoctor(
                        a.accountId(), accounts.fullName(a.accountId()).orElse(null)))
                .toList();
    }

    @RequiresPermission(function = Function.ROOM_ISOLATION, operation = Operation.UPDATE)
    @Transactional(readOnly = true)
    public List<AllocationDetails> pendingStays() {
        if (!activeDoctor()) throw new PermissionDeniedException("Chỉ Bác sĩ xem danh sách chờ xác nhận");
        return read.list(null, null, AllocationStatus.ACTIVE).stream()
                .filter(a -> a.flags().contains("AWAITING_DOCTOR_CONFIRMATION"))
                .toList();
    }

    private boolean activeDoctor() {
        return accounts.hasRole(user.accountId(), Role.DOCTOR)
                && accounts.byId(user.accountId())
                        .filter(a -> a.status() == AccountStatus.ACTIVE)
                        .isPresent();
    }

    private void scope(long bed, long floor) {
        if (!permissions
                .hasPermission(
                        user,
                        new PermissionKey(Function.BED_ALLOCATION, Operation.CREATE),
                        new Target("M03.BED", Long.toString(bed), null, floor))
                .allowed()) throw new PermissionDeniedException("Không có quyền thực hiện lệnh");
    }

    private BedTransition transition(BedEvent event, long id, Reason reason, BedHold hold, Long closed) {
        return new BedTransition(
                event,
                reason,
                new Basis("FR-010", null, id, null),
                "BED_ALLOCATION",
                id,
                null,
                user.accountId(),
                hold,
                closed,
                null,
                null,
                null,
                false,
                true);
    }

    private BusinessException invalidDoctor() {
        return new BusinessException(
                HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_PRESCRIBING_DOCTOR", "Cần tài khoản Bác sĩ đang Hoạt động");
    }

    private BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }
}
