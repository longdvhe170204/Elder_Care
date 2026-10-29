package com.eldercare.backend.medication.api;

import com.eldercare.backend.medication.api.dto.DosePendingHandoverView;
import com.eldercare.backend.medication.api.dto.DoseStatus;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * API for querying medication doses for shift handover (FR-028, BR-M09-06, Q-307).
 * Used by Module M09 (008 - Nursing Shift Handover).
 */
public interface DoseHandoverApi {
    /**
     * List of doses requiring handover in shift: Late, Missed, Refused, Carried, Special Control pending review.
     */
    List<DosePendingHandoverView> inShift(long floorAreaId, long shiftId);

    /**
     * Query current status of a set of dose keys (taking corrections into account).
     */
    Map<String, DoseStatus> currentStatuses(Collection<String> doseKeys);
}
