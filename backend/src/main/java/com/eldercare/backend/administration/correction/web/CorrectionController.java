package com.eldercare.backend.administration.correction.web;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.approval.api.ApprovalRequestApi;
import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestBody;
import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.CorrectionChainView;
import com.eldercare.backend.administration.correction.api.CorrectionResult;
import com.eldercare.backend.administration.correction.web.request.CreateBody;
import com.eldercare.backend.administration.correction.web.response.ChainBody;
import com.eldercare.backend.administration.correction.web.response.CorrectionBody;
import com.eldercare.backend.common.error.web.ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code /api/v1/corrections/{recordType}/{originalRecordId}} theo contracts/api-000.yaml (000 T059). */
@RestController
@RequestMapping("/api/v1/corrections/{recordType}/{originalRecordId}")
public class CorrectionController {

    private final CorrectionApi corrections;
    private final ApprovalRequestApi approvals;
    private final ObjectProvider<AccountApi> accounts;

    public CorrectionController(
            CorrectionApi corrections, ApprovalRequestApi approvals, ObjectProvider<AccountApi> accounts) {
        this.accounts = accounts;
        this.corrections = corrections;
        this.approvals = approvals;
    }

    @GetMapping
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "404",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    public ChainBody chain(@PathVariable String recordType, @PathVariable long originalRecordId) {
        CorrectionChainView chain = corrections.chain(recordType, originalRecordId);
        return new ChainBody(
                chain.recordType(),
                chain.originalRecordId(),
                chain.corrected(),
                chain.voided(),
                chain.originalContent(),
                chain.currentContent(),
                chain.corrections().stream()
                        .map(c -> CorrectionBody.of(c, this::fullName))
                        .toList());
    }

    @PostMapping
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = CorrectionBody.class))),
        @ApiResponse(
                responseCode = "202",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ApprovalRequestBody.class))),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<Object> create(
            @PathVariable String recordType,
            @PathVariable long originalRecordId,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @RequestBody CreateBody body) {
        CorrectionResult result =
                corrections.create(recordType, originalRecordId, body.type(), body.correctedContent(), body.reason());
        return switch (result) {
            case CorrectionResult.Saved saved ->
                ResponseEntity.status(HttpStatus.CREATED).body(CorrectionBody.of(saved.correction(), this::fullName));
            case CorrectionResult.PendingApproval pending ->
                ResponseEntity.status(HttpStatus.ACCEPTED)
                        .body(ApprovalRequestBody.of(
                                approvals.detail(pending.request().id()), this::fullName));
        };
    }

    /** Họ tên theo hồ sơ tài khoản (002 T032a); không có thì {@code null}. */
    String fullName(long accountId) {
        return accounts.getObject().fullName(accountId).orElse(null);
    }
}
