package com.eldercare.backend.roombedallocation.internal.bed.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.administration.parameter.api.*;
import com.eldercare.backend.administration.parameter.api.dto.*;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.events.*;
import com.eldercare.backend.roombedallocation.internal.allocation.service.AllocationLocks;
import com.eldercare.backend.roombedallocation.internal.bed.domain.*;
import com.eldercare.backend.roombedallocation.internal.bed.persistence.BedHoldStore;
import com.eldercare.backend.roombedallocation.internal.bed.persistence.BedStore;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** T040: tác động trong transaction của lệnh nguồn; facade BedAllocationCommandApi nối khi đủ lệnh theo lịch. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class BedHoldService {
    private final BedHoldStore store;
    private final BedStore beds;
    private final BedStateService state;
    private final AllocationLocks locks;
    private final ParameterApi parameters;
    private final BusinessTime time;
    private final CurrentUser user;
    private final ApplicationEventPublisher events;
    private final ObjectProvider<BedAssetPort> assets;
    private final AuditLogApi audit;
    private final com.eldercare.backend.roombedallocation.internal.allocation.persistence.WaitingHoldIndexStore index;

    public BedHoldService(
            BedHoldStore store,
            BedStore beds,
            BedStateService state,
            AllocationLocks locks,
            ParameterApi parameters,
            BusinessTime time,
            CurrentUser user,
            ApplicationEventPublisher events,
            ObjectProvider<BedAssetPort> assets,
            AuditLogApi audit,
            com.eldercare.backend.roombedallocation.internal.allocation.persistence.WaitingHoldIndexStore index) {
        this.store = store;
        this.beds = beds;
        this.state = state;
        this.locks = locks;
        this.parameters = parameters;
        this.time = time;
        this.user = user;
        this.events = events;
        this.assets = assets;
        this.audit = audit;
        this.index = index;
    }

    public void holdForWaitingRecord(long waitingRecordId, long residentId, long bedId, Instant at) {
        locks.lock(residentId, List.of(bedId));
        var asset = assets.getIfAvailable();
        if (asset == null)
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_UNAVAILABLE", "Chưa thể kiểm dấu tài sản giường");
        Instant until = at.plus(parameters.read(ParameterCode.CFG_M02_02).asDuration());
        var failures = new ArrayList<UnmetCondition>();
        if (store.overlaps(bedId, at, until))
            failures.add(new UnmetCondition("FR-012", "Khoảng giữ chỗ chồng phân bổ"));
        if (asset.awaitingResidentTransfer(bedId))
            failures.add(new UnmetCondition("BR-M03-15", "Giường đang chờ chuyển người"));
        if (!failures.isEmpty()) throw new ConditionNotMetException(failures);
        state.transition(
                bedId,
                command(
                        BedEvent.HOLD_WAITING_RECORD,
                        null,
                        new Basis("FR-012", null, waitingRecordId, null),
                        "WAITING_RECORD",
                        waitingRecordId,
                        null,
                        new BedHold(HoldReason.WAITING_RECORD, residentId, waitingRecordId, until),
                        null));
    }

    public void cancelHold(long waitingRecordId, Reason reason) {
        cancelHoldExcept(waitingRecordId, reason, null);
    }

    public void cancelHoldExcept(long waitingRecordId, Reason reason, Long retainedBedId) {
        for (var held : store.waiting(waitingRecordId)) {
            if (java.util.Objects.equals(held.bedId(), retainedBedId)) continue;
            locks.lock(held.residentId(), List.of(held.bedId()));
            if (!store.waitingCurrent(held.bedId(), waitingRecordId)) continue;
            state.transition(
                    held.bedId(),
                    command(
                            BedEvent.HOLD_CANCELLED,
                            reason,
                            new Basis("FR-014", null, waitingRecordId, null),
                            "WAITING_RECORD",
                            waitingRecordId,
                            null,
                            null,
                            null));
            becameEmpty(held.bedId(), "HOLD_CANCELLED");
        }
    }

    public void releaseWhenAbsent(long residentId, Instant at, Basis basis) {
        locks.lock(residentId, List.of());
        var active = store.active(residentId);
        locks.lock(residentId, List.of(active.bedId()));
        active = store.active(residentId);
        if (!store.heldAbsent(active.bedId(), residentId))
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("FR-015", "Giường không giữ cho người vắng này")));
        store.close(active.allocationId(), at, user.isScheduler() ? null : user.accountId());
        state.transition(
                active.bedId(),
                command(
                        BedEvent.RELEASE_WHEN_ABSENT,
                        null,
                        basis,
                        "BED_ALLOCATION",
                        active.allocationId(),
                        null,
                        null,
                        active.allocationId()));
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("M03_ALLOCATION_CLOSED", false))
                .entity("M03.BED_ALLOCATION", active.allocationId())
                .before(Map.of("status", "ACTIVE"))
                .after(Map.of("status", "CLOSED", "endAt", at.toString()))
                .basis(basis)
                .build());
        events.publishEvent(
                new BedAllocationClosed(active.allocationId(), residentId, active.bedId(), at, "RELEASE_WHEN_ABSENT"));
    }

    /** Nguồn trạng thái đã biết người: tra chỉ mục hiện hành sau khóa vị trí, rồi khóa giường tăng dần. */
    public void cancelResidentHolds(long residentId, Reason reason, Long retainedBedId) {
        locks.lock(residentId, List.of());
        var bedIds = index.current(residentId);
        locks.lock(residentId, bedIds);
        for (long bedId : bedIds) {
            if (Objects.equals(retainedBedId, bedId)) continue;
            for (var held : store.waitingForResidentCurrent(bedId, residentId)) {
                state.transition(
                        bedId,
                        command(
                                BedEvent.HOLD_CANCELLED,
                                reason,
                                new Basis("FR-014", null, held.waitingId(), null),
                                "WAITING_RECORD",
                                held.waitingId(),
                                null,
                                null,
                                null));
                becameEmpty(bedId, "HOLD_CANCELLED");
            }
        }
    }

    public void extendExpectedEnd(long residentId, Instant end) {
        locks.lock(residentId, List.of());
        var active = store.active(residentId);
        locks.lock(residentId, List.of(active.bedId()));
        active = store.active(residentId);
        if (store.extensionOverlaps(active.allocationId(), end))
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("FR-019", "Gia hạn chồng khoảng phân bổ khác")));
        store.extend(active.allocationId(), end);
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("M03_EXPECTED_END_EXTENDED", false))
                .entity("M03.BED_ALLOCATION", active.allocationId())
                .after(end == null ? Map.of() : Map.of("expectedEnd", end.toString()))
                .basis(new Basis("FR-015", null, null, null))
                .build());
    }

    void becameEmpty(long bedId, String reason) {
        var asset = assets.getIfAvailable();
        if (!beds.hasFutureAllocation(bedId) && asset != null && !asset.awaitingResidentTransfer(bedId))
            events.publishEvent(new BedBecameEmpty(bedId, reason, time.now()));
    }

    BedTransition command(
            BedEvent event,
            Reason reason,
            Basis basis,
            String type,
            long id,
            Instant planned,
            BedHold hold,
            Long closed) {
        return new BedTransition(
                event,
                reason,
                basis,
                type,
                id,
                planned,
                user.isScheduler() ? null : user.accountId(),
                hold,
                closed,
                null,
                null,
                null,
                false,
                false);
    }
}
