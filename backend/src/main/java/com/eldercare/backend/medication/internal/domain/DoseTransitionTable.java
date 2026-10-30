package com.eldercare.backend.medication.internal.domain;

import com.eldercare.backend.common.error.BusinessException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;

/**
 * State transition validation for medication doses (data-model section 5, FR-021; research D-07).
 */
public final class DoseTransitionTable {

    private static final Map<DoseStatus, Set<DoseStatus>> ALLOWED_TRANSITIONS;

    static {
        Map<DoseStatus, Set<DoseStatus>> transitions = new EnumMap<>(DoseStatus.class);

        // UPCOMING (CHUA_DEN_GIO)
        transitions.put(
                DoseStatus.UPCOMING,
                EnumSet.of(
                        DoseStatus.DUE, // Window start reached
                        DoseStatus.CANCELLED, // Prescription discontinued/replaced/expired
                        DoseStatus.SUSPENDED, // Absence/leave starts
                        DoseStatus.LEAVE_MEDICATION // Leave handover assigned
                        ));

        // DUE (DEN_GIO)
        transitions.put(
                DoseStatus.DUE,
                EnumSet.of(
                        DoseStatus.ADMINISTERED, // Successfully administered
                        DoseStatus.REFUSED, // Refused by resident
                        DoseStatus.NOT_ADMINISTERED, // Skipped / not administered
                        DoseStatus.LATE, // Window end elapsed
                        DoseStatus.CANCELLED // Prescription discontinued/replaced
                        ));

        // LATE (TRE)
        transitions.put(
                DoseStatus.LATE,
                EnumSet.of(
                        DoseStatus.ADMINISTERED, // Late administration
                        DoseStatus.REFUSED, // Refused late
                        DoseStatus.NOT_ADMINISTERED, // Skipped
                        DoseStatus.MISSED, // Grace period CFG-M07-02 elapsed
                        DoseStatus.CANCELLED // Prescription cancelled
                        ));

        // SUSPENDED (TAM_DUNG)
        transitions.put(
                DoseStatus.SUSPENDED,
                EnumSet.of(
                        DoseStatus.UPCOMING, // Resumed before window
                        DoseStatus.DUE, // Resumed in window
                        DoseStatus.CANCELLED // Discontinued
                        ));

        // LEAVE_MEDICATION (MANG_THEO)
        transitions.put(
                DoseStatus.LEAVE_MEDICATION,
                EnumSet.of(
                        DoseStatus.UPCOMING, // Returned early before window
                        DoseStatus.DUE, // Returned early within window
                        DoseStatus.ADMINISTERED, // Recorded as administered
                        DoseStatus.NOT_ADMINISTERED, // Recorded as skipped
                        DoseStatus.CANCELLED // Discontinued
                        ));

        // Terminal states cannot transition to anything
        transitions.put(DoseStatus.ADMINISTERED, Collections.emptySet());
        transitions.put(DoseStatus.REFUSED, Collections.emptySet());
        transitions.put(DoseStatus.NOT_ADMINISTERED, Collections.emptySet());
        transitions.put(DoseStatus.MISSED, Collections.emptySet());
        transitions.put(DoseStatus.CANCELLED, Collections.emptySet());

        ALLOWED_TRANSITIONS = Collections.unmodifiableMap(transitions);
    }

    private DoseTransitionTable() {}

    /**
     * Checks if the transition from current status to target status is permitted.
     */
    public static boolean isAllowed(DoseStatus from, DoseStatus to) {
        if (from == null || to == null) {
            return false;
        }
        if (from == to) {
            return true;
        }
        Set<DoseStatus> targets = ALLOWED_TRANSITIONS.get(from);
        return targets != null && targets.contains(to);
    }

    /**
     * Validates transition; throws BusinessException with 409 CHUYEN_KHONG_HOP_LE if invalid.
     */
    public static void validateTransition(DoseStatus from, DoseStatus to) {
        if (!isAllowed(from, to)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "INVALID_TRANSITION",
                    "Cannot transition dose status from " + from + " to " + to + " (FR-021)");
        }
    }

    /**
     * Checks whether a status is terminal (locked against updates).
     */
    public static boolean isTerminal(DoseStatus status) {
        return status == DoseStatus.ADMINISTERED
                || status == DoseStatus.REFUSED
                || status == DoseStatus.NOT_ADMINISTERED
                || status == DoseStatus.MISSED
                || status == DoseStatus.CANCELLED;
    }
}
