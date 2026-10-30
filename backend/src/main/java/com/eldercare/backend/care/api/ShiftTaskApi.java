package com.eldercare.backend.care.api;

import com.eldercare.backend.care.api.dto.HandoverTransferResult;
import java.time.Instant;
import java.util.Set;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Hợp đồng 005 mục 1.4; mọi callback tham gia transaction của lệnh gọi. */
@Transactional(propagation = Propagation.MANDATORY)
public interface ShiftTaskApi {
    void onAbsence(long staffId, long shiftId);

    void onApprovedLeave(long staffId, Set<Long> shiftIds);

    void onResignation(long staffId, Instant effectiveAt);

    void onAssignmentChanged(long residentId, long shiftId);

    void onAssignmentEnded(long assignmentId, long shiftId);

    HandoverTransferResult onHandoverConfirmed(
            long handoverId, long previousShiftId, Long nextShiftId, Set<Long> taskIds, Instant at);
}
