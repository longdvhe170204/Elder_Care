package com.eldercare.backend.medication.api;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.medication.api.dto.DoseReportView;
import com.eldercare.backend.medication.api.dto.ReconciliationReportView;
import java.time.Instant;
import java.util.stream.Stream;

/**
 * API for medication reporting and statistics (FR-051).
 * Used by Module M14 (016 - Reports & Dashboard).
 */
public interface MedicationStatisticsApi {
    /**
     * Stream of dose statistics within a period by scope filter.
     */
    Stream<DoseReportView> dosesInPeriod(ScopeFilter scopeFilter, Instant from, Instant to);

    /**
     * Stream of reconciliation report rows within a period.
     */
    Stream<ReconciliationReportView> reconciliationsInPeriod(ScopeFilter scopeFilter, Instant from, Instant to);
}
