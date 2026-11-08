package com.eldercare.backend.billing.balance.web;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.billing.balance.internal.export.service.BalanceExportService;
import com.eldercare.backend.billing.balance.internal.ledger.service.BalanceQueries;
import com.eldercare.backend.billing.balance.web.request.IncomeExpenseExportRequest;
import com.eldercare.backend.billing.balance.web.request.PortalStatementRequest;
import com.eldercare.backend.billing.balance.web.request.StatementExportRequest;
import com.eldercare.backend.billing.balance.web.response.BalanceExportResponse;
import com.eldercare.backend.common.error.web.ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Xuất sao kê, báo cáo thu chi (017 T038; api-017 tag {@code balance-exports}, {@code family-portal}; FR-025 → FR-027;
 * research E-11).
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "balance-exports")
class BalanceExportController {

    private static final String PROBLEM = "application/problem+json";

    private final BalanceExportService service;

    BalanceExportController(BalanceExportService service) {
        this.service = service;
    }

    @PostMapping("/balance-exports/statement")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Lần xuất"),
        @ApiResponse(
                responseCode = "422",
                description = "EXPORT_RANGE_EXCEEDED (CFG-M14-01)",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<BalanceExportResponse> statement(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey, @Valid @RequestBody StatementExportRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BalanceExportResponse.of(service.statement(
                        Target.UNSPECIFIED, body.residentIds(), body.from(), body.to(), body.format())));
    }

    @PostMapping("/balance-exports/income-expense-report")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Lần xuất"),
        @ApiResponse(
                responseCode = "403",
                description = "PERMISSION_DENIED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                description = "EXPORT_RANGE_EXCEEDED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<BalanceExportResponse> incomeExpense(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody IncomeExpenseExportRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BalanceExportResponse.of(
                        service.incomeExpense(Target.UNSPECIFIED, body.from(), body.to(), body.format())));
    }

    @GetMapping("/balance-exports/{id}/file")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "File",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                                schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(
                responseCode = "403",
                description = "PERMISSION_DENIED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<byte[]> file(@PathVariable long id) {
        return body(service.download(Target.UNSPECIFIED, id));
    }

    @PostMapping("/family-portal/residents/{residentId}/balance-statement")
    @Tag(name = "family-portal")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "File sao kê",
                content = {
                    @Content(
                            mediaType = MediaType.APPLICATION_PDF_VALUE,
                            schema = @Schema(type = "string", format = "binary")),
                    @Content(
                            mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            schema = @Schema(type = "string", format = "binary"))
                }),
        @ApiResponse(
                responseCode = "403",
                description = "PERMISSION_DENIED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                description = "EXPORT_RANGE_EXCEEDED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<byte[]> portalStatement(
            @PathVariable long residentId,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody PortalStatementRequest body) {
        return body(service.portalStatement(
                BalanceQueries.target(residentId), residentId, body.from(), body.to(), body.format()));
    }

    private static ResponseEntity<byte[]> body(BalanceExportService.Download download) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.format().mimeType()))
                .body(download.file().content());
    }
}
