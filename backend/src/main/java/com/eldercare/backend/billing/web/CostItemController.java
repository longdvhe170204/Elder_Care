package com.eldercare.backend.billing.web;

import com.eldercare.backend.billing.internal.item.service.*;
import com.eldercare.backend.billing.web.request.CostWorkflowRequests.*;
import com.eldercare.backend.billing.web.response.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/costs/items")
class CostItemController {
    private final CostItemService commands;
    private final CostQueries queries;

    CostItemController(CostItemService commands, CostQueries queries) {
        this.commands = commands;
        this.queries = queries;
    }

    @GetMapping("/{id}")
    @ApiResponse(
            responseCode = "200",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CostItemBody.class)))
    CostItemBody item(@PathVariable long id) {
        return CostItemBody.of(queries.item(id));
    }

    @PostMapping("/check")
    @ApiResponse(
            responseCode = "200",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CostBatchBody.class)))
    CostBatchBody check(@RequestHeader("Idempotency-Key") UUID key, @Valid @RequestBody Selection body) {
        return CostBatchBody.of(commands.check(body.domain()));
    }

    @PostMapping("/approve")
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
    CostBatchBody approve(@RequestHeader("Idempotency-Key") UUID key, @Valid @RequestBody Approval body) {
        return CostBatchBody.of(commands.approve(body.domain(), body.excessApprovalReason()));
    }

    @PostMapping("/{id}/uncheck")
    @ApiResponses({
        @ApiResponse(responseCode = "200", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = CostProblemBody.class)))
    })
    ResponseEntity<Void> uncheck(
            @PathVariable long id, @RequestHeader("Idempotency-Key") UUID key, @Valid @RequestBody ReasonBody body) {
        commands.uncheck(id, body.reason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/return")
    @ApiResponses({
        @ApiResponse(responseCode = "200", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = CostProblemBody.class)))
    })
    ResponseEntity<Void> returnItem(
            @PathVariable long id, @RequestHeader("Idempotency-Key") UUID key, @Valid @RequestBody ReasonBody body) {
        commands.returnItem(id, body.reason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/cancel")
    @ApiResponses({
        @ApiResponse(responseCode = "200", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = CostProblemBody.class)))
    })
    ResponseEntity<Void> cancel(
            @PathVariable long id, @RequestHeader("Idempotency-Key") UUID key, @Valid @RequestBody ReasonBody body) {
        commands.cancel(id, body.reason());
        return ResponseEntity.ok().build();
    }
}
