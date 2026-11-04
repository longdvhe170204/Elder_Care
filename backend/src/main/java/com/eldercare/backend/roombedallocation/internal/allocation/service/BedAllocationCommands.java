package com.eldercare.backend.roombedallocation.internal.allocation.service;

import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.roombedallocation.api.BedAllocationCommandApi;
import com.eldercare.backend.roombedallocation.api.dto.*;
import com.eldercare.backend.roombedallocation.internal.bed.service.BedHoldService;
import java.time.*;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Facade 003 cho lệnh nguồn004; mọi tác động cùng transaction nguồn. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class BedAllocationCommands implements BedAllocationCommandApi {
    private final BedHoldService holds;
    private final ScheduledBedTransfers transfers;

    public BedAllocationCommands(BedHoldService holds, ScheduledBedTransfers transfers) {
        this.holds = holds;
        this.transfers = transfers;
    }

    @Override
    public void holdForWaitingRecord(long waiting, long resident, long bed, Instant at) {
        holds.holdForWaitingRecord(waiting, resident, bed, at);
    }

    @Override
    public void cancelHold(long waiting, HoldCancellationReason reason) {
        holds.cancelHold(waiting, new Reason(reason.code()));
    }

    @Override
    public void releaseWhenAbsent(long resident, Instant at, Basis basis) {
        holds.releaseWhenAbsent(resident, at, basis);
    }

    @Override
    public void extendExpectedEnd(long resident, Instant end) {
        holds.extendExpectedEnd(resident, end);
    }

    @Override
    public List<UnmetCondition> checkScheduledReservation(long resident, long bed, LocalDate date) {
        return transfers.checkReservation(resident, bed, date);
    }

    @Override
    public long reserveScheduledTransfer(long request, long resident, long bed, LocalDate date) {
        return transfers.reserve(request, resident, bed, date);
    }

    @Override
    public List<UnmetCondition> checkScheduledTransfer(long request) {
        return transfers.checkTransfer(request);
    }

    @Override
    public ScheduledTransferResult applyScheduledTransfer(long request, Instant planned) {
        return transfers.apply(request, planned);
    }

    @Override
    public void cancelScheduledAllocation(long request, Reason reason) {
        transfers.cancel(request, reason);
    }

    @Override
    public void requestCreatedAfterUrgentTransfer(long allocation, long request) {
        transfers.requestCreated(allocation, request);
    }

    @Override
    public void urgentTransferRequestResult(long allocation, boolean approved) {
        transfers.requestResult(allocation, approved);
    }
}
