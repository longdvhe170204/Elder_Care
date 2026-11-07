package com.eldercare.backend.belongings.web.response;

import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverType;
import com.eldercare.backend.belongings.internal.belonging.service.BelongingHandoverService;
import java.util.List;

/**
 * Phản hồi {@code POST /belongings/handovers} ("Các bản ghi bàn giao đã tạo", hợp đồng không khai schema): bản ghi đã
 * tạo và các đồ sau lệnh (phiên bản mới).
 */
public record HandoverResponse(List<Record> handovers, List<BelongingResponse> belongings) {

    /** Một bản ghi bàn giao đã tạo. */
    public record Record(long id, long belongingId, HandoverType kind, BelongingStatus targetStatus) {}

    public static HandoverResponse of(BelongingHandoverService.Result result) {
        return new HandoverResponse(
                result.handovers().stream()
                        .map(h -> new Record(h.handoverId(), h.belongingId(), h.kind(), h.targetStatus()))
                        .toList(),
                result.belongings().stream().map(BelongingResponse::of).toList());
    }
}
