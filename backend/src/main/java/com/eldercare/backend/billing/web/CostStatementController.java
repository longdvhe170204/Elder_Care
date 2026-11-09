package com.eldercare.backend.billing.web;

import com.eldercare.backend.billing.internal.item.service.CostQueries;
import com.eldercare.backend.billing.internal.period.service.CostStatementService;
import com.eldercare.backend.billing.web.request.CostWorkflowRequests.ReturnStatement;
import com.eldercare.backend.billing.web.response.CostProblemBody;
import com.eldercare.backend.billing.web.response.CostStatementBody;
import com.eldercare.backend.billing.web.response.CostSubmissionBody;
import com.eldercare.backend.common.web.VersionEtag;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/costs/statements")
class CostStatementController {
    private final CostQueries queries;
    private final CostStatementService commands;

    CostStatementController(CostQueries queries, CostStatementService commands) {
        this.queries = queries;
        this.commands = commands;
    }

    @GetMapping
    @ApiResponse(
            responseCode = "200",
            content =
                    @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = CostStatementBody.class))))
    List<CostStatementBody> list(
            @RequestParam(required = false) Long periodId,
            @RequestParam(required = false) Long residentId,
            @RequestParam(required = false) @Schema(allowableValues = {"OPEN", "AWAITING_FINALIZATION", "FINALIZED"})
                    String status) {
        return queries.list(periodId, residentId, status).stream()
                .map(CostStatementBody::of)
                .toList();
    }

    @GetMapping("/{id}")
    @ApiResponse(
            responseCode = "200",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CostStatementBody.class)))
    ResponseEntity<CostStatementBody> statement(@PathVariable long id) {
        var value = queries.statement(id);
        return ResponseEntity.ok().eTag(VersionEtag.of(value.version())).body(CostStatementBody.of(value.value()));
    }

    @PostMapping("/{id}/submit-for-finalization")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = CostSubmissionBody.class))),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = CostProblemBody.class)))
    })
    CostSubmissionBody submit(@PathVariable long id, @RequestHeader("Idempotency-Key") UUID key) {
        var warning = commands.submit(id);
        return CostSubmissionBody.of(warning.orElse(null));
    }

    @PostMapping("/{id}/return")
    @ApiResponse(responseCode = "200", content = @Content)
    ResponseEntity<Void> returnStatement(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody ReturnStatement body) {
        commands.returnStatement(id, body.itemIds(), body.reason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/finalize")
    @ApiResponses({
        @ApiResponse(responseCode = "200", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = CostProblemBody.class))),
        @ApiResponse(
                responseCode = "412",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = CostProblemBody.class)))
    })
    ResponseEntity<Void> finalizeStatement(
            @PathVariable long id,
            @RequestHeader("If-Match") String ifMatch,
            @RequestHeader("Idempotency-Key") UUID key) {
        commands.finalizeStatement(id, ifMatch);
        return ResponseEntity.ok().build();
    }
}
