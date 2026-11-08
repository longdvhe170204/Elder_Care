package com.eldercare.backend.reporting.internal.resident.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.reporting.internal.resident.service.ResidentRecords.ResidentAtDateRecord;
import java.time.Instant;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ tiêu {@code RES.IN_RESIDENCE} ({@code NCT.DANG_LUU_TRU}; FR-020): người đang lưu trú gồm vắng tạm thời, chia theo
 * trạng thái. Phần "người bán trú có lịch hôm nay theo trạng thái có mặt" (005) chưa làm (plan 016, ghi chú 2026-11-08).
 */
@Component
@Transactional(readOnly = true)
public class InResidenceMetric implements MetricSource<ResidentAtDateRecord> {

    public static final String CODE = "RES.IN_RESIDENCE";

    private static final Metric<ResidentAtDateRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.RESIDENTS,
            "Người đang lưu trú",
            "Số hồ sơ Đang lưu trú, Tạm vắng, Hoạt động bên ngoài, Điều trị tại bệnh viện tại ngày cuối của khoảng (dashboard: hôm nay), chia theo trạng thái 5.5; loại lưu trú, mức chăm sóc ở dòng chi tiết",
            "001 ResidentStatisticsApi.statusAtDate; 004 ResidencyStatisticsApi.residenceTypeAtDate",
            new Metric.Computation.Count<>(r -> true),
            r -> r.status().name(),
            ResidentRecords::atDateRow);

    private final ResidentRecords records;

    public InResidenceMetric(ResidentRecords records) {
        this.records = records;
    }

    @Override
    public Metric<ResidentAtDateRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<ResidentAtDateRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return records.atDate(rough, to, filters.residenceType())
                .filter(r -> ResidentRecords.IN_RESIDENCE.contains(r.status()));
    }
}
