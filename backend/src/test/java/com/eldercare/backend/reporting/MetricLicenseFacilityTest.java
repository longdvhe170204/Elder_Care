package com.eldercare.backend.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.administration.account.api.spi.ExpiringFacilityLicense;
import com.eldercare.backend.administration.account.api.spi.FacilityLicenseApi;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.ParameterCode;
import com.eldercare.backend.administration.parameter.api.ParameterKind;
import com.eldercare.backend.administration.parameter.api.ParameterValue;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.internal.license.service.FacilityLicenseMetric;
import com.eldercare.backend.reporting.internal.license.service.FacilityLicenseMetric.FacilityLicenseRecord;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricValue;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.reporting.internal.scope.domain.ReportScope;
import com.eldercare.backend.support.fake.administration.FakeFacilityLicenses;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * Chỉ tiêu {@code LICENSE.FACILITY} (tên logic {@code GP.CO_SO}; FR-020, US1 #6, contracts/chi-tieu.md): giấy phép cơ sở hết
 * hạn trong CFG-M06-02 theo {@code FacilityLicenseApi.expiringWithin}, kèm số ngày còn lại; nhóm FR-010 Giấy phép cơ sở
 * (chỉ Quản lý viện); số liệu "lúc xem", không có định danh người cao tuổi.
 */
class MetricLicenseFacilityTest {

    private final FakeFacilityLicenses licenses = new FakeFacilityLicenses();

    private final ParameterApi parameters = new ParameterApi() {
        @Override
        public ParameterValue read(ParameterCode code) {
            return new ParameterValue(
                    code,
                    ParameterKind.DURATION,
                    JsonMapper.builder().build().readTree("{\"value\":60,\"unit\":\"DAYS\"}"));
        }

        @Override
        public ParameterValue readAt(ParameterCode code, Instant at) {
            return read(code);
        }
    };

    private final FacilityLicenseMetric source = new FacilityLicenseMetric(
            new StaticListableBeanFactory(Map.of("licenses", licenses)).getBeanProvider(FacilityLicenseApi.class),
            parameters,
            new BusinessTime(Clock.fixed(Instant.parse("2026-10-24T03:00:00Z"), BusinessTime.ZONE)));

    @Test
    void US1_6_countsExpiringFacilityLicenses_withDaysRemaining() {
        licenses.expiring(new ExpiringFacilityLicense(11, "Giấy phép hoạt động", LocalDate.of(2026, 12, 8)))
                .expiring(new ExpiringFacilityLicense(12, "Giấy phép phòng khám", LocalDate.of(2026, 11, 3)));
        Metric<FacilityLicenseRecord> metric = source.metric();

        List<FacilityLicenseRecord> included = metric.included(
                source.records(ScopeFilter.FACILITY, null, null, ReportFilters.NONE), ReportScope.FACILITY::covers);
        MetricValue value = metric.compute(included);
        List<DetailRow> rows = metric.details(included, null);

        assertThat(metric.code()).isEqualTo("LICENSE.FACILITY");
        assertThat(metric.group()).isEqualTo(MetricGroup.FACILITY_LICENSES);
        assertThat(value.value()).isEqualByComparingTo("2");
        assertThat(rows).extracting(r -> r.result().get("daysRemaining")).containsExactly(45L, 10L);
        assertThat(rows).extracting(DetailRow::residentId).containsOnlyNulls();
        assertThat(rows.getFirst().result()).containsEntry("name", "Giấy phép hoạt động");
    }

    @Test
    void noExpiringLicense_isZero() {
        Metric<FacilityLicenseRecord> metric = source.metric();

        MetricValue value = metric.compute(
                metric.included(source.records(ScopeFilter.FACILITY, null, null, ReportFilters.NONE), r -> true));

        assertThat(value.value()).isEqualByComparingTo("0");
    }
}
