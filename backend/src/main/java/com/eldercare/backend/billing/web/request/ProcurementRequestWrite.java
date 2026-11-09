package com.eldercare.backend.billing.web.request;

import com.eldercare.backend.billing.internal.procurement.domain.ProcurementContent;
import com.eldercare.backend.billing.internal.procurement.domain.ProcurementRequestRow;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.roombedallocation.api.dto.ProcurementType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 019 T008: thân {@code ProcurementRequestWrite} của api-019 (glossary 25.3). */
public record ProcurementRequestWrite(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long departmentId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ProcurementType type,
        @Schema(allowableValues = {"NORMAL", "URGENT"}) String priority,
        Instant neededBy,
        List<Long> relatedFileIds,

        @ArraySchema(minItems = 1) @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<Line> lines,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String reason,
        LocalDate neededOn,
        Map<String, Object> reference,
        Long originalRequestId) {

    @Schema(name = "ProcurementRequestLine")
    public record Line(
            @Schema(accessMode = Schema.AccessMode.READ_ONLY)
            Long id,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            String name,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal quantity,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Long estimatedUnitPrice,

            Long residentId,
            Long prescriptionId,
            String activeIngredient,
            String strength,
            String dosageForm,
            String unit) {

        public static Line of(Long id, ProcurementRequestRow.Line line) {
            return new Line(
                    id,
                    line.name(),
                    line.quantity(),
                    line.estimatedUnitPrice(),
                    line.residentId(),
                    line.prescriptionId(),
                    line.activeIngredient(),
                    line.strength(),
                    line.dosageForm(),
                    line.unit());
        }
    }

    /** Thành tiền, tổng dự kiến do dịch vụ tính lại. */
    public ProcurementContent content() {
        if (departmentId == null) throw new InvalidValueException("Cần bộ phận");
        return new ProcurementContent(
                departmentId,
                type,
                priority,
                neededBy,
                relatedFileIds,
                lines == null
                        ? null
                        : lines.stream()
                                .map(l -> l == null
                                        ? null
                                        : new ProcurementRequestRow.Line(
                                                l.name(),
                                                l.quantity(),
                                                l.estimatedUnitPrice() == null ? -1 : l.estimatedUnitPrice(),
                                                0,
                                                l.residentId(),
                                                l.prescriptionId(),
                                                l.activeIngredient(),
                                                l.strength(),
                                                l.dosageForm(),
                                                l.unit()))
                                .toList(),
                0,
                reason,
                neededOn,
                reference,
                originalRequestId);
    }
}
