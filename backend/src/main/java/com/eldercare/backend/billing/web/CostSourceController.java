package com.eldercare.backend.billing.web;

import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.billing.internal.governance.service.CostAccess;
import com.eldercare.backend.billing.internal.source.domain.*;
import com.eldercare.backend.billing.internal.source.service.*;
import com.eldercare.backend.billing.web.request.CreateCostsFromEventsRequest;
import com.eldercare.backend.billing.web.response.CostBatchBody;
import com.eldercare.backend.billing.web.response.CostItemBody;
import com.eldercare.backend.billing.web.response.CostProblemBody;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.InvalidValueException;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/costs")
class CostSourceController {
    private final CostSourceLookup lookup;
    private final SourceCostCommands commands;
    private final CostAccess access;

    CostSourceController(CostSourceLookup lookup, SourceCostCommands commands, CostAccess access) {
        this.lookup = lookup;
        this.commands = commands;
        this.access = access;
    }

    @GetMapping("/billing-sources")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                content =
                        @Content(
                                mediaType = "application/json",
                                array = @ArraySchema(schema = @Schema(implementation = CostItemBody.Source.class)))),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = CostProblemBody.class)))
    })
    List<CostItemBody.Source> sources(
            @RequestParam long residentId,
            @RequestParam
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
                    String type,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        access.require(
                Role.OFFICE_STAFF, Function.COST_ADJUSTMENT, Operation.VIEW, "M01.RESIDENT", residentId, residentId);
        return lookup.find(residentId, type, from, to).stream()
                .map(CostSourceCandidate::view)
                .map(CostItemBody.Source::of)
                .toList();
    }

    @PostMapping("/items/from-events")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = CostBatchBody.class))),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = CostProblemBody.class)))
    })
    CostBatchBody create(
            @RequestHeader("Idempotency-Key") UUID key, @Valid @RequestBody CreateCostsFromEventsRequest body) {
        if (body.residentId() == null) throw new InvalidValueException("Cần chọn người cao tuổi");
        return CostBatchBody.of(commands.create(new SourceCostSelection(
                body.residentId(),
                body.sourceType(),
                body.sourceIds(),
                body.sourceUnits() == null
                        ? null
                        : body.sourceUnits().stream()
                                .map(s -> s == null ? null : new BillingSourceKey(s.sourceId(), s.billingUnit()))
                                .toList(),
                body.dateRange() == null ? null : body.dateRange().from(),
                body.dateRange() == null ? null : body.dateRange().to())));
    }
}
