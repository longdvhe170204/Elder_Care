package com.eldercare.backend.roombedallocation.internal.allocation.service;

import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.roombedallocation.api.BedAllocationCommandApi;
import com.eldercare.backend.roombedallocation.api.dto.HoldCancellationReason;
import com.eldercare.backend.roombedallocation.api.dto.ScheduledTransferResult;
import com.eldercare.backend.roombedallocation.internal.allocation.persistence.AllocationCommandStore;
import com.eldercare.backend.roombedallocation.internal.bed.service.BedHoldService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 003 T033 (hợp đồng mục 1.4 {@code PhanBoGiuongLenhApi}): bản cài {@link BedAllocationCommandApi} cho lệnh của 004,
 * chạy trong transaction của lệnh gọi. Lượt 1 (2026-11-06): giữ chỗ hồ sơ chờ, hủy giữ chỗ, giải phóng khi vắng, gia hạn
 * kết thúc dự kiến qua {@link BedHoldService}; mã lý do hủy giữ chỗ của 004 ghi nguyên vào lịch sử giường. Lượt 2:
 * chuyển theo lịch và kết quả yêu cầu sau chuyển gấp qua {@link ScheduledTransfers}.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class BedAllocationCommands implements BedAllocationCommandApi {
    private final BedHoldService holds;
    private final ScheduledTransfers scheduled;
    private final AllocationCommandStore allocations;
    private final AllocationSuitabilityService suitability;

    public BedAllocationCommands(
            BedHoldService holds,
            ScheduledTransfers scheduled,
            AllocationCommandStore allocations,
            AllocationSuitabilityService suitability) {
        this.holds = holds;
        this.scheduled = scheduled;
        this.allocations = allocations;
        this.suitability = suitability;
    }

    @Override
    public void holdForWaitingRecord(long waitingRecordId, long residentId, long bedId, Instant at) {
        holds.holdForWaitingRecord(waitingRecordId, residentId, bedId, at);
    }

    @Override
    public void cancelHold(long waitingRecordId, HoldCancellationReason reason) {
        holds.cancelHold(waitingRecordId, new Reason(reason.code()));
    }

    @Override
    public void releaseWhenAbsent(long residentId, Instant at, Basis basis) {
        holds.releaseWhenAbsent(residentId, at, basis);
    }

    @Override
    public void extendExpectedEnd(long residentId, Instant expectedEnd) {
        // Bán trú hoặc chưa có giường: không có phân bổ để gia hạn (chủ dự án chốt 2026-11-06)
        if (allocations.openCurrent(residentId, true, false).isEmpty()) return;
        holds.extendExpectedEnd(residentId, expectedEnd);
    }

    @Override
    public List<UnmetCondition> checkScheduledReservation(long residentId, long nextBedId, LocalDate effectiveDate) {
        return scheduled.checkReservation(residentId, nextBedId, effectiveDate);
    }

    @Override
    public long reserveScheduledTransfer(long requestId, long residentId, long nextBedId, LocalDate effectiveDate) {
        return scheduled.reserve(requestId, residentId, nextBedId, effectiveDate);
    }

    @Override
    public List<UnmetCondition> checkScheduledTransfer(long requestId) {
        return scheduled.checkTransfer(requestId);
    }

    @Override
    public ScheduledTransferResult applyScheduledTransfer(long requestId, Instant plannedAt) {
        return scheduled.apply(requestId, plannedAt);
    }

    @Override
    public void cancelScheduledAllocation(long requestId, Reason reason) {
        scheduled.cancel(requestId, reason);
    }

    @Override
    public void requestCreatedAfterUrgentTransfer(long allocationId, long requestId) {
        scheduled.requestCreated(allocationId, requestId);
    }

    @Override
    public void urgentTransferRequestResult(long allocationId, boolean approved) {
        scheduled.requestResult(allocationId, approved);
    }

    @Override
    public void urgentTransferRequestWithdrawn(long allocationId) {
        scheduled.requestWithdrawn(allocationId);
    }

    @Override
    public void residenceTypeChanged(long residentId) {
        suitability.reassess(residentId);
    }
}
