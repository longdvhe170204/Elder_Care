package com.cambae.backend.m14;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m09.api.ShiftApi;
import com.cambae.backend.m09.api.StaffStatisticsApi;
import com.cambae.backend.m09.api.dto.AssignmentHistoryView;
import com.cambae.backend.m09.api.dto.ExpiringCredentialView;
import com.cambae.backend.m09.api.dto.StaffPublicView;
import com.cambae.backend.m14.internal.license.service.StaffLicenseMetric;
import com.cambae.backend.m14.internal.license.service.StaffLicenseMetric.StaffLicenseRecord;
import com.cambae.backend.m14.internal.metric.domain.DetailRow;
import com.cambae.backend.m14.internal.metric.domain.Metric;
import com.cambae.backend.m14.internal.metric.domain.MetricGroup;
import com.cambae.backend.m14.internal.metric.domain.MetricValue;
import com.cambae.backend.m14.internal.scope.domain.ReportScope;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import com.cambae.backend.m15.account.api.spi.StaffStatus;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import com.cambae.backend.m15.parameter.api.ParameterKind;
import com.cambae.backend.m15.parameter.api.ParameterValue;
import com.cambae.backend.support.fake.m09.FakeShifts;
import com.cambae.backend.support.fake.m09.FakeStaffStatistics;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * Chỉ tiêu {@code LICENSE.STAFF} (tên logic {@code GP.NHAN_VIEN}; 016 T023, FR-020, US1 #6): giấy phép hành nghề hết hạn
 * trong CFG-M06-02, chứng chỉ đào tạo hết hạn trong CFG-M09-03, kèm số ngày còn lại; Trưởng tầng chỉ thấy nhân viên có
 * phân công tại tầng được giao, đang hiệu lực hoặc đã công bố chưa kết thúc (Q-389).
 */
class MetricLicenseStaffTest {

    private static final Instant NOW = Instant.parse("2026-10-24T03:00:00Z");

    private final BusinessTime time = new BusinessTime(Clock.fixed(NOW, BusinessTime.ZONE));

    private final FakeStaffStatistics statistics = new FakeStaffStatistics();

    private final FakeShifts shifts = new FakeShifts();

    /** CFG-M06-02 60 ngày, CFG-M09-03 30 ngày để thấy hai mốc tách riêng. */
    private final ParameterApi parameters = new ParameterApi() {
        @Override
        public ParameterValue read(ParameterCode code) {
            int days = code == ParameterCode.CFG_M09_03 ? 30 : 60;
            return new ParameterValue(
                    code,
                    ParameterKind.DURATION,
                    JsonMapper.builder().build().readTree("{\"value\":" + days + ",\"unit\":\"DAYS\"}"));
        }

        @Override
        public ParameterValue readAt(ParameterCode code, Instant at) {
            return read(code);
        }
    };

    private final StaffLicenseMetric source = new StaffLicenseMetric(
            new StaticListableBeanFactory(Map.of("statistics", statistics)).getBeanProvider(StaffStatisticsApi.class),
            new StaticListableBeanFactory(Map.of("shifts", shifts)).getBeanProvider(ShiftApi.class),
            parameters,
            time);

    private ExpiringCredentialView credential(long id, long staffId, String group, int inDays) {
        return new ExpiringCredentialView(
                id, staffId, "TYPE-" + id, time.today().plusDays(inDays), group);
    }

    @Test
    void FR_020_licenseUsesCfgM0602_trainingUsesCfgM0903_withDaysRemaining() {
        statistics.expiring(List.of(
                credential(1, 11, "PROFESSIONAL_LICENSE", 45),
                credential(2, 12, "TRAINING", 45),
                credential(3, 12, "TRAINING", 20)));
        shifts.staff(new StaffPublicView(11, "[Nhân viên 1]", "Điều dưỡng", null, StaffStatus.WORKING));
        Metric<StaffLicenseRecord> metric = source.metric();

        List<StaffLicenseRecord> included =
                metric.included(source.current(ScopeFilter.FACILITY, null, null), ReportScope.FACILITY::covers);
        MetricValue value = metric.compute(included);
        List<DetailRow> rows = metric.details(included, null);

        assertThat(metric.code()).isEqualTo("LICENSE.STAFF");
        assertThat(metric.group()).isEqualTo(MetricGroup.STAFF_LICENSES);
        assertThat(value.value()).isEqualByComparingTo("2");
        assertThat(value.breakdown())
                .extracting(MetricValue.Breakdown::key, b -> b.value().intValue())
                .containsExactly(Tuple.tuple("PROFESSIONAL_LICENSE", 1), Tuple.tuple("TRAINING", 1));
        assertThat(rows).extracting(r -> r.result().get("daysRemaining")).containsExactly(45L, 20L);
        assertThat(rows.getFirst().result())
                .containsEntry("staffName", "[Nhân viên 1]")
                .containsEntry("credentialType", "TYPE-1");
        assertThat(rows).extracting(DetailRow::residentId).containsOnlyNulls();
    }

    @Test
    void Q_389_floorManager_onlyStaffWithAssignmentAtAssignedFloor() {
        statistics
                .expiring(List.of(
                        credential(4, 21, "PROFESSIONAL_LICENSE", 10), credential(5, 22, "PROFESSIONAL_LICENSE", 10)))
                .history(List.of(new AssignmentHistoryView(
                        7,
                        21,
                        70L,
                        "FLOOR",
                        101,
                        NOW.plus(Duration.ofDays(3)),
                        NOW.plus(Duration.ofDays(3).plusHours(8)),
                        "SHIFT_MEMBER")));
        ScopeFilter floor = new ScopeFilter(false, Set.of(101L), Set.of(), Set.of());
        ReportScope scope = ReportScope.of(List.of(ReportScope.Grant.always(floor)));
        Metric<StaffLicenseRecord> metric = source.metric();

        List<StaffLicenseRecord> included = metric.included(source.current(floor, null, null), scope::covers);

        assertThat(included).extracting(StaffLicenseRecord::staffId).containsExactly(21L);
    }
}
