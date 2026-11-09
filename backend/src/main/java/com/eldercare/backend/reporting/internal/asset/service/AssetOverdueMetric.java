package com.eldercare.backend.reporting.internal.asset.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.roombedallocation.api.AssetStatisticsApi;
import com.eldercare.backend.roombedallocation.api.dto.AssetFlag;
import com.eldercare.backend.roombedallocation.api.dto.OverdueAssetView;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ tiêu {@code ASSET.OVERDUE} ({@code TS.QUA_HAN}; FR-020 dòng "Tài sản quá hạn", 019 FR-014): dấu quá hạn bảo trì,
 * đăng kiểm, bảo hiểm đang có trên tài sản lúc xem (003 trả dấu hiện tại có hạn trước hôm nay, không gồm tài sản Đã
 * thanh lý), chia theo loại dấu; tài sản mang hai dấu đếm ở cả hai ô. Dấu "chờ chuyển người" không phải dấu quá hạn.
 * Tầng theo vị trí hiện hành của tài sản. Phần "tới mốc nhắc CFG-M03-09" chưa có nguồn (plan 016, ghi chú 2026-11-08).
 */
@Component
@Transactional(readOnly = true)
public class AssetOverdueMetric implements MetricSource<AssetOverdueMetric.AssetRecord> {

    public static final String CODE = "ASSET.OVERDUE";

    /** Một dấu quá hạn của một tài sản. */
    public record AssetRecord(OverdueAssetView view) implements MetricRecord {

        @Override
        public String sourceKey() {
            return "M03.ASSET:" + view.assetId() + ":" + view.flag().name();
        }

        @Override
        public Instant referenceTime() {
            return null;
        }

        @Override
        public Long floorAreaId() {
            return view.floorAreaId();
        }

        @Override
        public Long residentId() {
            return null;
        }
    }

    private static final Metric<AssetRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.ASSETS,
            "Tài sản quá hạn",
            "Dấu quá hạn bảo trì, đăng kiểm, bảo hiểm đang có trên tài sản lúc xem, chia theo loại dấu; tài sản Đã thanh"
                    + " lý không tính",
            "003 AssetStatisticsApi.overdue",
            new Metric.Computation.Count<>(r -> true),
            r -> r.view().flag().name(),
            r -> {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("code", r.view().code());
                result.put("name", r.view().name());
                result.put("departmentId", r.view().departmentId());
                result.put("dueOn", r.view().dueOn());
                return new DetailRow(
                        r.sourceKey(),
                        "019",
                        "ASSET",
                        r.view().flag().name(),
                        null,
                        r.floorAreaId(),
                        null,
                        null,
                        null,
                        null,
                        result,
                        null,
                        Map.of(),
                        false,
                        false,
                        false);
            });

    private final ObjectProvider<AssetStatisticsApi> statistics;
    private final BusinessTime time;

    public AssetOverdueMetric(ObjectProvider<AssetStatisticsApi> statistics, BusinessTime time) {
        this.statistics = statistics;
        this.time = time;
    }

    @Override
    public Metric<AssetRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<AssetRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        AssetStatisticsApi api = statistics.getIfAvailable();
        if (api == null) {
            return Stream.empty();
        }
        return api.overdue(rough, time.today())
                .filter(v -> v.flag() != AssetFlag.AWAITING_RESIDENT_TRANSFER)
                .map(AssetRecord::new);
    }
}
