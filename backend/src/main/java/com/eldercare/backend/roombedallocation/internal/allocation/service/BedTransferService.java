package com.eldercare.backend.roombedallocation.internal.allocation.service;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.dto.BedNotificationContext;
import com.eldercare.backend.roombedallocation.api.events.BedTransferred;
import com.eldercare.backend.roombedallocation.api.events.DifferentPriceBedTransfer;
import com.eldercare.backend.roombedallocation.api.spi.AllocationInfectionStatus;
import com.eldercare.backend.roombedallocation.api.spi.AllocationResidencySource;
import com.eldercare.backend.roombedallocation.internal.allocation.domain.AllocationStatus;
import com.eldercare.backend.roombedallocation.internal.allocation.persistence.*;
import com.eldercare.backend.roombedallocation.internal.bed.domain.*;
import com.eldercare.backend.roombedallocation.internal.bed.service.BedStateService;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** T032: chuyển trực tiếp tại thời điểm hiện tại; nhập bù và chuyển theo lịch thuộc task sau. */
@Service
public class BedTransferService {
    private final AllocationCommandStore allocations;
    private final BedTransferStore store;
    private final AllocationCheckStore beds;
    private final AllocationLocks locks;
    private final AllocationChecker checker;
    private final ResidentAllocationFacts facts;
    private final BedStateService states;
    private final CurrentUser user;
    private final PermissionApi permissions;
    private final BusinessTime time;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final ObjectProvider<AllocationResidencySource> residency;
    private final ObjectProvider<AllocationInfectionStatus> infection;

    public BedTransferService(
            AllocationCommandStore allocations,
            BedTransferStore store,
            AllocationCheckStore beds,
            AllocationLocks locks,
            AllocationChecker checker,
            ResidentAllocationFacts facts,
            BedStateService states,
            CurrentUser user,
            PermissionApi permissions,
            BusinessTime time,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            ObjectProvider<AllocationResidencySource> residency,
            ObjectProvider<AllocationInfectionStatus> infection) {
        this.allocations = allocations;
        this.store = store;
        this.beds = beds;
        this.locks = locks;
        this.checker = checker;
        this.facts = facts;
        this.states = states;
        this.user = user;
        this.permissions = permissions;
        this.time = time;
        this.audit = audit;
        this.events = events;
        this.residency = residency;
        this.infection = infection;
    }

    @RequiresPermission(function = Function.BED_ALLOCATION, operation = Operation.CREATE)
    @BusinessCommand
    @org.springframework.resilience.annotation.Retryable(
            includes = org.springframework.dao.PessimisticLockingFailureException.class,
            maxRetries = 2,
            delay = 50)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public long transfer(long id, long target, long reasonId, Reason reason) {
        var snapshot = allocations.get(id);
        locks.lock(snapshot.residentId(), List.of(snapshot.bedId(), target));
        var old = allocations.current(id);
        var from = beds.currentCandidate(old.bedId());
        var to = beds.currentCandidate(target);
        scope(from.bedId(), from.floorId());
        scope(to.bedId(), to.floorId());
        if (old.status() != AllocationStatus.ACTIVE || old.bedId() == target)
            throw conflict("ALLOCATION_NOT_TRANSFERABLE", "Cần phân bổ đang Hiệu lực và giường đích khác");
        // Chủ dự án giữ SPI hiện hành: chưa suy giá hợp đồng để giải quyết dấu chờ qua lần chuyển tiếp.
        if (store.awaitingPriceResolution(id))
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "SOURCE_UNAVAILABLE",
                    "Chưa có nguồn đối chiếu giá hợp đồng cho phân bổ đang chờ xử lý khác giá");
        var at = time.now().truncatedTo(ChronoUnit.MICROS);
        if (!at.isAfter(old.startAt())
                || old.expectedEnd() != null && !old.expectedEnd().isAfter(at))
            throw conflict("ALLOCATION_TIME_OVERLAP", "Khoảng chuyển không hợp lệ");
        var source = residency.getIfAvailable();
        var infections = infection.getIfAvailable();
        if (source == null || infections == null)
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_UNAVAILABLE", "Chưa đủ nguồn kiểm chuyển giường");
        var cordon = infections.cordonedAt(at);
        if (from.isolated()
                || cordon.floorIds().contains(from.floorId())
                || cordon.areaIds().contains(from.areaId()))
            throw conflict("INFECTION_CONTROL_TRANSFER_REQUIRED", "Cần lệnh chuyển theo chỉ định kiểm soát lây nhiễm");
        var violations = checker.checkCurrent(old.residentId(), target, at, old.expectedEnd(), id, false);
        if (!violations.isEmpty())
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "UNMET_CONDITIONS",
                    "Điều kiện giường đích chưa thỏa",
                    Map.of("unmetConditions", violations));
        boolean medical = store.medicalSafety(reasonId);
        boolean different = source.differentUnitPrice(
                old.residentId(), store.roomType(old.bedId()), store.roomType(target), time.today());
        if (different && !medical)
            throw conflict(
                    "RESIDENCY_CHANGE_REQUEST_REQUIRED", "Cần yêu cầu thay đổi lưu trú đã duyệt khi chuyển khác giá");
        var hold = store.absentHold(old.bedId());
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
        store.transferred(id, next, reasonId, different, at);
        states.transition(old.bedId(), transition(BedEvent.TRANSFERRED_OUT, id, reason, null, id));
        states.transition(target, transition(BedEvent.ALLOCATION_STARTED, next, reason, null, null));
        if (hold != null) states.transition(target, transition(BedEvent.ABSENT_KEEP_BED, next, reason, hold, null));
        source.allocated(old.residentId(), next);
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("M03_BED_TRANSFER", true))
                .entity("M03.BED_ALLOCATION", next)
                .resident(old.residentId())
                .before(Map.of("allocationId", id, "bedId", old.bedId(), "floorId", from.floorId()))
                .after(Map.of(
                        "allocationId",
                        next,
                        "bedId",
                        target,
                        "floorId",
                        to.floorId(),
                        "transferredAt",
                        at.toString(),
                        "transferReasonId",
                        reasonId))
                .reason(reason)
                .build());
        events.publishEvent(new BedTransferred(old.residentId(), id, next, from.floorId(), to.floorId(), at));
        if (different)
            events.publishEvent(new DifferentPriceBedTransfer(
                    next,
                    old.residentId(),
                    user.accountId(),
                    new BedNotificationContext(
                            at,
                            to.floorId(),
                            List.of(
                                    new RecipientGroup.FacilityRole(Role.OFFICE_STAFF),
                                    new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)))));
        return next;
    }

    private void scope(long bed, long floor) {
        if (!permissions
                .hasPermission(
                        user,
                        new PermissionKey(Function.BED_ALLOCATION, Operation.CREATE),
                        new Target("M03.BED", Long.toString(bed), null, floor))
                .allowed()) throw new PermissionDeniedException("Giường ngoài phạm vi chuyển");
    }

    private BedTransition transition(BedEvent event, long id, Reason reason, BedHold hold, Long closed) {
        return new BedTransition(
                event,
                reason,
                new Basis("FR-028", null, id, null),
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
                false);
    }

    private BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }
}
