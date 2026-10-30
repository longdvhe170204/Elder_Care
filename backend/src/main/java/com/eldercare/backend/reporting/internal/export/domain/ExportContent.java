package com.eldercare.backend.reporting.internal.export.domain;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.MetricValue;
import com.eldercare.backend.reporting.internal.metric.domain.ValueLabel;
import com.eldercare.backend.reporting.internal.report.domain.MetricResult;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;

/**
 * Nội dung được xuất ({@code NoiDungDuocXuat}; FR-061, research O-08, Q-193, Q-197, Q-198):
 *
 * <ul>
 *   <li>có định danh người cao tuổi: chỉ Quản lý viện, bắt buộc mục đích; file có mọi dòng tổng hợp và danh sách chi tiết;
 *   <li>không định danh (vai trò khác, hoặc Quản lý viện không chọn): chỉ số liệu tổng hợp, bỏ mọi dòng mà nhóm chỉ có
 *       đúng một người cao tuổi, không có danh sách chi tiết;
 *   <li>số giờ làm theo từng nhân viên (FR-073): chỉ Quản lý viện.
 * </ul>
 */
public record ExportContent(boolean includeIdentity, List<SummaryRow> summary, List<DetailLine> details) {

    /** Một dòng tổng hợp; {@code breakdownKey} rỗng là dòng tổng của chỉ tiêu. */
    public record SummaryRow(
            String metricCode,
            String metricName,
            String breakdownKey,
            BigDecimal value,
            BigDecimal numerator,
            BigDecimal denominator,
            Set<ValueLabel> labels,
            int pendingReviewCount) {}

    public record DetailLine(String metricCode, DetailRow row) {}

    /** Chỉ tiêu chỉ Quản lý viện xuất được (FR-061, FR-073). */
    static final Set<String> MANAGER_ONLY = Set.of("SHIFT.WORKING_HOURS");

    public static ExportContent decide(
            Set<Role> roles, boolean includeIdentity, Optional<String> purpose, List<MetricResult> results) {
        boolean manager = roles.contains(Role.NURSING_HOME_MANAGER);
        if (includeIdentity && !manager) {
            throw new PermissionDeniedException(
                    "PERMISSION_DENIED",
                    "chỉ Quản lý viện xuất được danh sách có định danh người cao tuổi",
                    Map.of("failedLayer", "ROLE"));
        }
        if (includeIdentity && purpose.filter(p -> !p.isBlank()).isEmpty()) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "PURPOSE_REQUIRED",
                    "Xuất danh sách có định danh người cao tuổi phải nhập mục đích");
        }
        List<SummaryRow> summary = new ArrayList<>();
        List<DetailLine> details = new ArrayList<>();
        for (MetricResult r : results) {
            if (!manager && MANAGER_ONLY.contains(r.metricCode())) {
                continue;
            }
            MetricValue v = r.value();
            if (includeIdentity || !singlePerson(r.residents())) {
                summary.add(new SummaryRow(
                        r.metricCode(),
                        r.metricName(),
                        null,
                        v.value(),
                        v.numerator(),
                        v.denominator(),
                        v.labels(),
                        v.pendingReviewCount()));
            }
            for (MetricValue.Breakdown b : v.breakdown()) {
                if (includeIdentity || !singlePerson(r.residentsByKey().getOrDefault(b.key(), Set.of()))) {
                    summary.add(new SummaryRow(
                            r.metricCode(),
                            r.metricName(),
                            b.key(),
                            b.value(),
                            b.numerator(),
                            b.denominator(),
                            Set.of(),
                            0));
                }
            }
            if (includeIdentity) {
                r.rows().forEach(row -> details.add(new DetailLine(r.metricCode(), row)));
            }
        }
        return new ExportContent(includeIdentity, List.copyOf(summary), List.copyOf(details));
    }

    /** Số dòng dữ liệu của file (FR-062 "số dòng"). */
    public int rowCount() {
        return summary.size() + details.size();
    }

    private static boolean singlePerson(Set<Long> residents) {
        return residents.size() == 1;
    }
}
