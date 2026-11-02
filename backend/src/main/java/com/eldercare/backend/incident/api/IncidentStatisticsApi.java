package com.eldercare.backend.incident.api;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.incident.api.dto.AlertReportView;
import com.eldercare.backend.incident.api.dto.IncidentReportView;
import com.eldercare.backend.incident.api.dto.MonitoringView;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

/** Nguồn thống kê đã lọc phạm vi, khoảng [from,to), FR-081. */
public interface IncidentStatisticsApi {
    Stream<AlertReportView> alerts(ScopeFilter scope, Instant from, Instant to);

    Stream<IncidentReportView> incidents(ScopeFilter scope, Instant from, Instant to);

    List<MonitoringView> underMonitoring(ScopeFilter scope);

    List<Long> criticalResidentIds(ScopeFilter scope);
}
