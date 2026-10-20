package com.cambae.backend.m15.account.web;

import com.cambae.backend.common.error.web.ProblemBody;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m15.account.api.dto.AccountStatus;
import com.cambae.backend.m15.account.api.dto.AccountType;
import com.cambae.backend.m15.account.internal.auth.service.AccountService;
import com.cambae.backend.m15.account.internal.auth.service.AccountService.AccountDetails;
import com.cambae.backend.m15.account.internal.permission.PermissionViews.AccountPermissions;
import com.cambae.backend.m15.account.internal.permission.RoleAssignmentService;
import com.cambae.backend.m15.account.web.request.CreateStaffAccountBody;
import com.cambae.backend.m15.account.web.request.EarlyUnlockBody;
import com.cambae.backend.m15.account.web.request.LockBody;
import com.cambae.backend.m15.account.web.request.ReactivateBody;
import com.cambae.backend.m15.account.web.request.ReasonBody;
import com.cambae.backend.m15.account.web.request.ResetPasswordBody;
import com.cambae.backend.m15.account.web.request.RoleBody;
import com.cambae.backend.m15.account.web.request.Verification;
import com.cambae.backend.m15.account.web.response.AccountBody;
import com.cambae.backend.m15.account.web.response.AccountPage;
import com.cambae.backend.m15.account.web.response.AccountWithTemporaryPassword;
import com.cambae.backend.m15.account.web.response.TemporaryPassword;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /api/v1/accounts} cho tài khoản nhân viên (002 T034; contracts/api-002.yaml): tạo, danh sách, chi tiết, khóa,
 * mở khóa, cấp lại mật khẩu, gán và gỡ vai trò. Không có {@code PUT}, {@code PATCH}, {@code DELETE} (FR-005,
 * constitution XI). Tạo tài khoản người thân ở {@code FamilyAccountController} (US6); người cao tuổi ở US8.
 */
@RestController
@RequestMapping("/api/v1/accounts")
class AccountController {

    private final AccountService accounts;
    private final RoleAssignmentService roleAssignments;

    AccountController(AccountService accounts, RoleAssignmentService roleAssignments) {
        this.accounts = accounts;
        this.roleAssignments = roleAssignments;
    }

    @GetMapping
    @Tag(name = "accounts")
    AccountPage list(
            @RequestParam(required = false)
                    @Parameter(
                            schema =
                                    @Schema(
                                            type = "string",
                                            allowableValues = {"STAFF", "FAMILY_MEMBER"}))
                    AccountType type,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String cursor) {
        AccountService.Page page = accounts.page(type, status, q, cursor);
        return new AccountPage(page.items().stream().map(this::body).toList(), page.nextCursor());
    }

    @PostMapping
    @Tag(name = "accounts")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã tạo; mật khẩu tạm chỉ trả một lần"),
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
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<AccountWithTemporaryPassword> create(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody CreateStaffAccountBody body) {
        AccountService.Created created = accounts.createStaff(
                body.staffId(),
                body.username().strip(),
                body.roles().stream().map(r -> r.role()).distinct().toList());
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag(VersionEtag.of(created.details().account().version()))
                .body(AccountWithTemporaryPassword.of(
                        created,
                        accounts.fullName(created.details().account().id()).orElse(null)));
    }

    @GetMapping("/{id}")
    @Tag(name = "accounts")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "404",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<AccountBody> detail(@PathVariable long id) {
        AccountDetails details = accounts.detail(id);
        return ResponseEntity.ok()
                .eTag(VersionEtag.of(details.account().version()))
                .body(body(details));
    }

    @PostMapping("/{id}/lock")
    @Tag(name = "accounts")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "412",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<AccountBody> lock(
            @PathVariable long id, @RequestHeader("If-Match") String ifMatch, @RequestBody LockBody body) {
        return ok(accounts.lock(id, ifMatch, body.reason(), body.replacementDepartmentLead()));
    }

    @PostMapping("/{id}/early-unlock")
    @Tag(name = "accounts")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<AccountBody> earlyUnlock(
            @PathVariable long id, @RequestHeader("If-Match") String ifMatch, @RequestBody EarlyUnlockBody body) {
        return ok(accounts.earlyUnlock(id, ifMatch, body.reason(), body.verifiedInPerson()));
    }

    @PostMapping("/{id}/reactivate")
    @Tag(name = "accounts")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<AccountBody> reactivate(
            @PathVariable long id, @RequestHeader("If-Match") String ifMatch, @RequestBody ReactivateBody body) {
        Verification v = body.verification();
        return ok(accounts.reactivate(
                id,
                ifMatch,
                body.reason(),
                v == null || v.method() == null ? null : v.method().name(),
                v == null ? null : v.idDocumentNumber(),
                v == null ? null : v.calledPhoneNumber()));
    }

    @PostMapping("/{id}/unlock")
    @Tag(name = "accounts")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<AccountBody> unlock(
            @PathVariable long id, @RequestHeader("If-Match") String ifMatch, @RequestBody ReasonBody body) {
        return ok(accounts.unlock(id, ifMatch, body.reason()));
    }

    @PostMapping("/{id}/reset-password")
    @Tag(name = "accounts")
    @ApiResponse(responseCode = "200", description = "Mật khẩu tạm, chỉ trả một lần")
    TemporaryPassword resetPassword(@PathVariable long id, @RequestBody ResetPasswordBody body) {
        Verification v = body.verification();
        return new TemporaryPassword(accounts.resetPassword(
                id,
                body.reason(),
                v == null || v.method() == null ? null : v.method().name(),
                v == null ? null : v.idDocumentNumber(),
                v == null ? null : v.calledPhoneNumber()));
    }

    /** Phần hiện hành (T076 kéo lên cho màn T035); {@code asOf} làm ở T076. */
    @GetMapping("/{id}/permissions")
    @Tag(name = "permissions")
    @ApiResponse(responseCode = "200", description = "OK")
    ResponseEntity<AccountPermissions> permissions(
            @PathVariable long id, @RequestParam(required = false) Instant asOf) {
        if (asOf != null) {
            throw new UnsupportedOperationException("Tập quyền tại thời điểm quá khứ làm ở 002 T076");
        }
        return permissions(roleAssignments.current(id));
    }

    @PostMapping("/{id}/assign-role")
    @Tag(name = "permissions")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "412",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<AccountPermissions> assignRole(
            @PathVariable long id, @RequestHeader("If-Match") String ifMatch, @Valid @RequestBody RoleBody body) {
        return permissions(roleAssignments.assign(id, ifMatch, body.role().role(), body.reason()));
    }

    @PostMapping("/{id}/remove-role")
    @Tag(name = "permissions")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<AccountPermissions> removeRole(
            @PathVariable long id, @RequestHeader("If-Match") String ifMatch, @Valid @RequestBody RoleBody body) {
        return permissions(roleAssignments.remove(id, ifMatch, body.role().role(), body.reason()));
    }

    private ResponseEntity<AccountBody> ok(AccountDetails details) {
        return ResponseEntity.ok()
                .eTag(VersionEtag.of(details.account().version()))
                .body(body(details));
    }

    private AccountBody body(AccountDetails details) {
        return AccountBody.of(details, accounts.fullName(details.account().id()).orElse(null));
    }

    private static ResponseEntity<AccountPermissions> permissions(RoleAssignmentService.Result result) {
        return ResponseEntity.ok()
                .eTag(VersionEtag.of(result.permissionVersion()))
                .body(result.permissions());
    }
}
