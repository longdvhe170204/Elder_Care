package com.eldercare.backend.administration.account.web;

import com.eldercare.backend.administration.account.internal.auth.service.AccountService;
import com.eldercare.backend.administration.account.web.request.CreateFamilyAccountBody;
import com.eldercare.backend.administration.account.web.request.Verification;
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

/**
 * {@code POST /api/v1/family-accounts}: tạo tài khoản người thân (002 T069; contracts/api-002.yaml; FR-050 → FR-052).
 * Thiếu {@code verification} thì lệnh trả 422 như kích hoạt lại, nên trường không gắn {@code @NotNull}.
 */
@RestController
class FamilyAccountController {

    private final AccountService accounts;

    FamilyAccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @PostMapping("/api/v1/family-accounts")
    @Tag(name = "accounts")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã tạo"),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<AccountWithTemporaryPassword> create(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody CreateFamilyAccountBody body) {
        Verification v = body.verification();
        AccountService.Created created = accounts.createFamily(
                body.familyMemberId(),
                body.username().strip(),
                v == null || v.method() == null ? null : v.method().name(),
                v == null ? null : v.idDocumentNumber(),
                v == null ? null : v.calledPhoneNumber());
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag(VersionEtag.of(created.details().account().version()))
                .body(AccountWithTemporaryPassword.of(
                        created,
                        accounts.fullName(created.details().account().id()).orElse(null)));
    }
}
