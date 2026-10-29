package com.eldercare.backend.roombedallocation.internal.bed.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.dto.*;
import com.eldercare.backend.roombedallocation.api.events.FutureAllocationCancelledForDamagedBed;
import com.eldercare.backend.roombedallocation.internal.allocation.service.AllocationLocks;
import com.eldercare.backend.roombedallocation.internal.bed.domain.*;
import com.eldercare.backend.roombedallocation.internal.bed.persistence.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Nhánh trạng thái độc lập T042. Adapter port nối thêm FR-013b/chuyển theo lịch khi đủ phụ thuộc. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class BedAssetStateService {
    private final BedAssetStateStore allocations;
    private final BedStateStore beds;
    private final BedStateService states;
    private final AllocationLocks locks;
    private final BedHoldService holds;
    private final CurrentUser user;
    private final ObjectProvider<BedAssetPort> assets;
    private final BusinessTime time;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;

    public BedAssetStateService(
            BedAssetStateStore allocations,
            BedStateStore beds,
            BedStateService states,
            AllocationLocks locks,
            BedHoldService holds,
            CurrentUser user,
            ObjectProvider<BedAssetPort> assets,
            BusinessTime time,
            AuditLogApi audit,
            ApplicationEventPublisher events) {
        this.allocations = allocations;
        this.beds = beds;
        this.states = states;
        this.locks = locks;
        this.holds = holds;
        this.user = user;
        this.assets = assets;
        this.time = time;
        this.audit = audit;
        this.events = events;
    }

    public void apply(
            long bedId, BedAssetCommand command, Basis basis, boolean damageWhilePreparing, boolean repeatedDamage) {
        var initial = allocations.future(bedId, false);
        var residents = initial.stream()
                .map(BedAssetStateStore.FutureAllocation::residentId)
                .distinct()
                .sorted()
                .toList();
        for (long resident : residents) locks.lock(resident, List.of());
        var bed = beds.lock(bedId);
        var current = allocations.future(bedId, true);
        if (!residents.containsAll(current.stream()
                .map(BedAssetStateStore.FutureAllocation::residentId)
                .toList()))
            throw new CannotAcquireLockException(
                    "Phân bổ đã đổi trong khi lấy khóa; cần thử lại transaction của lệnh nguồn");
        var failures = new ArrayList<UnmetCondition>();
        if (allocations.hasActive(bedId) || bed.state() == BedStatus.OCCUPIED || bed.state() == BedStatus.HELD)
            failures.add(new UnmetCondition("BR-M03-05", "Giường đang có người hoặc đang giữ chỗ"));
        boolean preparationState = bed.state() == BedStatus.EMPTY || bed.state() == BedStatus.AWAITING_READY;
        var source = assets.getIfAvailable();
        boolean repeatAllowed = repeatedDamage && source != null && source.awaitingResidentTransfer(bedId);
        boolean cancelAllowed = preparationState
                && (command == BedAssetCommand.RETIREMENT
                        || command == BedAssetCommand.MAINTENANCE && (damageWhilePreparing || repeatAllowed));
        if (!current.isEmpty() && command != BedAssetCommand.READY && !cancelAllowed)
            failures.add(new UnmetCondition("FR-013", "Giường có phân bổ tương lai; chưa thỏa ngoại lệ giường hỏng"));
        var event = switch (command) {
            case MAINTENANCE -> BedEvent.ASSET_MAINTENANCE;
            case RETIREMENT -> BedEvent.ASSET_RETIRED;
            case READY -> BedEvent.ASSET_READY;
        };
        try {
            BedTransitions.next(
                    bed.state(),
                    bed.holdReason(),
                    event,
                    command == BedAssetCommand.READY ? beds.maintenanceSource(bedId) : null,
                    false);
        } catch (IllegalStateException error) {
            failures.add(new UnmetCondition("FR-011", "Chuyển trạng thái tài sản không hợp lệ với giường"));
        }
        if (command == BedAssetCommand.READY && !bed.roomActive())
            failures.add(new UnmetCondition("FR-013", "Phòng đã ngừng hiệu lực"));
        if (!failures.isEmpty()) throw new ConditionNotMetException(failures);
        if (cancelAllowed && current.stream().anyMatch(BedAssetStateStore.FutureAllocation::hasLinkedSource))
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "SOURCE_UNAVAILABLE",
                    "Chưa nối xử lý căn cứ hoặc yêu cầu chuyển theo lịch; giữ nguyên phân bổ");
        if (cancelAllowed) {
            for (var allocation : current) {
                allocations.cancel(allocation.id(), user.accountId());
                audit.write(AuditLogEntry.builder()
                        .action(new AuditAction("M03_FUTURE_ALLOCATION_CANCELLED", false))
                        .entity("M03.BED_ALLOCATION", allocation.id())
                        .before(Map.of("status", "FUTURE"))
                        .after(Map.of("status", "CANCELLED", "cancellationReason", "Giường hỏng"))
                        .basis(basis)
                        .build());
            }
        }
        var change =
                states.transition(bedId, holds.command(event, null, basis, "ASSET_COMMAND", bedId, null, null, null));
        if (cancelAllowed && !current.isEmpty())
            events.publishEvent(new FutureAllocationCancelledForDamagedBed(
                    bedId,
                    current.stream()
                            .map(BedAssetStateStore.FutureAllocation::residentId)
                            .distinct()
                            .toList(),
                    new BedNotificationContext(
                            time.now(),
                            bed.floorId(),
                            List.of(
                                    new RecipientGroup.FacilityRole(Role.OFFICE_STAFF),
                                    new RecipientGroup.FloorManager(bed.floorId(), new Substitution.Default())))));
        if (change.after() == BedStatus.EMPTY) holds.becameEmpty(bedId, "ASSET_READY");
    }
}
