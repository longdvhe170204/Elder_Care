package com.eldercare.backend.billing.web.response;

import com.eldercare.backend.billing.api.dto.ProcurementStatus;
import com.eldercare.backend.billing.internal.procurement.domain.ProcurementLineId;
import com.eldercare.backend.billing.internal.procurement.domain.ProcurementRequestRow;
import com.eldercare.backend.billing.web.request.ProcurementRequestWrite;
import com.eldercare.backend.roombedallocation.api.dto.ProcurementType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * 019 T008, T027: phản hồi {@code ProcurementRequest} của api-019; id dòng theo {@link ProcurementLineId}.
 * {@code received} chỉ có ở loại MEDICATION (FR-008d), dịch vụ tính từ {@code ProcurementIntakePort} của 006.
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

    public static ProcurementRequestResponse of(ProcurementRequestRow row, Boolean received) {
        return new ProcurementRequestResponse(
                row.id(),
                row.departmentId(),
                row.type(),
                row.priority(),
                row.neededBy(),
                row.relatedFileIds(),
                IntStream.range(0, row.lines().size())
                        .mapToObj(i -> ProcurementRequestWrite.Line.of(
                                ProcurementLineId.of(row.id(), i), row.lines().get(i)))
                        .toList(),
                row.reason(),
                row.neededOn(),
                row.reference(),
                row.originalRequestId(),
                row.estimatedTotal(),
                row.approvedAmount(),
                row.actualAmount(),
                row.status(),
                row.linkedSourceTotal(),
                received);
    }
}
