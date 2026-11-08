package com.eldercare.backend.billing.balance.web;

import com.eldercare.backend.billing.balance.internal.ledger.domain.Payments;
import com.eldercare.backend.billing.balance.internal.ledger.service.BalanceQueries;
import com.eldercare.backend.billing.balance.internal.ledger.service.PaymentService;
import com.eldercare.backend.billing.balance.web.request.BankTransferPaymentRequest;
import com.eldercare.backend.billing.balance.web.request.CashPaymentRequest;
import com.eldercare.backend.billing.balance.web.response.DuplicateCodeProblemBody;
import com.eldercare.backend.common.error.web.ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Ghi nhận tiền nộp (017 T014; api-017 tag {@code payments}; FR-005 → FR-007). */
@RestController
@RequestMapping("/api/v1/residents/{residentId}")
@Tag(name = "payments")
class PaymentController {

    private static final String PROBLEM = "application/problem+json";

    private final PaymentService payments;

    PaymentController(PaymentService payments) {
        this.payments = payments;
    }

    @PostMapping("/cash-payments")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Giao dịch và tình trạng sau khi nộp"),
        @ApiResponse(
                responseCode = "403",
                description = "PERMISSION_DENIED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                description = "NO_LEDGER",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                description = "INVALID_VALUE",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<Payments.Result> cash(
            @PathVariable long residentId,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody CashPaymentRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(payments.recordCash(BalanceQueries.target(residentId), residentId, body.cash()));
    }

    @PostMapping("/bank-transfer-payments")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Các giao dịch và tình trạng sau khi nộp"),
        @ApiResponse(
                responseCode = "403",
                description = "PERMISSION_DENIED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                description = "BANK_TRANSACTION_CODE_USED (kèm duplicateTransactions), NO_LEDGER",
                content =
                        @Content(
                                mediaType = PROBLEM,
                                schema = @Schema(implementation = DuplicateCodeProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                description = "INVALID_VALUE",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<Payments.Result> bankTransfer(
            @PathVariable long residentId,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody BankTransferPaymentRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(payments.recordBankTransfer(BalanceQueries.target(residentId), residentId, body.transfer()));
    }
}
