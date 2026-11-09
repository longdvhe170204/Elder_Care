package com.eldercare.backend.belongings.web.response;

import com.eldercare.backend.admission.api.dto.PostDeathItemStatus;
import com.eldercare.backend.belongings.internal.ending.service.BelongingEndingService;
import java.util.List;

/**
 * {@code GET /residents/{residentId}/belongings/ending-condition} (api-013 không khai schema): điều kiện (b) kết thúc
 * lưu trú kèm đồ chặn, đồ Thất lạc có sự cố mở ("chặn qua điều kiện (d)") và mục "xử lý đồ gửi" sau qua đời.
 */
public record EndingConditionResponse(
        boolean met,
        List<BelongingResponse> blocking,
        List<BelongingResponse> lostWithOpenIncident,
        PostDeathItem postDeathItem) {

    /** Mục "xử lý đồ gửi" của danh sách việc sau qua đời (FR-026). */
    public record PostDeathItem(PostDeathItemStatus status, String basis) {}

    public static EndingConditionResponse of(BelongingEndingService.Ending ending) {
        return new EndingConditionResponse(
                ending.condition().met(),
                ending.blocking().stream().map(BelongingResponse::of).toList(),
                ending.lostWithOpenIncident().stream()
                        .map(BelongingResponse::of)
                        .toList(),
                new PostDeathItem(
                        ending.postDeathItem().status(), ending.postDeathItem().basis()));
    }
}
