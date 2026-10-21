package com.cambae.backend.m15.account.web;

import com.cambae.backend.common.error.web.ProblemBody;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m15.account.internal.permission.domain.PermissionViews.AccountPermissions;
import com.cambae.backend.m15.account.internal.permission.domain.PermissionViews.RoleInfo;
import com.cambae.backend.m15.account.internal.permission.domain.PermissionViews.RolePermissions;
import com.cambae.backend.m15.account.internal.permission.service.PermissionChangeService;
import com.cambae.backend.m15.account.internal.permission.service.PermissionChangeService.RestrictionInput;
import com.cambae.backend.m15.account.internal.permission.service.RoleAssignmentService;
import com.cambae.backend.m15.account.web.request.ReasonBody;
import com.cambae.backend.m15.account.web.request.RestoreBody;
import com.cambae.backend.m15.account.web.request.RestrictBody;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Điều chỉnh quyền trong giới hạn ma trận (002 T076; contracts/api-002.yaml tag {@code permissions}): danh sách vai trò,
 * quyền của vai trò, thu hẹp và khôi phục theo vai trò hoặc tài khoản, gán và gỡ quyền Duyệt kế hoạch chăm sóc. Gán,
 * gỡ vai trò và {@code GET /accounts/{id}/permissions} ở {@code AccountController}.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "permissions")
class PermissionController {

    private final PermissionChangeService changes;

    PermissionController(PermissionChangeService changes) {
        this.changes = changes;
    }

    @GetMapping("/roles")
    @ApiResponse(responseCode = "200", description = "OK")
    List<RoleInfo> roles() {
        return changes.roles();
    }

    @GetMapping("/roles/{code}/permissions")
    @ApiResponse(responseCode = "200", description = "OK")
    ResponseEntity<RolePermissions> rolePermissions(
            @PathVariable
                    @Parameter(
                            schema =
                                    @Schema(
                                            type = "string",
                                            allowableValues = {
                                                "AC-01", "AC-02", "AC-03", "AC-04", "AC-05", "AC-07", "AC-09", "AC-10",
                                                "AC-14", "AC-16"
                                            }))
                    String code) {
        return role(changes.role(code));
    }

    @PostMapping("/roles/{code}/restrict")
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
    ResponseEntity<RolePermissions> restrictRole(
            @PathVariable
                    @Parameter(
                            schema =
                                    @Schema(
                                            type = "string",
                                            allowableValues = {
                                                "AC-01", "AC-02", "AC-03", "AC-04", "AC-05", "AC-07", "AC-09", "AC-10",
                                                "AC-14", "AC-16"
                                            }))
                    String code,
            @RequestHeader("If-Match") String ifMatch,
            @Valid @RequestBody RestrictBody body) {
        return role(changes.restrictRole(code, ifMatch, input(body), body.reason()));
    }

    @PostMapping("/roles/{code}/restore-permission")
    @ApiResponse(responseCode = "200", description = "OK")
    ResponseEntity<RolePermissions> restoreRole(
            @PathVariable
                    @Parameter(
                            schema =
                                    @Schema(
                                            type = "string",
                                            allowableValues = {
                                                "AC-01", "AC-02", "AC-03", "AC-04", "AC-05", "AC-07", "AC-09", "AC-10",
                                                "AC-14", "AC-16"
                                            }))
                    String code,
            @RequestHeader("If-Match") String ifMatch,
            @Valid @RequestBody RestoreBody body) {
        return role(changes.restoreRole(code, ifMatch, body.restrictionId(), body.reason()));
    }

    @PostMapping("/accounts/{id}/restrict")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                description = "EXCEEDS_MAXIMUM_PERMISSION, NO_MANAGER_LEFT",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<AccountPermissions> restrictAccount(
            @PathVariable long id, @RequestHeader("If-Match") String ifMatch, @Valid @RequestBody RestrictBody body) {
        return account(changes.restrictAccount(id, ifMatch, input(body), body.reason()));
    }

    @PostMapping("/accounts/{id}/restore-permission")
    @ApiResponse(responseCode = "200", description = "OK")
    ResponseEntity<AccountPermissions> restoreAccount(
            @PathVariable long id, @RequestHeader("If-Match") String ifMatch, @Valid @RequestBody RestoreBody body) {
        return account(changes.restoreAccount(id, ifMatch, body.restrictionId(), body.reason()));
    }

    @PostMapping("/accounts/{id}/grant-care-plan-approval")
    @ApiResponse(responseCode = "200", description = "OK")
    ResponseEntity<AccountPermissions> grantCarePlanApproval(
            @PathVariable long id, @RequestHeader("If-Match") String ifMatch, @RequestBody ReasonBody body) {
        return account(changes.grantCarePlanApproval(id, ifMatch, body.reason()));
    }

    @PostMapping("/accounts/{id}/revoke-care-plan-approval")
    @ApiResponse(responseCode = "200", description = "OK")
    ResponseEntity<AccountPermissions> revokeCarePlanApproval(
            @PathVariable long id, @RequestHeader("If-Match") String ifMatch, @RequestBody ReasonBody body) {
        return account(changes.revokeCarePlanApproval(id, ifMatch, body.reason()));
    }

    private static RestrictionInput input(RestrictBody body) {
        return new RestrictionInput(
                body.function(),
                body.operation(),
                body.kind(),
                body.metricGroup(),
                body.channel() == null ? null : body.channel().name());
    }

    private static ResponseEntity<RolePermissions> role(PermissionChangeService.RoleResult result) {
        return ResponseEntity.ok()
                .eTag(VersionEtag.of(result.permissionVersion()))
                .body(result.permissions());
    }

    private static ResponseEntity<AccountPermissions> account(RoleAssignmentService.Result result) {
        return ResponseEntity.ok()
                .eTag(VersionEtag.of(result.permissionVersion()))
                .body(result.permissions());
    }
}
