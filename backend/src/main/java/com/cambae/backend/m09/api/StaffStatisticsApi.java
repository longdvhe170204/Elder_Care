package com.cambae.backend.m09.api;

import com.cambae.backend.m09.api.dto.*;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.stream.Stream;

/** Hợp đồng đọc thống kê 008 mục 1.4; 016 dùng bộ lọc phạm vi do 002 cấp. */
public interface StaffStatisticsApi {
    Stream<StaffingRatioView> staffingRatios(ScopeFilter scope, LocalDate from, LocalDate to);

    Stream<HandoverReportView> handovers(ScopeFilter scope, LocalDate from, LocalDate to);

    Stream<ExpiringCredentialView> expiringCredentials(int withinDays);

    Stream<DutyBasisView> nonRoleBases(ScopeFilter scope, Instant from, Instant to);

    Stream<AssignmentHistoryView> assignmentHistory(ScopeFilter scope, Instant from, Instant to);
}
