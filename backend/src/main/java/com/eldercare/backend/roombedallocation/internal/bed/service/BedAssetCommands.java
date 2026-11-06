package com.eldercare.backend.roombedallocation.internal.bed.service;

import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.notification.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.dto.BedStatus;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import com.eldercare.backend.roombedallocation.internal.allocation.service.AllocationChecker;
import com.eldercare.backend.roombedallocation.internal.allocation.service.ResidentAllocationFacts;
import com.eldercare.backend.roombedallocation.internal.bed.domain.BedAssetCommand;
import com.eldercare.backend.roombedallocation.internal.bed.domain.BedAssetCommandPort;
import com.eldercare.backend.roombedallocation.internal.bed.domain.FlaggedBedFutureAllocations;
import com.eldercare.backend.roombedallocation.internal.bed.persistence.BedAssetStateStore;
import com.eldercare.backend.roombedallocation.internal.bed.persistence.BedStateStore;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 019 T020, hợp đồng 003 mục 3: bản cài {@link BedAssetCommandPort} cho lệnh tài sản giường của 019, chạy trong
 * transaction của lệnh. {@link #applyAssetCommand} chỉ được gọi sau {@link #checkAssetCommand} không lỗi trong cùng
 * transaction: điều kiện ngoại lệ FR-013a (hư hỏng khi dọn giường, báo hỏng lại) đã kiểm ở bước kiểm, nên bước áp dụng
 * hủy phân bổ tương lai khi giường ở Trống, Chờ sẵn sàng.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class BedAssetCommands implements BedAssetCommandPort {
    private final BedAssetStateService states;
    private final BedAssetStateStore allocations;
    private final BedStateStore beds;
    private final BedHoldService holds;
    private final AllocationChecker checker;
    private final ResidentAllocationFacts facts;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;

    public BedAssetCommands(
            BedAssetStateService states,
            BedAssetStateStore allocations,
            BedStateStore beds,
            BedHoldService holds,
            AllocationChecker checker,
            ResidentAllocationFacts facts,
            BusinessTime time,
            ApplicationEventPublisher events) {
        this.states = states;
        this.allocations = allocations;
        this.beds = beds;
        this.holds = holds;
        this.checker = checker;
        this.facts = facts;
        this.time = time;
        this.events = events;
    }

    @Override
    public List<UnmetCondition> checkAssetCommand(
            long bedId, BedAssetCommand command, boolean damageWhilePreparing, boolean repeatedDamage) {
        return states.check(bedId, command, damageWhilePreparing, repeatedDamage);
    }

    @Override
    public void applyAssetCommand(long bedId, BedAssetCommand command, Basis basis) {
        states.apply(bedId, command, basis, true, true);
    }

    /** FR-013b: phân bổ tương lai đã có được giữ; báo Hành chính và Trưởng tầng kèm người bị ảnh hưởng. */
    @Override
    public void residentTransferFlagAdded(long bedId) {
        var residents = allocations.future(bedId, false).stream()
                .map(BedAssetStateStore.FutureAllocation::residentId)
                .distinct()
                .toList();
        if (residents.isEmpty()) return;
        var bed = beds.lock(bedId);
        var now = time.now();
        String title = "Giường chờ chuyển người có phân bổ tương lai của " + residents.size() + " người cao tuổi";
        events.publishEvent(new FlaggedBedFutureAllocations(
                bedId,
                residents,
                List.of(new NotificationRequest(
                        "M03",
                        "FLAGGED_BED_FUTURE_ALLOCATIONS",
                        SourceGroup.OTHER,
                        new SourceRecord("M03.BED", Long.toString(bedId)),
                        "FLAGGED_BED_FUTURE_ALLOCATIONS:" + bedId + ":" + now,
                        Optional.of(NotificationLevel.LOW),
                        Optional.empty(),
                        Optional.of(bed.floorId()),
                        List.of(
                                new RecipientGroup.FacilityRole(Role.OFFICE_STAFF),
                                new RecipientGroup.FloorManager(bed.floorId(), new Substitution.Default())),
                        List.of(new ContentPart(InformationType.GENERAL, title)),
                        Optional.of(title),
                        Optional.of(title),
                        Optional.empty(),
                        false,
                        now,
                        Map.of("bedId", bedId, "residentIds", residents),
                        false,
                        Optional.empty(),
                        Optional.empty()))));
    }

    /** FR-013b, FR-014: dấu gỡ khi giường Trống thì kích hoạt BR-M02-01 (trừ khi có phân bổ tương lai). */
    @Override
    public void residentTransferFlagRemoved(long bedId) {
        if (beds.lock(bedId).state() == BedStatus.EMPTY) holds.becameEmpty(bedId, "ASSET_FLAG_REMOVED");
    }

    @Override
    public BedStatus bedState(long bedId) {
        return allocations.state(bedId);
    }

    /** Q-256: giường Trống đạt FR-018 cho người đang nằm (hoặc được giữ chỗ), theo tầng. */
    @Override
    public List<LocationView> suitableEmptyBedsForOccupant(long bedId) {
        var occupant = allocations.occupant(bedId);
        if (occupant.isEmpty()) return List.of();
        long resident = occupant.get();
        // Không có mức chăm sóc thì mọi giường trượt FR-018 (a); khỏi kiểm từng giường
        if (facts.careLevel(resident).isEmpty()) return List.of();
        var now = time.now();
        return allocations.emptyBeds(bedId).stream()
                .filter(b -> checker.check(resident, b.bedId(), now, null, null, false)
                        .isEmpty())
                .toList();
    }
}
