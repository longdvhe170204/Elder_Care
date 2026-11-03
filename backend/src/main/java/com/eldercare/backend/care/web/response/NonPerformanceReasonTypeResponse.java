package com.eldercare.backend.care.web.response;

import com.eldercare.backend.care.internal.catalog.domain.NonPerformanceReasonType;
import com.eldercare.backend.common.data.CatalogStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/** Version chỉ đọc theo hợp đồng được chủ dự án duyệt ngày 31/10. */
@Schema(name = "NonPerformanceReasonType")
public record NonPerformanceReasonTypeResponse(
        long id,
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) int version,
        String name,
        boolean fixed,
        CatalogStatus status) {
    public static NonPerformanceReasonTypeResponse of(NonPerformanceReasonType row) {
        return new NonPerformanceReasonTypeResponse(
                row.getId(), row.getVersion(), row.getName(), row.fixed(), row.getStatus());
    }
}
