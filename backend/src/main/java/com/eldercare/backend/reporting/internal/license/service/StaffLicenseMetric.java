package com.eldercare.backend.reporting.internal.license.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.Metric;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.staffing.api.ShiftApi;
import com.eldercare.backend.staffing.api.StaffStatisticsApi;
import com.eldercare.backend.staffing.api.dto.AssignmentHistoryView;
import com.eldercare.backend.staffing.api.dto.ExpiringCredentialView;
import com.eldercare.backend.staffing.api.dto.StaffPublicView;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ tiêu {@code LICENSE.STAFF} ({@code GP.NHAN_VIEN}; FR-020, US1 #6): giấy phép hành nghề hết hạn trong CFG-M06-02,
 * chứng chỉ đào tạo hết hạn trong CFG-M09-03, kèm số ngày còn lại; nguồn {@code StaffStatisticsApi.expiringCredentials}
 * (008 xét hiệu lực). Số liệu "lúc xem" của dashboard, không thuộc báo cáo nào. Trưởng tầng chỉ thấy nhân viên có phân
 * công tại tầng được giao, đang hiệu lực hoặc đã công bố chưa kết thúc trong cửa sổ cảnh báo (FR-010, Q-389).
 */
@Component
@Transactional(readOnly = true)
public class StaffLicenseMetric implements MetricSource<StaffLicenseMetric.StaffLicenseRecord> {

    public static final String CODE = "LICENSE.STAFF";

    /** Nhóm dùng mốc CFG-M06-02; nhóm còn lại ({@code TRAINING}) dùng CFG-M09-03. */
    static final String PROFESSIONAL_LICENSE = "PROFESSIONAL_LICENSE";

    /**
     * Một giấy phép, chứng chỉ sắp hết hạn. {@code floorAreaId} chỉ để lọc phạm vi bước 2: một tầng của phạm vi mà 008 đã
     * xác nhận nhân viên có phân công; rỗng với phạm vi toàn viện.
     */
    public record StaffLicenseRecord(
            ExpiringCredentialView view, String staffName, long daysRemaining, Long floorAreaId)
            implements MetricRecord {

        public long staffId() {
            return view.staffId();
        }

        @Override
        public String sourceKey() {
            return "M09.CREDENTIAL:" + view.credentialId();
        }

        @Override
        public Instant referenceTime() {
            return null;
        }

        @Override
        public Long residentId() {
            return null;
        }
    }

    private static final Metric<StaffLicenseRecord> METRIC = new Metric<>(
            CODE,
            MetricGroup.STAFF_LICENSES,
            "Giấy phép hành nghề, chứng chỉ đào tạo sắp hết hạn",
            "Giấy phép hành nghề hết hạn trong CFG-M06-02, chứng chỉ đào tạo hết hạn trong CFG-M09-03, kèm số ngày còn"
                    + " lại",
            "008 StaffStatisticsApi.expiringCredentials",
            new Metric.Computation.Count<>(r -> true),
            r -> r.view().credentialGroup(),
            r -> {
                Map<String, Object> result = new HashMap<>();
                result.put("staffName", r.staffName());
                result.put("credentialType", r.view().credentialType());
                result.put("expiryDate", r.view().expiryDate());
                result.put("daysRemaining", r.daysRemaining());
                return new DetailRow(
                        r.sourceKey(),
                        "008",
                        "CREDENTIAL",
                        r.view().credentialGroup(),
                        null,
                        null,
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

    private final ObjectProvider<StaffStatisticsApi> statistics;
    private final ObjectProvider<ShiftApi> shifts;
    private final ParameterApi parameters;
    private final BusinessTime time;

    public StaffLicenseMetric(
            ObjectProvider<StaffStatisticsApi> statistics,
            ObjectProvider<ShiftApi> shifts,
            ParameterApi parameters,
            BusinessTime time) {
        this.statistics = statistics;
        this.shifts = shifts;
        this.parameters = parameters;
        this.time = time;
    }

    @Override
    public Metric<StaffLicenseRecord> metric() {
        return METRIC;
    }

    /** Số liệu "lúc xem": bỏ qua khoảng. */
    @Override
    public Stream<StaffLicenseRecord> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters) {
        StaffStatisticsApi api = statistics.getIfAvailable();
        if (api == null) {
            return Stream.empty();
        }
        LocalDate today = time.today();
        LocalDate licenseUntil =
                today.plus(parameters.read(ParameterCode.CFG_M06_02).asPeriod());
        LocalDate trainingUntil =
                today.plus(parameters.read(ParameterCode.CFG_M09_03).asPeriod());
        LocalDate until = licenseUntil.isAfter(trainingUntil) ? licenseUntil : trainingUntil;
        Long floor = null;
        Set<Long> staff = null;
        if (!rough.facilityWide()) {
            if (rough.floorAreaIds().isEmpty()) {
                return Stream.empty();
            }
            floor = Collections.min(rough.floorAreaIds());
            staff = api.assignmentHistory(rough, time.now(), time.startOfDay(until.plusDays(1)))
                    .map(AssignmentHistoryView::staffId)
                    .collect(Collectors.toSet());
        }
        Long scopeFloor = floor;
        Set<Long> scopeStaff = staff;
        ShiftApi names = shifts.getIfAvailable();
        return api.expiringCredentials((int) ChronoUnit.DAYS.between(today, until))
                .filter(c -> !c.expiryDate()
                        .isAfter(PROFESSIONAL_LICENSE.equals(c.credentialGroup()) ? licenseUntil : trainingUntil))
                .filter(c -> scopeStaff == null || scopeStaff.contains(c.staffId()))
                .map(c -> new StaffLicenseRecord(
                        c,
                        names == null
                                ? null
                                : names.staff(c.staffId())
                                        .map(StaffPublicView::fullName)
                                        .orElse(null),
                        ChronoUnit.DAYS.between(today, c.expiryDate()),
                        scopeFloor));
    }
}
