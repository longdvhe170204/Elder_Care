package com.eldercare.backend.billing.web.request;

import com.eldercare.backend.billing.api.dto.CostType;
import com.eldercare.backend.billing.internal.item.domain.CostItemSelection;
import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public final class CostWorkflowRequests {
    private CostWorkflowRequests() {}

    @Schema(name = "CostItemFilter")
    public record Filter(Long statementId, CostType costType, String flags, String origin) {
        CostItemSelection.Filter domain() {
            return new CostItemSelection.Filter(statementId, costType, flags, origin);
        }
    }

    @Schema(name = "CostItemSelection")
    public record Selection(
            List<@NotNull @Positive Long> itemIds, @Valid Filter filter) {
        public CostItemSelection domain() {
            return new CostItemSelection(itemIds, filter == null ? null : filter.domain());
        }
    }

    @Schema(name = "CostItemApproval")
    public record Approval(
            List<@NotNull @Positive Long> itemIds, @Valid Filter filter, String excessApprovalReason) {
        public CostItemSelection domain() {
            return new CostItemSelection(itemIds, filter == null ? null : filter.domain());
        }
    }

    @Schema(name = "CostReason")
    public record ReasonBody(
            @NotNull @Schema(implementation = String.class, minLength = 1, maxLength = 2000)
            Reason reason) {}

    @Schema(name = "ReturnCostStatement")
    public record ReturnStatement(
            @NotNull @Schema(implementation = String.class, minLength = 1, maxLength = 2000)
            Reason reason,

            @NotNull List<@NotNull @Positive Long> itemIds) {}
}
