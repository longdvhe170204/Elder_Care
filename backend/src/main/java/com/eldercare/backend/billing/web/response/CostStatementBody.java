package com.eldercare.backend.billing.web.response;

import com.eldercare.backend.billing.api.dto.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "CostStatement")
public record CostStatementBody(
        long id,
        String period,
        long residentId,
        CostStatementType statementType,
        boolean finalPeriod,
        LocalDate throughDate,

        @Schema(allowableValues = {"OPEN", "AWAITING_FINALIZATION", "FINALIZED"})
        String status,

        List<CostTypeTotal> totalsByType,
        Long total,
        List<CostItemBody> items) {
    public static CostStatementBody of(CostStatementView v) {
        return new CostStatementBody(
                v.id(),
                v.period(),
                v.residentId(),
                v.statementType(),
                v.finalPeriod(),
                v.throughDate(),
                v.status(),
                v.totalsByType(),
                v.total(),
                v.items() == null
                        ? null
                        : v.items().stream().map(CostItemBody::of).toList());
    }
}
