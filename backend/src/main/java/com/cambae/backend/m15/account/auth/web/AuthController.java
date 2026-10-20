package com.cambae.backend.m15.account.auth.web;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.web.ProblemBody;
import com.cambae.backend.m15.account.api.AccessChannel;
import com.cambae.backend.m15.account.api.AccountApi;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.auth.internal.LoginService;
import com.cambae.backend.m15.account.auth.internal.SessionTokens;
import com.cambae.backend.m15.account.permission.web.UserInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Đăng nhập, đăng xuất, tự đổi mật khẩu (002 T044; contracts/api-002.yaml {@code /auth/*}; research A-02, A-03). Web
 * nhận token trong cookie {@code session}, app nhận trong thân phản hồi. Lỗi đăng nhập trả sau khi transaction lưu
 * bộ đếm sai và nhật ký.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "auth")
class AuthController {

    private final LoginService logins;
    private final AccountApi accounts;
    private final ObjectProvider<CurrentUser> currentUser;

    AuthController(LoginService logins, AccountApi accounts, ObjectProvider<CurrentUser> currentUser) {
        this.logins = logins;
        this.accounts = accounts;
        this.currentUser = currentUser;
    }

    record LoginBody(
            @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            String username,

            @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "password")
            String password,

            @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            AccessChannel channel) {}

    /** {@code token} chỉ với kênh app; {@code deviceToken} chỉ kênh người cao tuổi (US8). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record LoginResponse(String token, String deviceToken, boolean mustChangePassword, UserInfo user) {}

    record ChangePasswordBody(
            @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "password")
            String currentPassword,

            @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "password")
            String newPassword) {}

    @PostMapping("/login")
    @SecurityRequirements
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đăng nhập thành công"),
        @ApiResponse(
                responseCode = "401",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "423",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<LoginResponse> login(
            @RequestHeader("X-Device-Id") @Parameter(schema = @Schema(type = "string", format = "uuid")) UUID deviceId,
            @Valid @RequestBody LoginBody body) {
        LoginService.Result result =
                logins.login(body.username().strip(), body.password(), body.channel(), deviceId.toString());
        if (result instanceof LoginService.Refused refused) {
            throw new BusinessException(
                    HttpStatus.valueOf(refused.httpStatus()),
                    refused.errorCode(),
                    message(refused.errorCode()),
                    refused.unlocksAt() == null ? Map.of() : Map.of("unlocksAt", refused.unlocksAt()));
        }
        LoginService.Success success = (LoginService.Success) result;
        UserInfo user = new UserInfo(
                success.accountId(),
                success.type(),
                accounts.fullName(success.accountId()).orElse(null),
                success.roles().stream().map(Role::code).sorted().toList(),
                success.mustChangePassword());
        boolean web = body.channel() == AccessChannel.WEB;
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (web) {
            response.header(
                    HttpHeaders.SET_COOKIE,
                    SessionTokens.cookie(success.token()).toString());
        }
        return response.body(new LoginResponse(web ? null : success.token(), null, success.mustChangePassword(), user));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponse(responseCode = "204", description = "Đã đăng xuất")
    void logout(HttpServletRequest request) {
        logins.logout(token(request));
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Đã đổi; cờ buộc đổi mật khẩu được gỡ"),
        @ApiResponse(
                responseCode = "422",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    void changePassword(@Valid @RequestBody ChangePasswordBody body) {
        logins.changePassword(currentUser.getObject(), body.currentPassword(), body.newPassword());
    }

    /** Thông báo cho người dùng; không nói tên đăng nhập có tồn tại hay không (FR-014). */
    private static String message(String errorCode) {
        return switch (errorCode) {
            case "ACCOUNT_TEMPORARILY_LOCKED" -> "Tài khoản đang bị khóa tạm do đăng nhập sai nhiều lần";
            case "ACCOUNT_CONTACT_FACILITY" -> "Tài khoản không đăng nhập được, cần liên hệ viện";
            default -> "Tên đăng nhập hoặc mật khẩu không đúng";
        };
    }

    /** Token của yêu cầu: Bearer trước, cookie sau (như bộ lọc phiên). */
    private static String token(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring("Bearer ".length()).strip();
        }
        Cookie[] cookies = request.getCookies();
        return cookies == null
                ? ""
                : Arrays.stream(cookies)
                        .filter(c -> SessionTokens.COOKIE.equals(c.getName()))
                        .map(Cookie::getValue)
                        .findFirst()
                        .orElse("");
    }
}
