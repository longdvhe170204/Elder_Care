package com.eldercare.backend.health.api;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.health.api.dto.VitalSignReportView;
import java.time.Instant;
import java.util.stream.Stream;

/** Nguồn thống kê chỉ số cho 016; bản đo lại thay bản đầu, tính cả chưa đo lại (FR-081). */
public interface VitalSignStatisticsApi {
    Stream<VitalSignReportView> readings(ScopeFilter scope, Instant from, Instant to);
}
