package com.eldercare.backend.roombedallocation.api;

import com.eldercare.backend.administration.auditlog.api.Basis;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.roombedallocation.api.dto.HoldCancellationReason;
import com.eldercare.backend.roombedallocation.api.dto.ScheduledTransferResult;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Hợp đồng 003 BedAllocationCommandApi (T003); bản cài thuộc user story tương ứng. */
public interface BedAllocationCommandApi {
    @Transactional(propagation = Propagation.MANDATORY)
    void holdForWaitingRecord(long waitingRecordId, long residentId, long bedId, Instant at);

    @Transactional(propagation = Propagation.MANDATORY)
    void cancelHold(long waitingRecordId, HoldCancellationReason reason);

    @Transactional(propagation = Propagation.MANDATORY)
    void releaseWhenAbsent(long residentId, Instant at, Basis basis);

    @Transactional(propagation = Propagation.MANDATORY)
    void extendExpectedEnd(long residentId, Instant expectedEnd);

    @Transactional(propagation = Propagation.MANDATORY)
    List<UnmetCondition> checkScheduledReservation(long residentId, long nextBedId, LocalDate effectiveDate);

    @Transactional(propagation = Propagation.MANDATORY)
    long reserveScheduledTransfer(long requestId, long residentId, long nextBedId, LocalDate effectiveDate);

    @Transactional(propagation = Propagation.MANDATORY)
    List<UnmetCondition> checkScheduledTransfer(long requestId);

    @Transactional(propagation = Propagation.MANDATORY)
    ScheduledTransferResult applyScheduledTransfer(long requestId, Instant plannedAt);

    @Transactional(propagation = Propagation.MANDATORY)
    void cancelScheduledAllocation(long requestId, Reason reason);

    @Transactional(propagation = Propagation.MANDATORY)
    void requestCreatedAfterUrgentTransfer(long allocationId, long requestId);

    @Transactional(propagation = Propagation.MANDATORY)
    void urgentTransferRequestResult(long allocationId, boolean approved);
}
