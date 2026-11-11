package com.eldercare.backend.reporting.internal.metric.service;

import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogRecord;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loại dữ liệu đã loại bỏ theo NFR-07 giao với khoảng báo cáo (016 FR-007, T109; chủ dự án duyệt
 * {@code MetricValue.purgedDataTypes} 2026-11-10). Nguồn là bản tóm tắt {@code EXPIRED_DATA_PURGED} của 000 FR-051:
 * mỗi loại dữ liệu kèm số bản ghi và khoảng mốc lưu giữ {@code from}, {@code to} của các bản ghi đã xóa. Bản ghi đã xóa có
 * thời điểm không sau mốc lưu giữ của nó, nên khoảng báo cáo bắt đầu trước {@code to} là có thể thiếu số liệu.
 *
 * <p>Chỉ tiêu ứng với module qua tên API nguồn ở {@code Metric.source} (ví dụ "006 MedicationStatisticsApi" đọc M07); loại
 * dữ liệu mang tiền tố mã module (research R-02 của 000).
 */
@Component
@Transactional(readOnly = true)
public class PurgedDataTypes {

    static final String RUN_ENTITY = "M15.PURGE_RUN";

    private static final Map<String, String> MODULE_BY_API = Map.ofEntries(
            Map.entry("ResidentStatisticsApi", "M01"),
            Map.entry("ResidencyStatisticsApi", "M02"),
            Map.entry("BedStatisticsApi", "M03"),
            Map.entry("BedAllocationApi", "M03"),
            Map.entry("AssetStatisticsApi", "M03"),
            Map.entry("TaskStatisticsApi", "M04"),
            Map.entry("ActivityStatisticsApi", "M04"),
            Map.entry("IncidentStatisticsApi", "M05"),
            Map.entry("VitalSignStatisticsApi", "M06"),
            Map.entry("FacilityLicenseApi", "M06"),
            Map.entry("MedicationStatisticsApi", "M07"),
            Map.entry("NutritionStatisticsApi", "M08"),
            Map.entry("StaffStatisticsApi", "M09"),
            Map.entry("SwapStatisticsApi", "M09"),
            Map.entry("CostStatisticsApi", "M11"));

    private final AuditLogApi audit;

    public PurgedDataTypes(AuditLogApi audit) {
        this.audit = audit;
    }

    /** Loại dữ liệu (sắp theo tên) mà chỉ tiêu có {@code source} đọc và có thể thiếu khi khoảng bắt đầu ở {@code from}. */
    public List<String> affecting(String source, Instant from) {
        String module = module(source);
        if (module == null) {
            return List.of();
        }
        Set<String> types = new TreeSet<>();
        for (AuditLogRecord run : audit.recorded(AuditAction.EXPIRED_DATA_PURGED, RUN_ENTITY, null, null)) {
            if (!(run.details().get("deleted") instanceof Map<?, ?> deleted)) {
                continue;
            }
            deleted.forEach((type, range) -> {
                if (type.toString().startsWith(module + ".")
                        && range instanceof Map<?, ?> r
                        && r.get("count") instanceof Number count
                        && count.longValue() > 0
                        && r.get("to") != null
                        && from.isBefore(Instant.parse(r.get("to").toString()))) {
                    types.add(type.toString());
                }
            });
        }
        return List.copyOf(types);
    }

    private static String module(String source) {
        if (source == null) {
            return null;
        }
        for (Map.Entry<String, String> e : MODULE_BY_API.entrySet()) {
            if (source.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }
}
