package com.eldercare.backend.billing.web.response;

import com.eldercare.backend.billing.api.dto.CostTypeTotal;
import com.eldercare.backend.billing.api.dto.ProvisionalCostSummaryView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "ProvisionalCostSummary")
public record ProvisionalCostSummaryBody(String label, List<CostTypeTotal> totalsByType, boolean hasUnpricedItems) {
    public static ProvisionalCostSummaryBody of(ProvisionalCostSummaryView value) {
        return new ProvisionalCostSummaryBody(value.label(), value.totalsByType(), value.hasUnpricedItems());
    }
}
