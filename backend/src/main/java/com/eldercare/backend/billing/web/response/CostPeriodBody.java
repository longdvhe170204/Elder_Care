package com.eldercare.backend.billing.web.response;

import com.eldercare.backend.billing.api.dto.CostPeriodView;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.*;

/** Hình dạng đã công bố qua CostPeriodView; không thêm dữ liệu nghiệp vụ. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "CostPeriod")
public record CostPeriodBody(
        long id,
        String month,
        LocalDate fromDate,
        LocalDate throughDate,

        @Schema(allowableValues = {"OPEN", "AWAITING_FINALIZATION", "FINALIZED"})
        String status,

        LocalDate finalizationDueOn,
        Instant finalizedAt) {
    public static CostPeriodBody of(CostPeriodView value) {
        return new CostPeriodBody(
                value.id(),
                value.month(),
                value.fromDate(),
                value.throughDate(),
                value.status(),
                value.finalizationDueOn(),
                value.finalizedAt());
    }
}
