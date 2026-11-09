package com.eldercare.backend.reporting.internal.resident.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.reporting.internal.resident.service.ResidentRecords.MovementRecord;
import java.time.Instant;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ tiêu {@code RES.MOVEMENTS} ({@code NCT.BIEN_DONG}; FR-023): biến động theo lệnh; lệnh ghi nhận quay về tách theo
 * trạng thái trước. Đổi loại lưu trú (004) chưa có nguồn sự kiện ở {@code ResidencyStatisticsApi} (plan 016, ghi chú
 * 2026-11-08).
 */
@Component
@Transactional(readOnly = true)
public class MovementsMetric implements MetricSource<MovementRecord> {

    public static final String CODE = "RES.MOVEMENTS";

    private static final Metric<MovementRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.RESIDENTS,
            "Biến động người cao tuổi",
            "Số lần Hoàn tất tiếp nhận, Hủy tiếp nhận, Kết thúc lưu trú, Qua đời, bắt đầu và kết thúc Tạm vắng, Điều trị tại bệnh viện, đổi mức chăm sóc, theo thời điểm hiệu lực trong khoảng",
            "001 ResidentStatisticsApi.changes",
            new Metric.Computation.Count<>(r -> true),
            MovementRecord::kind,
            ResidentRecords::movementRow);

    private final ResidentRecords records;

    public MovementsMetric(ResidentRecords records) {
        this.records = records;
    }

    @Override
    public Metric<MovementRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<MovementRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return records.movements(rough, from, to);
    }
}
