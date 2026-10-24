package com.cambae.backend.m14.web.request;

import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.m14.internal.export.service.ReportExportService.ExportFormat;
import com.cambae.backend.m14.internal.metric.domain.ReportFilters;
import com.cambae.backend.m14.internal.metric.domain.ReportFilters.CostMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Thân {@code POST /reports/{group}/export} ({@code YeuCauXuat}; FR-060 → FR-062). {@code filters} dùng tên tham số của
 * {@code GET /reports/{group}}: {@code floorAreaId} (một hoặc nhiều), {@code residenceType}, {@code careLevelId},
 * {@code residentId}, {@code staffId}, {@code shiftId}.
 */
public record ExportRequestBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate from,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate to,

        Map<String, Object> filters,
        CostMode costMode,

        @Schema(description = "Chỉ Quản lý viện") Boolean includeIdentity,

        @Schema(description = "Bắt buộc khi includeIdentity")
        String purpose,

        ExportFormat format) {

    private static final Set<String> KEYS =
            Set.of("floorAreaId", "residenceType", "careLevelId", "residentId", "staffId", "shiftId");

    public ReportFilters toFilters() {
        Map<String, Object> f = filters == null ? Map.of() : filters;
        for (String key : f.keySet()) {
            if (!KEYS.contains(key)) {
                throw new InvalidValueException("Bộ lọc không hợp lệ: " + key);
            }
        }
        return new ReportFilters(
                floors(f.get("floorAreaId")),
                f.get("residenceType") == null ? null : f.get("residenceType").toString(),
                id(f.get("careLevelId")),
                id(f.get("residentId")),
                id(f.get("staffId")),
                id(f.get("shiftId")),
                costMode);
    }

    private static Set<Long> floors(Object value) {
        Set<Long> result = new HashSet<>();
        if (value instanceof Collection<?> values) {
            values.forEach(v -> result.add(id(v)));
        } else if (value != null) {
            result.add(id(value));
        }
        return result;
    }

    private static Long id(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            throw new InvalidValueException("Bộ lọc không hợp lệ: " + value);
        }
    }
}
