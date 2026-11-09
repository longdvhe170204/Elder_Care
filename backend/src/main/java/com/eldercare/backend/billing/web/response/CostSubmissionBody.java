package com.eldercare.backend.billing.web.response;

import com.eldercare.backend.billing.internal.period.domain.PeriodCostChange;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CostSubmissionBody(ChangeWarning changeWarning) {
    public static CostSubmissionBody of(PeriodCostChange.Warning warning) {
        return new CostSubmissionBody(
                warning == null ? null : new ChangeWarning(warning.increaseRatio(), warning.topThreeIncreases()));
    }

    @Schema(name = "CostChangeWarning")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ChangeWarning(BigDecimal increaseRatio, List<String> topThreeIncreases) {}
}
