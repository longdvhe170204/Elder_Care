package com.eldercare.backend.administration.account.web;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.spi.StaffProfileApi;
import com.eldercare.backend.administration.account.internal.auth.service.AccountService;
import com.eldercare.backend.administration.account.internal.auth.service.MyContactService;
import com.eldercare.backend.administration.account.internal.auth.service.SessionService;
import com.eldercare.backend.administration.account.internal.permission.domain.PermissionViews.EffectivePermission;
import com.eldercare.backend.administration.account.internal.permission.service.AccountPermissionsQuery;
import com.eldercare.backend.administration.account.web.request.MyContactBody;
import com.eldercare.backend.administration.account.web.response.UserInfo;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.web.ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /me}, {@code GET /me/permissions} (002 T025; FR-042; contracts/api-002.yaml): người dùng của phiên và quyền
 * hiện hành để client ẩn hiện chức năng; backend luôn kiểm lại ở lệnh (constitution XI). Quyền liệt kê là phương án
 * vai trò không vướng chú thích nhiệm vụ hay chú thích điều kiện chưa có bản cài; quyền theo nhiệm vụ thêm ở T053.
 * {@code PUT /me/contact}: liên hệ của chính mình trên hồ sơ nhân viên (chủ dự án duyệt 2026-11-09).
 */
@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "auth")
class MeController {

    private final ObjectProvider<CurrentUser> currentUser;
    private final AccountApi accounts;
    private final AccountPermissionsQuery permissions;
    private final AccountService accountService;
    private final SessionService sessions;
    private final StaffProfileApi staffProfiles;
    private final MyContactService myContact;

    MeController(
            ObjectProvider<CurrentUser> currentUser,
            AccountApi accounts,
            AccountPermissionsQuery permissions,
            AccountService accountService,
            SessionService sessions,
            StaffProfileApi staffProfiles,
            MyContactService myContact) {
        this.currentUser = currentUser;
        this.accounts = accounts;
        this.permissions = permissions;
        this.accountService = accountService;
        this.sessions = sessions;
        this.staffProfiles = staffProfiles;
        this.myContact = myContact;
    }

    @GetMapping
    UserInfo me() {
        CurrentUser user = currentUser.getObject();
        boolean mustChange = accountService.mustChangePassword(user.accountId());
        Optional<Long> staffId = user.staffId().filter(id -> user.type() == AccountType.STAFF);
        return new UserInfo(
                user.accountId(),
                user.type(),
                accounts.fullName(user.accountId()).orElse(null),
                user.roles().stream().map(Role::code).sorted().toList(),
                mustChange,
                sessions.idleLimit(user.channel()).map(Duration::toSeconds).orElse(null),
                staffId.flatMap(staffProfiles::phoneNumber).orElse(null),
                staffId.flatMap(staffProfiles::email).orElse(null));
    }

    /** Nhân viên tự sửa số điện thoại, email của mình (api-002, bổ sung 2026-11-09); trả thông tin người dùng mới. */
    @PutMapping("/contact")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đã lưu; trả thông tin người dùng mới"),
        @ApiResponse(
                responseCode = "403",
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
    UserInfo contact(@Valid @RequestBody MyContactBody body) {
        myContact.update(currentUser.getObject(), body.currentPassword(), body.phoneNumber(), body.email());
        return me();
    }

    @GetMapping("/permissions")
    List<EffectivePermission> permissions() {
        return permissions.current(currentUser.getObject().accountId());
    }
}
