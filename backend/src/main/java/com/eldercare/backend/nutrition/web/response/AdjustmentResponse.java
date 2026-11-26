package com.eldercare.backend.nutrition.web.response;

import com.eldercare.backend.nutrition.internal.closing.domain.PortionContent;
import com.eldercare.backend.nutrition.internal.closing.service.PortionClosingService.AdjustmentRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Phát sinh cho bếp (api-011 {@code Adjustment}; FR-039, FR-041, Q-445): không có người, dị ứng (SC-008). */
@Schema(name = "Adjustment")
public record AdjustmentResponse(
        Long id,
        @Schema(allowableValues = {"FLOOR", "STAFF"}) String group,
        Long floorAreaId,
        Integer portionCount,
        String changeType,
        Boolean needsDelivery,
        Instant kitchenConfirmedAt) {

    public static AdjustmentResponse of(AdjustmentRow row) {
        boolean staff = row.floorAreaId() == PortionContent.STAFF_GROUP;
        return new AdjustmentResponse(
                row.id(),
                staff ? "STAFF" : "FLOOR",
                staff ? null : row.floorAreaId(),
                row.portionCount(),
                row.changeType(),
                row.needsDelivery(),
                row.kitchenConfirmedAt());
    }
}
