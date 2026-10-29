package com.eldercare.backend.administration.account.web;

import com.eldercare.backend.administration.account.internal.auth.service.AccountService;
import com.eldercare.backend.administration.account.web.request.CreateResidentAccountBody;
import com.eldercare.backend.administration.account.web.request.UnlockResidentBody.ResidentConsent;
import com.eldercare.backend.administration.account.web.response.AccountWithTemporaryPassword;
import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.common.web.VersionEtag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** {@code POST /api/v1/resident-accounts}: tạo tài khoản người cao tuổi (002 T091; contracts/api-002.yaml; FR-067). */
@RestController
class ResidentAccountCreationController {

    private final AccountService accounts;

    ResidentAccountCreationController(AccountService accounts) {
        this.accounts = accounts;
    }

    @PostMapping("/api/v1/resident-accounts")
    @Tag(name = "accounts")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã tạo"),
        @ApiResponse(
                responseCode = "409",
                description = "ALREADY_HAS_ACCOUNT, NOT_IN_RESIDENCE",
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
    ResponseEntity<AccountWithTemporaryPassword> create(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody CreateResidentAccountBody body) {
        ResidentConsent consent = body.consent();
        AccountService.Created created = accounts.createResident(
                body.residentId(),
                body.username().strip(),
                consent == null || consent.method() == null
                        ? null
                        : consent.method().name(),
                consent == null ? null : consent.withStaffId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag(VersionEtag.of(created.details().account().version()))
                .body(AccountWithTemporaryPassword.of(
                        created,
                        accounts.fullName(created.details().account().id()).orElse(null)));
    }
}
