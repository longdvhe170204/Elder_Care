package com.eldercare.backend.roombedallocation.internal.allocation.service;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.roombedallocation.api.events.BedAllocationStarted;
import com.eldercare.backend.roombedallocation.api.spi.AllocationResidencySource;
import com.eldercare.backend.roombedallocation.internal.allocation.domain.*;
import com.eldercare.backend.roombedallocation.internal.allocation.persistence.*;
import com.eldercare.backend.roombedallocation.internal.bed.domain.*;
import com.eldercare.backend.roombedallocation.internal.bed.service.*;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T026: lệnh theo trạng thái M01, cùng bộ kiểm và cùng khóa với các tác động trạng thái. */
@Service
public class AllocationCommands {
    private final AllocationCommandStore store;
    private final AllocationStore positions;
    private final AllocationCheckStore checks;
    private final AllocationLocks locks;
    private final AllocationChecker checker;
    private final ResidentAllocationFacts facts;
    private final ResidentApi residents;
    private final ObjectProvider<AllocationResidencySource> residency;
    private final BusinessTime time;
    private final CurrentUser user;
    private final PermissionApi permissions;
    private final BedHoldService holds;
    private final BedStateService state;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;

    public AllocationCommands(
            AllocationCommandStore store,
            AllocationStore positions,
            AllocationCheckStore checks,
            AllocationLocks locks,
            AllocationChecker checker,
            ResidentAllocationFacts facts,
            ResidentApi residents,
            ObjectProvider<AllocationResidencySource> residency,
            BusinessTime time,
            CurrentUser user,
            PermissionApi permissions,
            BedHoldService holds,
            BedStateService state,
            AuditLogApi audit,
            ApplicationEventPublisher events) {
        this.store = store;
        this.positions = positions;
        this.checks = checks;
        this.locks = locks;
        this.checker = checker;
        this.facts = facts;
        this.residents = residents;
        this.residency = residency;
        this.time = time;
        this.user = user;
        this.permissions = permissions;
        this.holds = holds;
        this.state = state;
        this.audit = audit;
        this.events = events;
    }

    @RequiresPermission(function = Function.BED_ALLOCATION, operation = Operation.CREATE)
    @BusinessCommand
    @org.springframework.resilience.annotation.Retryable(
            includes = org.springframework.dao.PessimisticLockingFailureException.class,
            maxRetries = 2,
            delay = 50)
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public long create(long residentId, long bedId, Instant startAt, Reason reason) {
        locks.lock(residentId, List.of());
        var bedIds = new ArrayList<>(store.heldBeds(residentId));
        bedIds.add(bedId);
        locks.lock(residentId, bedIds);
        var bed = checks.candidate(bedId);
        if (!permissions
                .hasPermission(
                        user,
                        new PermissionKey(Function.BED_ALLOCATION, Operation.CREATE),
                        new Target("M03.BED", Long.toString(bedId), null, bed.floorId()))
                .allowed()) throw new PermissionDeniedException("Giường ngoài phạm vi phân bổ");
        boolean future = residents.status(residentId) == ResidentStatus.ADMISSION_IN_PROGRESS;
        Instant start = future && startAt != null ? startAt : time.now();
        if (store.hasOpen(residentId)) throw error("ALREADY_ALLOCATED", "Đã có phân bổ, hãy dùng lệnh chuyển giường");
        if (!future && (residents.status(residentId) != ResidentStatus.IN_RESIDENCE || !positions.hasNoBed(residentId)))
            throw error("WITHOUT_BED_REQUIRED", "Chỉ phân bổ ngay cho người đang lưu trú chưa có giường");
        var source = residency.getIfAvailable();
        if (source == null)
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_UNAVAILABLE", "Chưa có nguồn lưu trú");
        Instant end = source.expectedEnd(residentId).orElse(null);
        if (end != null && !end.isAfter(start))
            throw error("ALLOCATION_TIME_OVERLAP", "Kết thúc dự kiến phải sau bắt đầu");
        var violations = checker.check(residentId, bedId, start, end, null, future);
        if (!violations.isEmpty())
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "UNMET_CONDITIONS",
                    "Điều kiện phân bổ chưa thỏa",
                    Map.of("unmetConditions", violations));
        long id;
        try {
            id = store.create(
                    residentId,
                    bedId,
                    future ? AllocationStatus.FUTURE : AllocationStatus.ACTIVE,
                    start,
                    end,
                    reason.value(),
                    user.accountId(),
                    facts.careLevel(residentId).orElseThrow());
        } catch (DataIntegrityViolationException error) {
            throw error("ALLOCATION_TIME_OVERLAP", "Giường đã được phân bổ trong khoảng thời gian này");
        }
        for (long waiting : store.waitingRecords(residentId))
            holds.cancelHoldExcept(waiting, reason, future ? bedId : null);
        source.allocated(residentId, id);
        if (!future) {
            store.clearWithoutBed(residentId);
            state.transition(
                    bedId,
                    new BedTransition(
                            BedEvent.ALLOCATION_STARTED,
                            reason,
                            new Basis("FR-025a", null, id, null),
                            "BED_ALLOCATION",
                            id,
                            null,
                            user.accountId(),
                            null,
                            null,
                            null,
                            null,
                            null,
                            false,
                            false));
            events.publishEvent(new BedAllocationStarted(id, residentId, bedId, start));
        }
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("M03_ALLOCATION_CREATED", true))
                .entity("M03.BED_ALLOCATION", id)
                .after(Map.of("residentId", residentId, "bedId", bedId, "status", future ? "FUTURE" : "ACTIVE"))
                .reason(reason)
                .build());
        return id;
    }

    @RequiresPermission(function = Function.BED_ALLOCATION, operation = Operation.UPDATE)
    @BusinessCommand
    @org.springframework.resilience.annotation.Retryable(
            includes = org.springframework.dao.PessimisticLockingFailureException.class,
            maxRetries = 2,
            delay = 50)
    @Transactional
    public void cancel(long id, Reason reason) {
        var allocation = store.get(id);
        locks.lock(allocation.residentId(), List.of(allocation.bedId()));
        allocation = store.current(id);
        var bed = checks.candidate(allocation.bedId());
        if (!permissions
                .hasPermission(
                        user,
                        new PermissionKey(Function.BED_ALLOCATION, Operation.UPDATE),
                        new Target("M03.BED", Long.toString(bed.bedId()), null, bed.floorId()))
                .allowed()) throw new PermissionDeniedException("Giường ngoài phạm vi phân bổ");
        if (allocation.status() != AllocationStatus.FUTURE)
            throw error("FUTURE_ALLOCATION_REQUIRED", "Chỉ hủy phân bổ tương lai");
        store.cancel(id, reason.value());
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("M03_ALLOCATION_CANCELLED", true))
                .entity("M03.BED_ALLOCATION", id)
                .before(Map.of("status", "FUTURE"))
                .after(Map.of("status", "CANCELLED"))
                .reason(reason)
                .build());
    }

    private static BusinessException error(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }
}
