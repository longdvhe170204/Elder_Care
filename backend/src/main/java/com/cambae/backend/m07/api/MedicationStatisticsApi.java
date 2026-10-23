package com.cambae.backend.m07.api;

import com.cambae.backend.m07.api.dto.DoseReportView;
import com.cambae.backend.m07.api.dto.ReconciliationReportView;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import java.time.Instant;
import java.util.stream.Stream;

/**
 * API báo cáo và thống kê về thuốc (ThuocThongKeApi, FR-051).
 * Dùng bởi Module M14 (016 - Báo cáo & Dashboard).
 */
public interface MedicationStatisticsApi {
    /**
     * Dòng thống kê liều thuốc trong khoảng thời gian theo phạm vi lọc.
     */
    Stream<DoseReportView> dosesInPeriod(ScopeFilter scopeFilter, Instant from, Instant to);

    /**
     * Dòng thống kê phiếu đối chiếu thuốc trong khoảng thời gian.
     */
    Stream<ReconciliationReportView> reconciliationsInPeriod(ScopeFilter scopeFilter, Instant from, Instant to);
}
