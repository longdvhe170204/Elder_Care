package com.eldercare.backend.billing.balance.web;

import com.eldercare.backend.billing.api.dto.BalanceStatus;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceDetails;
import com.eldercare.backend.billing.balance.internal.ledger.service.BalanceQueries;
import com.eldercare.backend.billing.balance.internal.status.service.BalanceStatusCommands;
import com.eldercare.backend.billing.balance.web.request.StopRemindersRequest;
import com.eldercare.backend.common.error.web.ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Tình trạng số dư, dừng nhắc (017 T020; api-017 tag {@code balance-status}; FR-016 → FR-020). */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "balance-status")
class BalanceStatusController {

    private static final String PROBLEM = "application/problem+json";

    private final BalanceStatusCommands commands;

    BalanceStatusController(BalanceStatusCommands commands) {
        this.commands = commands;
    }

    @GetMapping("/balance-statuses")
    List<BalanceDetails> statuses(
            @RequestParam(required = false) @Schema(allowableValues = {"RUNNING_LOW", "IN_DEBT"}) String status) {
        return commands.statuses(status == null ? null : BalanceStatus.valueOf(status));
    }

    @PostMapping("/residents/{residentId}/balance/stop-reminders")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "403",
                description = "PERMISSION_DENIED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                description = "NOT_FINAL_STATUS, STATE_CHANGED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<Void> stopReminders(
            @PathVariable long residentId,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @RequestBody StopRemindersRequest body) {
        commands.stopReminders(BalanceQueries.target(residentId), residentId, body.reason());
        return ResponseEntity.ok().build();
    }
}
