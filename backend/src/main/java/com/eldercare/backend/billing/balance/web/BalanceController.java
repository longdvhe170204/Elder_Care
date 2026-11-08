package com.eldercare.backend.billing.balance.web;

import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceDetails;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTransactionView;
import com.eldercare.backend.billing.balance.internal.ledger.service.BalanceQueries;
import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionStatus;
import com.eldercare.backend.billing.balance.web.response.BankResponse;
import com.eldercare.backend.billing.balance.web.response.PortalBalanceResponse;
import com.eldercare.backend.common.error.web.ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Sổ, giao dịch, danh mục ngân hàng, cổng người thân (017 T010, T045; api-017 tag {@code balance}, {@code family-portal}). */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "balance")
class BalanceController {

    private static final String PROBLEM = "application/problem+json";

    private final BalanceQueries queries;

    BalanceController(BalanceQueries queries) {
        this.queries = queries;
    }

    @GetMapping("/residents/{residentId}/balance")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                description = "PERMISSION_DENIED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Chưa có sổ",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    BalanceDetails balance(@PathVariable long residentId) {
        return queries.balance(BalanceQueries.target(residentId), residentId);
    }

    @GetMapping("/residents/{residentId}/balance-transactions")
    List<BalanceTransactionView> transactions(
            @PathVariable long residentId,
            @RequestParam(required = false) Ledger ledger,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) TransactionStatus status) {
        return queries.transactions(BalanceQueries.target(residentId), residentId, ledger, from, to, status);
    }

    @GetMapping("/banks")
    List<BankResponse> banks() {
        return queries.banks().stream().map(BankResponse::of).toList();
    }

    @GetMapping("/balances/by-payment-code")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "404",
                description = "Không có hồ sơ với mã này hoặc ký tự kiểm tra sai",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    BalanceDetails byPaymentCode(@RequestParam String code) {
        return queries.byPaymentCode(code);
    }

    @GetMapping("/family-portal/residents/{residentId}/balance")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                description = "PERMISSION_DENIED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    PortalBalanceResponse portal(@PathVariable long residentId) {
        return PortalBalanceResponse.of(queries.portal(BalanceQueries.target(residentId), residentId));
    }
}
