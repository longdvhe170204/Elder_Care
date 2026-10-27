package com.cambae.backend.m04.api;

import com.cambae.backend.m04.api.dto.*;
import java.time.*;
import java.util.*;
import org.springframework.transaction.annotation.*;

/** T003: contracts 005 mục 1.2–1.10a; hành vi do story tương ứng cài. */
@Transactional(propagation = Propagation.MANDATORY)
public interface ShiftTaskApi {
    void onShiftAbsence(long staffId, long shiftId);

    void onApprovedLeave(long staffId, Set<Long> shiftIds);

    void onEmploymentEnded(long staffId, Instant from);

    void onAssignmentChanged(long residentId, long shiftId);

    void onAssignmentEnded(long assignmentId, long shiftId);

    HandoverConfirmationResult onHandoverConfirmed(
            long handoverId, long previousShiftId, long nextShiftId, Set<Long> handoverTaskIds, Instant at);
}
