package com.cambae.backend.m04.api;

import com.cambae.backend.m04.api.dto.HandoverTransferResult;
import java.time.*;
import java.util.*;
import org.springframework.transaction.annotation.*;

/** T003: contracts 005 mục 1.2–1.10a; hành vi do story tương ứng cài. */
@Transactional(propagation = Propagation.MANDATORY)
public interface ShiftTaskApi {
    void onAbsence(long staffId, long shiftId);

    void onApprovedLeave(long staffId, Set<Long> shiftIds);

    void onResignation(long staffId, Instant from);

    void onAssignmentChanged(long residentId, long shiftId);

    void onAssignmentEnded(long assignmentId, long shiftId);

    HandoverTransferResult onHandoverConfirmed(
            long handoverId, long previousShiftId, Long nextShiftId, Set<Long> handoverTaskIds, Instant at);
}
