package com.eldercare.backend.billing.balance.web;

import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTransactionView;
import com.eldercare.backend.billing.balance.internal.ledger.service.BalanceQueries;
import com.eldercare.backend.billing.balance.internal.transaction.service.BalanceTransactionService;
import com.eldercare.backend.billing.balance.web.request.DepositOffsetRequest;
import com.eldercare.backend.billing.balance.web.request.NewBalanceTransactionRequest;
import com.eldercare.backend.billing.balance.web.response.PendingBalanceTransactionResponse;
import com.eldercare.backend.common.error.web.ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Giao dịch cần duyệt, cấn trừ cọc (017 T025, T026; api-017 tag {@code balance-transactions}; FR-010 → FR-015). Duyệt,
 * từ chối, hủy đi qua API yêu cầu phê duyệt của 000.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "balance-transactions")
class BalanceTransactionController {

    private static final String PROBLEM = "application/problem+json";

    private final BalanceTransactionService service;

    BalanceTransactionController(BalanceTransactionService service) {
        this.service = service;
    }

    @PostMapping("/balance-transactions")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Giao dịch Chờ duyệt"),
        @ApiResponse(
                responseCode = "403",
                description = "PERMISSION_DENIED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                description = "NO_LEDGER, BALANCE_EXCEEDED, DEPOSIT_BALANCE_EXCEEDED, REFUND_LIMIT_EXCEEDED,"
                        + " RECIPIENT_NO_LONGER_REPRESENTATIVE, STATEMENT_PAYMENT_NOT_ADJUSTABLE",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                description = "INVALID_VALUE",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<BalanceTransactionView> create(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody NewBalanceTransactionRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(BalanceQueries.target(body.residentId()), body.command(), body.reasonValue()));
    }

    @GetMapping("/balance-transactions/pending-approval")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    List<PendingBalanceTransactionResponse> pendingApproval() {
        return service.pendingApproval().stream()
                .map(PendingBalanceTransactionResponse::of)
                .toList();
    }

    @PostMapping("/residents/{residentId}/deposit-offsets")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Cặp giao dịch"),
        @ApiResponse(
                responseCode = "409",
                description = "NO_LEDGER, OFFSET_CONDITIONS_NOT_MET",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<List<BalanceTransactionView>> offset(
            @PathVariable long residentId,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody DepositOffsetRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.offset(BalanceQueries.target(residentId), residentId, body.amount(), body.note()));
    }
}
