package com.cambae.backend.m07.api;

import com.cambae.backend.m07.api.dto.DoseReportView;
import com.cambae.backend.m07.api.dto.ReconciliationReportView;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
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
