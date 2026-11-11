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
import com.eldercare.backend.roombedallocation.api.dto.OverdueAssetView;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ tiêu {@code ASSET.DUE_SOON} ({@code TS.TOI_MOC_NHAC}; FR-020 dòng "Tài sản quá hạn": số tài sản tới mốc nhắc
 * CFG-M03-09, BR-M03-17): hạn bảo trì, đăng kiểm, bảo hiểm trong cửa sổ nhắc lúc xem, chưa quá hạn, tài sản Đã thanh lý
 * không tính; chia theo loại hạn. Mã riêng với {@code ASSET.OVERDUE} vì mỗi mã một khóa chia (tiền lệ
 * {@code RES.DAY_CARE_ATTENDANCE}).
 */
@Component
@Transactional(readOnly = true)
public class AssetDueSoonMetric implements MetricSource<AssetDueSoonMetric.DueSoonRecord> {

    public static final String CODE = "ASSET.DUE_SOON";

    /** Một hạn tới mốc nhắc của một tài sản. */
    public record DueSoonRecord(OverdueAssetView view) implements MetricRecord {

        /** Loại hạn: tên dấu bỏ đuôi {@code _OVERDUE} (MAINTENANCE, VEHICLE_INSPECTION, INSURANCE). */
        public String dueKind() {
            return view.flag().name().replace("_OVERDUE", "");
        }

        @Override
        public String sourceKey() {
            return "M03.ASSET:" + view.assetId() + ":" + dueKind();
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

    private static final Metric<DueSoonRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.ASSETS,
            "Tài sản tới mốc nhắc",
            "Hạn bảo trì, đăng kiểm, bảo hiểm trong khoảng nhắc trước (CFG-M03-09) lúc xem, chưa quá hạn, chia theo loại"
                    + " hạn; tài sản Đã thanh lý không tính",
            "019 AssetStatisticsApi.dueSoon",
            new Metric.Computation.Count<>(r -> true),
            DueSoonRecord::dueKind,
            r -> {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("code", r.view().code());
                result.put("name", r.view().name());
                result.put("departmentId", r.view().departmentId());
                result.put("dueOn", r.view().dueOn());
                return DetailRow.simple(
                        r.sourceKey(), "019", "ASSET", r.dueKind(), null, r.floorAreaId(), null, result);
            });

    private final ObjectProvider<AssetStatisticsApi> statistics;
    private final BusinessTime time;

    public AssetDueSoonMetric(ObjectProvider<AssetStatisticsApi> statistics, BusinessTime time) {
        this.statistics = statistics;
        this.time = time;
    }

    @Override
    public Metric<DueSoonRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<DueSoonRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        AssetStatisticsApi api = statistics.getIfAvailable();
        if (api == null) {
            return Stream.empty();
        }
        return api.dueSoon(rough, time.today()).map(DueSoonRecord::new);
    }
}
