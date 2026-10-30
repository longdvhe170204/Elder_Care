package com.eldercare.backend.care.web.response;

import com.eldercare.backend.care.internal.catalog.domain.NonPerformanceReasonType;
import com.eldercare.backend.common.data.CatalogStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/** Chỉ trả bốn trường NonPerformanceReasonType của hợp đồng đã duyệt. */
@Schema(name = "NonPerformanceReasonType")
public record NonPerformanceReasonTypeResponse(long id, String name, boolean fixed, CatalogStatus status) {
    public static NonPerformanceReasonTypeResponse of(NonPerformanceReasonType row) {
        return new NonPerformanceReasonTypeResponse(row.getId(), row.getName(), row.fixed(), row.getStatus());
    }
}
