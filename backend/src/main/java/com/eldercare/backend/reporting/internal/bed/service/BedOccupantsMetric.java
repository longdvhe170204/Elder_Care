package com.eldercare.backend.reporting.internal.bed.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.administration.account.api.spi.BedAllocationApi;
import com.eldercare.backend.administration.account.api.spi.BedAllocationView;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ tiêu {@code BED.OCCUPANTS} (FR-024): số người cao tuổi có phân bổ giường đang hiệu lực theo tầng, phòng (003
 * {@code BedAllocationApi.residentsIn}, {@code openAt}), lúc xem hoặc cuối ngày đã chọn; chia theo tầng, phòng ở dòng chi
 * tiết.
 */
@Component
@Transactional(readOnly = true)
public class BedOccupantsMetric implements MetricSource<BedOccupantsMetric.OccupantRecord> {

    public static final String CODE = "BED.OCCUPANTS";

    /** Một người cao tuổi với phân bổ đang hiệu lực. */
    public record OccupantRecord(long resident, BedAllocationView allocation, Instant at) implements MetricRecord {

        @Override
        public String sourceKey() {
            return "M03.BED_ALLOCATION:" + allocation.allocationId();
        }

        @Override
        public Instant referenceTime() {
            return at;
        }

        @Override
        public Long floorAreaId() {
            return allocation.floorAreaId();
        }

        @Override
        public Long roomId() {
            return allocation.roomId();
        }

        @Override
        public Long residentId() {
            return resident;
        }
    }

    private static final Metric<OccupantRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.BEDS,
            "Số người theo tầng, phòng",
            "Số người cao tuổi có phân bổ giường đang hiệu lực tại thời điểm xem hoặc cuối ngày đã chọn, theo tầng, phòng",
            "003 BedAllocationApi.residentsIn, openAt",
            new Metric.Computation.Count<>(r -> true),
            r -> Long.toString(r.allocation().floorAreaId()),
            r -> new DetailRow(
                    r.sourceKey(),
                    "003",
                    "BED_ALLOCATION",
                    null,
                    r.at(),
                    r.floorAreaId(),
                    null,
                    r.resident(),
                    null,
                    null,
                    Map.of(
                            "roomId",
                            r.allocation().roomId(),
                            "bedId",
                            r.allocation().bedId()),
                    null,
                    Map.of(),
                    false,
                    false,
                    false));

    private final ObjectProvider<BedAllocationApi> allocations;
    private final BusinessTime time;

    public BedOccupantsMetric(ObjectProvider<BedAllocationApi> allocations, BusinessTime time) {
        this.allocations = allocations;
        this.time = time;
    }

    @Override
    public Metric<OccupantRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<OccupantRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        BedAllocationApi api = allocations.getIfAvailable();
        if (api == null) {
            return Stream.empty();
        }
        Instant now = time.now();
        Instant at = to == null || to.isAfter(now) ? now : to.minus(1, ChronoUnit.MICROS);
        return api.residentsIn(rough, at).stream()
                .sorted()
                .flatMap(resident -> api.openAt(resident, at).map(a -> new OccupantRecord(resident, a, at)).stream());
    }
}
