package com.eldercare.backend.belongings.web.response;

import com.eldercare.backend.belongings.internal.belonging.domain.HandoverRow;
import io.swagger.v3.oas.annotations.media.Schema;

/** Schema {@code HandoverRef} của api-013 (bổ sung 2026-11-23): đồ, người cao tuổi của một bản ghi bàn giao. */
@Schema(
        name = "HandoverRef",
        requiredProperties = {"id", "belongingId", "residentId", "kind"})
public record HandoverRefResponse(long id, long belongingId, long residentId, String kind) {

    public static HandoverRefResponse of(HandoverRow row) {
        return new HandoverRefResponse(
                row.id(), row.belongingId(), row.residentId(), row.kind().name());
    }
}
