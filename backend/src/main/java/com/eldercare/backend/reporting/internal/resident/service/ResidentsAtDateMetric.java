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
 * Chỉ tiêu {@code RES.AT_DATE} ({@code NCT.TAI_NGAY}; FR-022, Q-20, Q-22): người cao tuổi tại một ngày theo giá trị hiệu
 * lực tại ngày đó, không theo giá trị hiện tại; chia theo loại lưu trú.
 */
@Component
@Transactional(readOnly = true)
public class ResidentsAtDateMetric implements MetricSource<ResidentAtDateRecord> {

    public static final String CODE = "RES.AT_DATE";

    private static final Metric<ResidentAtDateRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.RESIDENTS,
            "Người cao tuổi tại ngày",
            "Số hồ sơ theo trạng thái 5.5, loại lưu trú, mức chăm sóc có hiệu lực tại ngày cuối của khoảng, chia theo loại lưu trú (theo hợp đồng Hiệu lực tại ngày; chưa có hợp đồng là chưa xác định); trạng thái, mức ở dòng chi tiết",
            "001 ResidentStatisticsApi.statusAtDate; 004 ResidencyStatisticsApi.residenceTypeAtDate",
            new Metric.Computation.Count<>(r -> true),
            ResidentAtDateRecord::residenceType,
            ResidentRecords::atDateRow);

    private final ResidentRecords records;

    public ResidentsAtDateMetric(ResidentRecords records) {
        this.records = records;
    }

    @Override
    public Metric<ResidentAtDateRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<ResidentAtDateRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        return records.atDate(rough, to, filters.residenceType());
    }
}
