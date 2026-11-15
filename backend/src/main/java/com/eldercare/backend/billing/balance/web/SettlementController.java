package com.eldercare.backend.billing.balance.web;

import com.eldercare.backend.billing.balance.internal.ledger.service.BalanceQueries;
import com.eldercare.backend.billing.balance.internal.settlement.service.SettlementService;
import com.eldercare.backend.billing.balance.web.request.SignedCopyRequest;
import com.eldercare.backend.billing.balance.web.response.SettlementResponse;
import com.eldercare.backend.billing.balance.web.response.SettlementStatementResponse;
import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.common.file.FileContent;
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

/** Quyết toán (017 T032; api-017 tag {@code settlement}; FR-022 → FR-024, FR-034, FR-037; BR-M11-22, 23). */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "settlement")
class SettlementController {

    private static final String PROBLEM = "application/problem+json";

    private final SettlementService service;

    SettlementController(SettlementService service) {
        this.service = service;
    }

    @GetMapping("/residents/{residentId}/settlement")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    SettlementResponse settlement(@PathVariable long residentId) {
        return SettlementResponse.of(service.view(BalanceQueries.target(residentId), residentId));
    }

    @PostMapping("/residents/{residentId}/settlement/confirm")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Phiên bản bảng quyết toán"),
        @ApiResponse(
                responseCode = "403",
                description = "PERMISSION_DENIED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                description = "NO_LEDGER, NOT_FINAL_STATUS, SETTLEMENT_CONDITIONS_NOT_MET, PENDING_TRANSACTIONS_REMAIN",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<SettlementStatementResponse> confirm(
            @PathVariable long residentId, @RequestHeader("Idempotency-Key") UUID idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SettlementStatementResponse.of(service.confirm(BalanceQueries.target(residentId), residentId)));
    }

    @PostMapping("/settlement-statements/{id}/signed-copy")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                description = "NOT_CURRENT_VERSION, SIGNED_COPY_EXISTS, FILE_ALREADY_ATTACHED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    void signedCopy(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody SignedCopyRequest body) {
        service.attachSignedCopy(BalanceQueries.target(service.residentOfStatement(id)), id, body.fileId());
    }

    @GetMapping("/settlement-statements/{id}/file")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "PDF",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_PDF_VALUE,
                                schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(
                responseCode = "403",
                description = "PERMISSION_DENIED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<byte[]> file(@PathVariable long id) {
        FileContent file = service.download(BalanceQueries.target(service.residentOfStatement(id)), id);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(file.content());
    }
}
