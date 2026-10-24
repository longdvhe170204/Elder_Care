package com.cambae.backend.m14;

import com.cambae.backend.m14.internal.metric.domain.DetailRow;
import com.cambae.backend.m14.internal.metric.domain.Metric;
import com.cambae.backend.m14.internal.metric.domain.MetricGroup;
import com.cambae.backend.m14.internal.metric.domain.MetricRecord;
import com.cambae.backend.m14.internal.metric.domain.MetricSource;
import com.cambae.backend.m14.internal.metric.domain.ReportFilters;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Chỉ tiêu giả của nhóm Liều thuốc (báo cáo Sức khỏe, thuốc) cho test xuất, thay {@code MED.*} tới khi 006 có
 * {@code MedicationStatisticsApi} kèm bản giả (docs/team-plan.md mục 8). Dòng chi tiết có họ tên người cao tuổi; chia
 * nhóm theo ca.
 */
@TestConfiguration
class TestDoseMetric {

    static final String CODE = "TEST.MISSED_DOSES";

    record Dose(
            String sourceKey, Instant referenceTime, Long floorAreaId, Long residentId, String resident, String shift)
            implements MetricRecord {}

    /** Bản ghi test đặt sẵn; nguồn lọc thô theo tầng, người cao tuổi như {@code *ThongKeApi}. */
    static final class DoseSource implements MetricSource<Dose> {

        final List<Dose> doses = new CopyOnWriteArrayList<>();

        private final Metric<Dose> metric = new Metric<>(
                CODE,
                MetricGroup.MEDICATION_DOSES,
                "Liều Bỏ lỡ (test)",
                "Số liều Bỏ lỡ trong khoảng",
                "006 (giả)",
                new Metric.Computation.Count<>(d -> true),
                Dose::shift,
                d -> new DetailRow(
                        d.sourceKey(),
                        "006",
                        "DOSE",
                        "MISSED",
                        d.referenceTime(),
                        d.floorAreaId(),
                        "Tầng " + d.floorAreaId(),
                        d.residentId(),
                        d.resident(),
                        "Điều dưỡng A",
                        Map.of(),
                        null,
                        Map.of(),
                        false,
                        false,
                        true));

        @Override
        public Metric<Dose> metric() {
            return metric;
        }

        @Override
        public Stream<Dose> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
            return doses.stream()
                    .filter(d -> rough.facilityWide() || rough.floorAreaIds().contains(d.floorAreaId()))
                    .filter(d -> from == null
                            || !d.referenceTime().isBefore(from)
                                    && d.referenceTime().isBefore(to));
        }
    }

    @Bean
    DoseSource testDoseSource() {
        return new DoseSource();
    }
}
