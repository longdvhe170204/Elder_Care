package com.eldercare.backend.billing.web.response;

import com.eldercare.backend.billing.api.dto.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/** T019: không xuất null cho trường tùy chọn của hợp đồng. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "CostItem")
public record CostItemBody(
        long id,
        CostType costType,

        @Schema(allowableValues = {"EVENT_BASED", "MANUAL", "PURCHASE_ASSISTANCE", "ADJUSTMENT"})
        String origin,

        Source billingSource,
        String description,
        String itemCode,
        String medicationName,
        LocalDate incurredOn,
        BigDecimal quantity,
        String unit,
        Long unitPrice,
        Map<String, Object> unitPriceBasis,
        BigDecimal factor,
        Long amount,
        List<String> flags,

        @Schema(allowableValues = {"DRAFT", "CHECKED", "APPROVED", "FINALIZED", "CANCELLED"})
        String status,

        Long originalItemId,
        String originalPeriod,
        List<Map<String, Object>> recalculationHistory,
        List<Command> commandHistory,
        List<Map<String, Object>> adjustments,
        Long currentTotal,
        List<String> hiddenFields) {
    public static CostItemBody of(CostItemView v) {
        Source source = null;
        if (v.billingSource() != null) {
            var s = v.billingSource();
            source = new Source(
                    s.sourceType(),
                    s.sourceId(),
                    s.unit(),
                    s.incurredOn(),
                    s.occurredAt(),
                    s.description(),
                    s.quantity(),
                    s.factor(),
                    s.flags(),
                    s.existingItemId(),
                    s.billingUnit());
        }
        return new CostItemBody(
                v.id(),
                v.costType(),
                v.origin(),
                source,
                v.description(),
                v.itemCode(),
                v.medicationName(),
                v.incurredOn(),
                v.quantity(),
                v.unit(),
                v.unitPrice(),
                v.unitPriceBasis(),
                v.factor(),
                v.amount(),
                v.flags(),
                v.status(),
                v.originalItemId(),
                v.originalPeriod(),
                v.recalculationHistory(),
                v.commandHistory().stream()
                        .map(h -> new Command(
                                (String) h.get("operation"),
                                ((Number) h.get("actorId")).longValue(),
                                (Instant) h.get("at"),
                                (String) h.get("reason")))
                        .toList(),
                v.adjustments(),
                v.currentTotal(),
                v.hiddenFields());
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(name = "CostCommandHistory")
    public record Command(
            @Schema(allowableValues = {"CHECK", "UNCHECK", "APPROVE", "RETURN", "CANCEL", "FINALIZE"})
            String operation,

            Long actorId,
            Instant at,
            String reason) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(name = "BillingSource")
    public record Source(
            @Schema(
                    allowableValues = {
                        "RESIDENCY_DAY",
                        "DAY_CARE_SESSION",
                        "CARE_TASK_RESULT",
                        "DOSE",
                        "PRN_ADMINISTRATION",
                        "DISPENSE_RECORD",
                        "FAMILY_MEAL",
                        "OVERNIGHT_STAY",
                        "ACTIVITY_ATTENDANCE"
                    })
            String sourceType,

            String sourceId,
            String unit,
            LocalDate incurredOn,
            Instant occurredAt,
            String description,
            BigDecimal quantity,
            BigDecimal factor,
            List<String> flags,
            Long existingItemId,
            String billingUnit) {}
}
