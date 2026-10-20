package com.cambae.backend.m15.account.auth.web;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.web.ProblemBody;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m15.account.api.AccountStatus;
import com.cambae.backend.m15.account.api.AccountType;
import com.cambae.backend.m15.account.auth.internal.AccountService;
import com.cambae.backend.m15.account.auth.internal.AccountService.AccountDetails;
import com.cambae.backend.m15.account.auth.internal.AccountStore.AccountRow;
import com.cambae.backend.m15.account.permission.internal.PermissionViews.AccountPermissions;
import com.cambae.backend.m15.account.permission.internal.RoleAssignmentService;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Arrays;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /api/v1/accounts} cho tài khoản nhân viên (002 T034; contracts/api-002.yaml): tạo, danh sách, chi tiết, khóa,
 * mở khóa, cấp lại mật khẩu, gán và gỡ vai trò. Không có {@code PUT}, {@code PATCH}, {@code DELETE} (FR-005,
 * constitution XI). Tài khoản người thân, người cao tuổi ở US6, US8.
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

    /** Mã vai trò nhân viên (schema {@code StaffRoleCode}). */
    @Schema(name = "StaffRoleCode")
    enum StaffRoleCode {
        AC_01(Role.NURSING_HOME_MANAGER),
        AC_02(Role.FLOOR_MANAGER),
        AC_03(Role.DOCTOR),
        AC_04(Role.NURSING_STAFF),
        AC_05(Role.CAREGIVER),
        AC_07(Role.KITCHEN_STAFF),
        AC_09(Role.OFFICE_STAFF),
        AC_14(Role.ACCOUNTANT);

        private final Role role;

        StaffRoleCode(Role role) {
            this.role = role;
        }

        @JsonValue
        String code() {
            return role.code();
        }

        @JsonCreator
        static StaffRoleCode of(String code) {
            return Arrays.stream(values())
                    .filter(r -> r.code().equals(code))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Không phải vai trò nhân viên: " + code));
        }
    }

    @Schema(name = "Account")
    record AccountBody(
            long id,
            String username,
            String fullName,
            AccountType type,
            Long staffId,
            Long familyMemberId,
            Long residentId,
            BoundDevice boundDevice,
            AccountStatus status,
            String lockCause,
            Instant lockedUntil,
            Instant lastLoginAt,
            List<String> roles,
            List<String> allowedCommands) {

        /** {@code fullName} theo hồ sơ liên kết (T032a), không có thì {@code null}. */
        static AccountBody of(AccountDetails d, String fullName) {
            AccountRow a = d.account();
            return new AccountBody(
                    a.id(),
                    a.username(),
                    fullName,
                    a.type(),
                    a.staffId(),
                    a.familyMemberId(),
                    null,
                    null,
                    a.status(),
                    a.lockCause(),
                    a.lockedUntil(),
                    a.lastLoginAt(),
                    d.roles().stream().map(Role::code).sorted().toList(),
                    d.allowedCommands());
        }
    }

    /** Thiết bị gắn của tài khoản người cao tuổi (FR-070, US8). */
    record BoundDevice(Instant boundAt, String deviceId) {}

    // Hợp đồng ghi allOf(Account, {temporaryPassword}); JSON vẫn là một object phẳng
    @Schema(
            name = "AccountWithTemporaryPassword",
            allOf = {AccountBody.class, TemporaryPassword.class})
    record AccountWithTemporaryPassword(
            long id,
            String username,
            String fullName,
            AccountType type,
            Long staffId,
            Long familyMemberId,
            Long residentId,
            BoundDevice boundDevice,
            AccountStatus status,
            String lockCause,
            Instant lockedUntil,
            Instant lastLoginAt,
            List<String> roles,
            List<String> allowedCommands,
            String temporaryPassword) {}

    @Schema(name = "AccountPage")
    record AccountPage(List<AccountBody> items, String nextCursor) {}

    record CreateStaffAccountBody(
            @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Long staffId,

            @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            String username,

            @NotNull @Size(min = 1) @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            List<StaffRoleCode> roles) {}

    record LockBody(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Reason reason,

            Long replacementDepartmentLead) {}

    record ReasonBody(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Reason reason) {}

    /** Xác minh danh tính khi cấp lại mật khẩu người thân (FR-052, US6). */
    @Schema(name = "Verification")
    record Verification(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            VerificationMethod method,

            String idDocumentNumber,
            String calledPhoneNumber) {}

    enum VerificationMethod {
        IN_PERSON_ID_DOCUMENT,
        CALLBACK_REGISTERED_NUMBER
    }

    record ResetPasswordBody(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Reason reason,

            Verification verification) {}

    record TemporaryPassword(String temporaryPassword) {}

    record EarlyUnlockBody(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Reason reason,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Boolean verifiedInPerson) {}

    record ReactivateBody(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Reason reason,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Verification verification) {}

    record RoleBody(
            @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            StaffRoleCode role,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Reason reason) {}

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
                body.roles().stream().map(r -> r.role).distinct().toList());
        AccountBody a = body(created.details());
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag(VersionEtag.of(created.details().account().version()))
                .body(new AccountWithTemporaryPassword(
                        a.id(),
                        a.username(),
                        a.fullName(),
                        a.type(),
                        a.staffId(),
                        a.familyMemberId(),
                        a.residentId(),
                        a.boundDevice(),
                        a.status(),
                        a.lockCause(),
                        a.lockedUntil(),
                        a.lastLoginAt(),
                        a.roles(),
                        a.allowedCommands(),
                        created.temporaryPassword()));
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
        return new TemporaryPassword(accounts.resetPassword(id, body.reason()));
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
        return permissions(roleAssignments.assign(id, ifMatch, body.role().role, body.reason()));
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
        return permissions(roleAssignments.remove(id, ifMatch, body.role().role, body.reason()));
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
