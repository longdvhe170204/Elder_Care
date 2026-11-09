package com.eldercare.backend.belongings.web;

import com.eldercare.backend.belongings.internal.consent.service.SelfCustodyConsentService;
import com.eldercare.backend.belongings.web.request.SelfCustodyConsentRequests;
import com.eldercare.backend.belongings.web.response.IdResponse;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.web.ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Đồng ý cho tự giữ (013 T011; api-013 tag {@code belongings}, {@code family-portal}): Hành chính ghi bản ký; người đại
 * diện ghi, rút qua cổng.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "belongings")
class SelfCustodyConsentController {

    private final SelfCustodyConsentService consents;

    SelfCustodyConsentController(SelfCustodyConsentService consents) {
        this.consents = consents;
    }

    @PostMapping("/self-custody-consents")
    @ApiResponse(
            responseCode = "409",
            description = "Lỗi",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemBody.class)))
    @ApiResponse(
            responseCode = "201",
            description = "OK",
            content = @Content(schema = @Schema(implementation = IdResponse.class)))
    @ResponseStatus(HttpStatus.CREATED)
    IdResponse record(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @RequestBody SelfCustodyConsentRequests.Record body) {
        if (body.belongingId() == null || body.representativeId() == null || body.signedFormFileId() == null) {
            throw invalid();
        }
        return new IdResponse(
                consents.recordBySignedForm(body.belongingId(), body.representativeId(), body.signedFormFileId()));
    }

    @PostMapping("/self-custody-consents/{id}/withdraw")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    @ApiResponse(
            responseCode = "409",
            description = "Lỗi",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemBody.class)))
    void withdraw(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @RequestBody SelfCustodyConsentRequests.Withdraw body) {
        if (body.representativeId() == null || body.signedFormFileId() == null) {
            throw invalid();
        }
        consents.withdrawBySignedForm(id, body.reason(), body.representativeId(), body.signedFormFileId());
    }

    @PostMapping("/family-portal/self-custody-consents")
    @ApiResponse(
            responseCode = "403",
            description = "Lỗi",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemBody.class)))
    @ApiResponse(
            responseCode = "409",
            description = "Lỗi",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemBody.class)))
    @ApiResponse(
            responseCode = "201",
            description = "OK",
            content = @Content(schema = @Schema(implementation = IdResponse.class)))
    @ResponseStatus(HttpStatus.CREATED)
    IdResponse recordOnPortal(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @RequestBody SelfCustodyConsentRequests.PortalRecord body) {
        if (body.belongingId() == null) {
            throw invalid();
        }
        return new IdResponse(consents.recordOnPortal(body.belongingId()));
    }

    @PostMapping("/family-portal/self-custody-consents/{id}/withdraw")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    @ApiResponse(
            responseCode = "403",
            description = "Lỗi",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemBody.class)))
    void withdrawOnPortal(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @RequestBody SelfCustodyConsentRequests.PortalWithdraw body) {
        consents.withdrawOnPortal(id, body.reason());
    }

    private static BusinessException invalid() {
        return new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_VALUE", "Thiếu trường bắt buộc");
    }
}
