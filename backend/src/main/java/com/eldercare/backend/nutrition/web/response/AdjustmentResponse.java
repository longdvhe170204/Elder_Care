package com.eldercare.backend.nutrition.web.response;

import com.eldercare.backend.nutrition.internal.closing.service.PortionClosingService.AdjustmentRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Phát sinh cho bếp (api-011 {@code Adjustment}; FR-039, FR-041): không có người, dị ứng, thành phần (SC-008). */
@Schema(name = "Adjustment")
public record AdjustmentResponse(
        Long id,
        Long floorAreaId,
        Long dietId,
        Integer portionCount,
        String changeType,
        Boolean needsDelivery,
        Instant kitchenConfirmedAt) {

    public static AdjustmentResponse of(AdjustmentRow row) {
        return new AdjustmentResponse(
                row.id(),
                row.floorAreaId(),
                row.dietId(),
                row.portionCount(),
                row.changeType(),
                row.needsDelivery(),
                row.kitchenConfirmedAt());
    }
}
