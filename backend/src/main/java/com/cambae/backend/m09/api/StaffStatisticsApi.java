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

    /**
     * Bàn giao đang mở lúc xem (016 FR-020): Đã lập hoặc Có ý kiến, cộng Bản nháp của ca đang Chờ bàn giao; không giới hạn
     * ngày ca (luật "Đổi api", chủ dự án duyệt 2026-10-27).
     */
    Stream<HandoverReportView> openHandovers(ScopeFilter scope);

    Stream<ExpiringCredentialView> expiringCredentials(int withinDays);

    Stream<DutyBasisView> nonRoleBases(ScopeFilter scope, Instant from, Instant to);

    Stream<AssignmentHistoryView> assignmentHistory(ScopeFilter scope, Instant from, Instant to);
}
