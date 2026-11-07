package com.eldercare.backend.billing.web.response;

import com.eldercare.backend.billing.api.dto.ProcurementStatus;
import com.eldercare.backend.billing.internal.procurement.domain.ProcurementRequestRow;
import com.eldercare.backend.billing.web.request.ProcurementRequestWrite;
import com.eldercare.backend.roombedallocation.api.dto.ProcurementType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 019 T008: phản hồi {@code ProcurementRequest} của api-019. {@code received} chỉ có ở loại MEDICATION, đọc từ 006
 * (FR-008d); cài ở lượt sau của Phase 3, hiện luôn rỗng.
 */
@Schema(name = "ProcurementRequest")
public record ProcurementRequestResponse(
        Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long departmentId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ProcurementType type,
        @Schema(allowableValues = {"NORMAL", "URGENT"}) String priority,
        Instant neededBy,
        List<Long> relatedFileIds,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<ProcurementRequestWrite.Line> lines,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String reason,
        LocalDate neededOn,
        Map<String, Object> reference,
        Long originalRequestId,
        Long estimatedTotal,
        Long approvedAmount,
        Long actualAmount,
        ProcurementStatus status,
        Long linkedSourceTotal,
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) Boolean received) {

    public static ProcurementRequestResponse of(ProcurementRequestRow row) {
        return new ProcurementRequestResponse(
                row.id(),
                row.departmentId(),
                row.type(),
                row.priority(),
                row.neededBy(),
                row.relatedFileIds(),
                row.lines().stream().map(ProcurementRequestWrite.Line::of).toList(),
                row.reason(),
                row.neededOn(),
                row.reference(),
                row.originalRequestId(),
                row.estimatedTotal(),
                row.approvedAmount(),
                row.actualAmount(),
                row.status(),
                row.linkedSourceTotal(),
                null);
    }
}
