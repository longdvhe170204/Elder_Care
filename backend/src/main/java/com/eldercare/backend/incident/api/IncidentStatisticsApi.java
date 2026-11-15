package com.eldercare.backend.incident.api;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.incident.api.dto.AlertReportView;
import com.eldercare.backend.incident.api.dto.IncidentReportView;
import com.eldercare.backend.incident.api.dto.IsolationZoneView;
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

    /**
     * Số sự cố Đã hủy tạo trong khoảng, theo phạm vi như {@link #incidents}: chỉ cho dòng tham khảo "số sự cố đã hủy ghi
     * nhận" của 016 FR-042, không vào chỉ tiêu nào (bổ sung 2026-11-10, "Đổi api" duyệt 2026-11-08).
     */
    int cancelledCount(ScopeFilter scope, Instant from, Instant to);

    /**
     * Vùng đang khoanh (mục 1.8 {@code dangKhoanh}) có tầng hoặc khu nằm trong phạm vi; toàn viện thì mọi vùng (016
     * FR-020 {@code HEALTH.CONTAINMENT_ZONES}; bổ sung 2026-11-10, "Đổi api" duyệt 2026-11-08).
     */
    List<IsolationZoneView> containmentZones(ScopeFilter scope);
}
