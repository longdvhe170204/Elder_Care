package com.cambae.backend.m14.internal.report.domain;

import com.cambae.backend.m14.internal.metric.domain.MetricGroup;
import java.util.List;

/**
 * Nhóm báo cáo 18.1 → 18.6 ({@code nhom} của {@code /reports/{group}}; glossary 22.3) và các nhóm chỉ tiêu FR-010 nằm
 * trong báo cáo, theo dải FR của spec: người cao tuổi FR-022 → FR-025, chăm sóc FR-030 → FR-036, chất lượng FR-037, sức
 * khỏe và thuốc FR-040 → FR-044, chi phí FR-050 → FR-055a, nhân sự và ca FR-070 → FR-073. Nhóm chỉ có ở dashboard (giấy
 * phép, khoanh vùng, bàn giao, số dư, khai báo tạm trú, tài sản, kho, dấu nguy kịch) không thuộc báo cáo nào nên không
 * xuất được (FR-061: "Dashboard không có lệnh xuất").
 */
public enum ReportGroup {
    RESIDENTS("Người cao tuổi", List.of(MetricGroup.RESIDENTS, MetricGroup.WAITING_LIST, MetricGroup.BEDS)),
    CARE(
            "Chăm sóc",
            List.of(
                    MetricGroup.CARE_TASKS,
                    MetricGroup.PER_STAFF,
                    MetricGroup.BED_AWAITING_READY,
                    MetricGroup.MEAL_TICKETS,
                    MetricGroup.ACTIVITIES)),
    QUALITY("Chất lượng", List.of(MetricGroup.QUALITY_CHECKS)),
    HEALTH_MEDICATION(
            "Sức khỏe, thuốc",
            List.of(
                    MetricGroup.HEALTH_EVENTS,
                    MetricGroup.NON_HEALTH_INCIDENTS,
                    MetricGroup.OVERDUE_REASSESSMENTS,
                    MetricGroup.MEDICATION_DOSES,
                    MetricGroup.MEDICATION_RECONCILIATIONS)),
    INCURRED_COSTS("Chi phí phát sinh", List.of(MetricGroup.INCURRED_COSTS, MetricGroup.PURCHASING)),
    STAFFING_SHIFTS("Nhân sự, ca trực", List.of(MetricGroup.STAFFING_RATIO, MetricGroup.STAFFING_SHIFTS));

    private final String label;
    private final List<MetricGroup> metricGroups;

    ReportGroup(String label, List<MetricGroup> metricGroups) {
        this.label = label;
        this.metricGroups = metricGroups;
    }

    public String label() {
        return label;
    }

    public List<MetricGroup> metricGroups() {
        return metricGroups;
    }
}
