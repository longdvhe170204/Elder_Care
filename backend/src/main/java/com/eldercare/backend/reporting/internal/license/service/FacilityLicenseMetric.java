package com.eldercare.backend.reporting.internal.license.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.administration.account.api.spi.FacilityLicenseApi;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ tiêu {@code LICENSE.FACILITY} ({@code GP.CO_SO}; FR-020, US1 #6): giấy phép cơ sở hết hạn trong CFG-M06-02 (mặc
 * định 60 ngày), kèm số ngày còn lại; cùng mốc với cảnh báo giấy phép của 002 FR-039. Nguồn {@code FacilityLicenseApi}
 * (007 cài ở M06; chưa có bản cài thì không có giấy phép nào). Số liệu "lúc xem" của dashboard, nhóm FR-010 chỉ Quản lý
 * viện; không thuộc báo cáo nào nên không xuất được.
 */
@Component
@Transactional(readOnly = true)
public class FacilityLicenseMetric implements MetricSource<FacilityLicenseMetric.FacilityLicenseRecord> {

    public static final String CODE = "LICENSE.FACILITY";

    /** Một giấy phép cơ sở sắp hết hạn; không gắn tầng, người cao tuổi. */
    public record FacilityLicenseRecord(long licenseId, String name, LocalDate expiryDate, long daysRemaining)
            implements MetricRecord {

        @Override
        public String sourceKey() {
            return "M06.FACILITY_LICENSE:" + licenseId;
        }

        @Override
        public Instant referenceTime() {
            return null;
        }

        @Override
        public Long floorAreaId() {
            return null;
        }

        @Override
        public Long residentId() {
            return null;
        }
    }

    private static final Metric<FacilityLicenseRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.FACILITY_LICENSES,
            "Giấy phép cơ sở sắp hết hạn",
            "Giấy phép hoạt động của cơ sở hết hạn trong CFG-M06-02, kèm số ngày còn lại",
            "007 FacilityLicenseApi.expiringWithin",
            new Metric.Computation.Count<>(r -> true),
            null,
            r -> new DetailRow(
                    r.sourceKey(),
                    "007",
                    "FACILITY_LICENSE",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Map.of("name", r.name(), "expiryDate", r.expiryDate(), "daysRemaining", r.daysRemaining()),
                    null,
                    Map.of(),
                    false,
                    false,
                    false));

    private final ObjectProvider<FacilityLicenseApi> licenses;
    private final ParameterApi parameters;
    private final BusinessTime time;

    public FacilityLicenseMetric(
            ObjectProvider<FacilityLicenseApi> licenses, ParameterApi parameters, BusinessTime time) {
        this.licenses = licenses;
        this.parameters = parameters;
        this.time = time;
    }

    @Override
    public Metric<FacilityLicenseRecord> metric() {
        return METRIC;
    }

    @Override
    public Stream<FacilityLicenseRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        FacilityLicenseApi api = licenses.getIfAvailable();
        if (api == null) {
            return Stream.empty();
        }
        Period within = Period.ofDays(
                (int) parameters.read(ParameterCode.CFG_M06_02).asDuration().toDays());
        LocalDate today = time.today();
        return api.expiringWithin(within).stream()
                .map(l -> new FacilityLicenseRecord(
                        l.licenseId(), l.name(), l.expiryDate(), ChronoUnit.DAYS.between(today, l.expiryDate())));
    }
}
