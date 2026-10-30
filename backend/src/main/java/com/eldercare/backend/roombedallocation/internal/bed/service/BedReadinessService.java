package com.eldercare.backend.roombedallocation.internal.bed.service;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.BedAllocationCommandApi;
import com.eldercare.backend.roombedallocation.api.dto.*;
import com.eldercare.backend.roombedallocation.api.spi.AllocationInfectionStatus;
import com.eldercare.backend.roombedallocation.internal.allocation.persistence.AllocationStore;
import com.eldercare.backend.roombedallocation.internal.bed.domain.*;
import com.eldercare.backend.roombedallocation.internal.bed.persistence.*;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** T058: quyền theo tầng, kiểm nguồn lại trong khóa và một transaction xác nhận/chuyển đang chờ. */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class BedReadinessService {
    private final BedReadinessStore store;
    private final AllocationStore locks;
    private final BedStateService states;
    private final BedStore beds;
    private final CurrentUser user;
    private final ScopeApi scope;
    private final PermissionApi permissions;
    private final BusinessTime time;
    private final ObjectProvider<AllocationInfectionStatus> infection;
    private final ObjectProvider<BedAllocationCommandApi> transfers;
    private final BedHoldService holds;

    public BedReadinessService(
            BedReadinessStore store,
            AllocationStore locks,
            BedStateService states,
            BedStore beds,
            CurrentUser user,
            ScopeApi scope,
            PermissionApi permissions,
            BusinessTime time,
            ObjectProvider<AllocationInfectionStatus> infection,
            ObjectProvider<BedAllocationCommandApi> transfers,
            BedHoldService holds) {
        this.store = store;
        this.locks = locks;
        this.states = states;
        this.beds = beds;
        this.user = user;
        this.scope = scope;
        this.permissions = permissions;
        this.time = time;
        this.infection = infection;
        this.transfers = transfers;
        this.holds = holds;
    }

    @Transactional(readOnly = true)
    public Target target(long id) {
        return target(store.read(id));
    }

    private Target target(BedReadinessStore.Snapshot row) {
        return new Target("M03.BED", Long.toString(row.id()), null, row.floorId());
    }

    private void access(BedReadinessStore.Snapshot row, Operation operation) {
        if (!permissions
                        .hasPermission(user, new PermissionKey(Function.BED_READINESS, operation), target(row))
                        .allowed()
                || !(scope.assignedFloorManager(row.floorId())
                                .filter(a -> a == user.accountId())
                                .isPresent()
                        || scope.shiftLead(row.floorId(), time.now())
                                .filter(a -> a == user.accountId())
                                .isPresent()))
            throw new PermissionDeniedException("Chỉ Trưởng tầng được giao hoặc Người phụ trách ca của tầng thực hiện");
    }

    @RequiresPermission(function = Function.BED_READINESS, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public BedReadiness conditions(Target target) {
        var row = store.read(Long.parseLong(target.id()));
        access(row, Operation.VIEW);
        return conditions(row);
    }

    private BedReadiness conditions(BedReadinessStore.Snapshot row) {
        boolean required = false;
        if (row.departedResidentId() != null) {
            var source = infection.getIfAvailable();
            if (source == null) throw unavailable();
            required = source.listedAt(row.departedResidentId(), time.now());
        }
        var violations = row.state() == BedStatus.AWAITING_READY
                ? List.<UnmetCondition>of()
                : List.of(new UnmetCondition(
                        "BED_NOT_AWAITING_READY", "Giường không ở trạng thái Chờ sẵn sàng", "BR-M03-09", "003", false));
        return new BedReadiness(violations.isEmpty(), required, row.departedResidentId(), violations);
    }

    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.BED_READINESS, operation = Operation.CREATE)
    public BedDetails confirm(Target target, Boolean disinfected, Boolean ppeUsed, String note) {
        long id = Long.parseLong(target.id());
        locks.guardWrites(false);
        locks.guardRooms(List.of(id));
        locks.lockBed(id);
        var row = store.read(id);
        access(row, Operation.CREATE);
        var condition = conditions(row);
        if (!condition.allowed())
            throw new BusinessException(
                    HttpStatus.CONFLICT, "BED_NOT_AWAITING_READY", "Giường không ở trạng thái Chờ sẵn sàng");
        var pending = store.waiting(id);
        var source = pending.isEmpty() ? null : transfers.getIfAvailable();
        if (!pending.isEmpty() && source == null) throw unavailable();
        states.transition(
                id,
                new BedTransition(
                        BedEvent.CONFIRM_READY,
                        null,
                        new Basis("BR-M03-09", null, id, null),
                        "BED",
                        id,
                        null,
                        user.accountId(),
                        null,
                        null,
                        condition.disinfectionRequired(),
                        disinfected,
                        ppeUsed,
                        false,
                        false,
                        note));
        for (var waiting : pending)
            if (source.applyScheduledTransfer(waiting.requestId(), waiting.plannedAt())
                    != ScheduledTransferResult.TRANSFERRED)
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "SCHEDULED_TRANSFER_NOT_COMPLETED",
                        "Chuyển theo lịch chưa thực hiện được");
        var result = beds.details(id).orElseThrow(NotFoundException::new);
        if (!pending.isEmpty() && result.state() != BedStatus.OCCUPIED && result.state() != BedStatus.HELD)
            throw new IllegalStateException("Nguồn chuyển chưa cập nhật giường");
        if (pending.isEmpty()) holds.becameEmpty(id, "CONFIRM_READY");
        return result;
    }

    private BusinessException unavailable() {
        return new BusinessException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "SOURCE_UNAVAILABLE",
                "Chưa có nguồn kiểm lây nhiễm hoặc thực hiện chuyển theo lịch");
    }
}
