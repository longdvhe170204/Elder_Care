package com.eldercare.backend.medication.api;

import com.eldercare.backend.medication.api.dto.IntakeResultView;
import java.util.List;

/**
 * API for supplied medication intake results (FR-054, Q-337).
 * Used by Module M11/M03 (019 - Procurement & Assets).
 */
public interface ProcurementIntakeApi {
    /**
     * List of intake results (pass/fail) by procurement request.
     */
    List<IntakeResultView> byProcurementRequest(long procurementRequestId);
}
