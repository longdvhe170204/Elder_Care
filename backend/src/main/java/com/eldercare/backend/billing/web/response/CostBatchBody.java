package com.eldercare.backend.billing.web.response;

import com.eldercare.backend.billing.internal.item.domain.CostBatchResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "CostBatchResult")
public record CostBatchBody(List<Long> succeededIds, List<Skipped> skipped) {
    public static CostBatchBody of(CostBatchResult result) {
        return new CostBatchBody(
                result.succeededIds(),
                result.skipped().stream()
                        .map(s -> new Skipped(null, s.itemId(), s.errorCode(), s.reason()))
                        .toList());
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(name = "CostBatchSkipped")
    public record Skipped(String source, Long itemId, String errorCode, String reason) {}
}
