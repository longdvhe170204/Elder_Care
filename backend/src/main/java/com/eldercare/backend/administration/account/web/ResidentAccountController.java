package com.eldercare.backend.administration.account.web;

import com.eldercare.backend.administration.account.internal.auth.service.AccountService;
import com.eldercare.backend.administration.account.internal.auth.service.AccountService.AccountDetails;
import com.eldercare.backend.administration.account.internal.auth.service.ResidentAccountService;
import com.eldercare.backend.administration.account.internal.auth.service.ResidentAccountService.Consent;
import com.eldercare.backend.administration.account.web.request.LockResidentBody;
import com.eldercare.backend.administration.account.web.request.ReasonBody;
import com.eldercare.backend.administration.account.web.request.UnlockResidentBody;
import com.eldercare.backend.administration.account.web.response.AccountBody;
import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.common.web.VersionEtag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lệnh trên tài khoản người cao tuổi (002 T092, T094; contracts/api-002.yaml mục H): gỡ thiết bị, khóa và mở khóa theo
 * lời chính người cao tuổi. Tạo tài khoản, mã QR đăng nhập ở {@code ResidentAccountCreationController} (T091, T102).
 */
@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "accounts")
class ResidentAccountController {

    private final ResidentAccountService residents;
    private final AccountService accounts;

    ResidentAccountController(ResidentAccountService residents, AccountService accounts) {
        this.residents = residents;
        this.accounts = accounts;
    }

    @PostMapping("/{id}/unbind-device")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                description = "NO_BOUND_DEVICE",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<AccountBody> unbindDevice(
            @PathVariable long id, @RequestHeader("If-Match") String ifMatch, @RequestBody ReasonBody body) {
        return ok(residents.unbindDevice(id, ifMatch, body.reason()));
    }

    @PostMapping("/{id}/lock-resident")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "412",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<AccountBody> lockResident(
            @PathVariable long id, @RequestHeader("If-Match") String ifMatch, @RequestBody LockResidentBody body) {
        return ok(residents.lockResident(
                id, ifMatch, body.basis() == null ? null : body.basis().name(), body.reason()));
    }

    @PostMapping("/{id}/unlock-resident")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                description = "LOCK_CAUSE_STILL_EXISTS (khóa do kết thúc lưu trú)",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<AccountBody> unlockResident(
            @PathVariable long id, @RequestHeader("If-Match") String ifMatch, @RequestBody UnlockResidentBody body) {
        UnlockResidentBody.ResidentConsent c = body.consent();
        return ok(residents.unlockResident(
                id,
                ifMatch,
                c == null
                        ? null
                        : new Consent(c.method() == null ? null : c.method().name(), c.withStaffId())));
    }

    private ResponseEntity<AccountBody> ok(AccountDetails details) {
        return ResponseEntity.ok()
                .eTag(VersionEtag.of(details.account().version()))
                .body(AccountBody.of(
                        details, accounts.fullName(details.account().id()).orElse(null)));
    }
}
