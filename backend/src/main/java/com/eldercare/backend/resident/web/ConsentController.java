package com.eldercare.backend.resident.web;

import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.resident.internal.consent.service.ConsentService;
import com.eldercare.backend.resident.internal.profile.service.ResidentProfileService;
import com.eldercare.backend.resident.web.request.RecordConsentBody;
import com.eldercare.backend.resident.web.request.WithdrawConsentBody;
import com.eldercare.backend.resident.web.response.ConsentBody;
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
 * Bản đồng ý chia sẻ dữ liệu (001 T057; contracts/api-001.yaml, tag {@code consents}). Mọi logic và kiểm quyền ở
 * {@link ConsentService}.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "consents")
class ConsentController {

    private static final String PROBLEM = "application/problem+json";

    private final ConsentService consents;

    ConsentController(ConsentService consents) {
        this.consents = consents;
    }

    @GetMapping("/residents/{id}/consents")
    List<ConsentBody> list(@PathVariable long id) {
        return consents.list(ResidentProfileService.target(id)).stream()
                .map(ConsentBody::of)
                .toList();
    }

    @PostMapping("/residents/{id}/consents")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã ghi nhận"),
        @ApiResponse(
                responseCode = "409",
                description = "PROFILE_CLOSED, INVALID_SIGNER",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                description = "EVIDENCE_REQUIRED, COURT_DECISION_REQUIRED, COURT_DECISION_NOT_ALLOWED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<ConsentBody> record(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody RecordConsentBody body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ConsentBody.of(consents.record(ResidentProfileService.target(id), body.command())));
    }

    @PostMapping("/consents/{id}/withdraw")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                description = "CONSENT_NOT_ACTIVE",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ConsentBody withdraw(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody WithdrawConsentBody body) {
        return ConsentBody.of(consents.withdraw(consents.targetOf(id), id, body.reason(), body.evidenceFileId()));
    }
}
