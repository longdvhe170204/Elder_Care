package com.cambae.backend.m07.internal.service;

import com.cambae.backend.m07.internal.domain.DoseStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pure consecutive refusal counter for medication doses (BR-M07-13, FR-032).
 * When consecutive refusals >= CFG-M05-04 (default 2), alert TU_CHOI_LIEN_TIEP is triggered.
 */
public final class ConsecutiveRefusalCounter {

    private ConsecutiveRefusalCounter() {}

    /**
     * Counts the number of consecutive REFUSED doses in reverse chronological order.
     * Skips SUSPENDED and CANCELLED doses.
     * Stops immediately when an ADMINISTERED, NOT_ADMINISTERED, or MISSED dose is encountered.
     */
    public static int count(List<ClosedDoseView> doses) {
        if (doses == null || doses.isEmpty()) {
            return 0;
        }

        List<ClosedDoseView> sorted = new ArrayList<>(doses);
        sorted.sort(Comparator.comparing(ClosedDoseView::scheduledTime).reversed());

        int count = 0;
        for (ClosedDoseView dose : sorted) {
            DoseStatus status = dose.status();
            if (status == null || status == DoseStatus.SUSPENDED || status == DoseStatus.CANCELLED) {
                // Skip non-actionable / suspended doses
                continue;
            }

            if (status == DoseStatus.REFUSED) {
                count++;
            } else {
                // Streak broken by any other closed status (ADMINISTERED, NOT_ADMINISTERED, MISSED)
                break;
            }
        }

        return count;
    }

    /**
     * Checks if the consecutive refusal count meets or exceeds the threshold.
     */
    public static boolean meetsThreshold(int count, int threshold) {
        return count >= threshold;
    }
}
