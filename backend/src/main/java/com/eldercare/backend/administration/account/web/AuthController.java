package com.eldercare.backend.administration.account.web;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.internal.auth.service.LoginService;
import com.eldercare.backend.administration.account.internal.auth.service.PasswordResetService;
import com.eldercare.backend.administration.account.internal.auth.service.SessionTokens;
import com.eldercare.backend.administration.account.web.request.ChangePasswordBody;
import com.eldercare.backend.administration.account.web.request.LoginBody;
import com.eldercare.backend.administration.account.web.request.PasswordResetConfirmBody;
import com.eldercare.backend.administration.account.web.request.PasswordResetRequestBody;
import com.eldercare.backend.administration.account.web.request.QrLoginBody;
import com.eldercare.backend.administration.account.web.response.LoginResponse;
import com.eldercare.backend.administration.account.web.response.PasswordResetAvailability;
import com.eldercare.backend.administration.account.web.response.QrLoginResponse;
import com.eldercare.backend.administration.account.web.response.UserInfo;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.web.ProblemBody;
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
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final PasswordResetService resets;

    AuthController(
            LoginService logins,
            AccountApi accounts,
            ObjectProvider<CurrentUser> currentUser,
            PasswordResetService resets) {
        this.resets = resets;
        this.logins = logins;
        this.accounts = accounts;
        this.currentUser = currentUser;
    }

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
                responseCode = "409",
                description = "DEVICE_ALREADY_BOUND (thiết bị đang gắn với tài khoản người cao tuổi khác, FR-014a)",
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
        LoginService.Result result = logins.login(
                body.username().strip(), body.password(), body.channel().session(), deviceId.toString());
        if (result instanceof LoginService.Refused refused) {
            throw new BusinessException(
                    HttpStatus.valueOf(refused.httpStatus()),
                    refused.errorCode(),
                    message(refused.errorCode()),
                    refused.unlocksAt() == null ? Map.of() : Map.of("unlocksAt", refused.unlocksAt()));
        }
        LoginService.Success success = (LoginService.Success) result;
        UserInfo user = user(success);
        boolean web = body.channel() == LoginBody.Channel.WEB;
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (web) {
            response.header(
                    HttpHeaders.SET_COOKIE,
                    SessionTokens.cookie(success.token()).toString());
        }
        return response.body(new LoginResponse(
                web ? null : success.token(), success.deviceToken(), success.mustChangePassword(), user));
    }

    /** Người cao tuổi đăng nhập bằng mã QR, gắn thiết bị (002 T092; FR-069, FR-070; research A-15). */
    @PostMapping("/qr-login")
    @SecurityRequirements
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đã đăng nhập và gắn thiết bị"),
        @ApiResponse(
                responseCode = "401",
                description = "INVALID_QR_CODE (đã dùng, hết hạn, đã vô hiệu)",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                description = "DEVICE_ALREADY_BOUND (thiết bị đang gắn với tài khoản người cao tuổi khác)",
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
    QrLoginResponse qrLogin(
            @RequestHeader("X-Device-Id") @Parameter(schema = @Schema(type = "string", format = "uuid")) UUID deviceId,
            @Valid @RequestBody QrLoginBody body) {
        LoginService.Success success = logins.qrLogin(body.code(), deviceId.toString());
        return new QrLoginResponse(success.token(), success.deviceToken(), user(success));
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

    /** Màn đăng nhập có hiện "Quên mật khẩu" không (002 T105; FR-052a, Q-06). */
    @GetMapping("/password-reset/availability")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "OK")
    PasswordResetAvailability passwordResetAvailability() {
        return new PasswordResetAvailability(resets.available());
    }

    /** Xin mã đặt lại mật khẩu; luôn 202 như nhau (FR-052a, BR-M15-25). */
    @PostMapping("/password-reset/request")
    @SecurityRequirements
    @ResponseStatus(HttpStatus.ACCEPTED)
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Đã nhận yêu cầu"),
        @ApiResponse(
                responseCode = "404",
                description = "Kênh tin nhắn chưa cấu hình",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    void requestPasswordReset(@Valid @RequestBody PasswordResetRequestBody body) {
        resets.request(body.username());
    }

    /** Đặt mật khẩu mới bằng mã (FR-052a, BR-M15-25); lỗi trả sau khi transaction lưu bộ đếm sai. */
    @PostMapping("/password-reset/confirm")
    @SecurityRequirements
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Đã đặt mật khẩu mới"),
        @ApiResponse(
                responseCode = "422",
                description = "RESET_CODE_INVALID, PASSWORD_POLICY_NOT_MET",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    void confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmBody body) {
        switch (resets.confirm(body.username(), body.code(), body.newPassword())) {
            case PasswordResetService.Done done -> {}
            case PasswordResetService.InvalidCode invalid ->
                throw new BusinessException(
                        HttpStatus.UNPROCESSABLE_CONTENT,
                        "RESET_CODE_INVALID",
                        "Mã không đúng, đã hết hạn hoặc không còn hiệu lực; hãy xin mã mới");
            case PasswordResetService.WeakPassword weak ->
                throw new BusinessException(
                        HttpStatus.UNPROCESSABLE_CONTENT,
                        "PASSWORD_POLICY_NOT_MET",
                        "Mật khẩu mới chưa đạt yêu cầu",
                        Map.of("unmetConditions", weak.unmet()));
        }
    }

    private UserInfo user(LoginService.Success success) {
        return new UserInfo(
                success.accountId(),
                success.type(),
                accounts.fullName(success.accountId()).orElse(null),
                success.roles().stream().map(Role::code).sorted().toList(),
                success.mustChangePassword(),
                null,
                null,
                null);
    }

    /** Thông báo cho người dùng; không nói tên đăng nhập có tồn tại hay không (FR-014). */
    private static String message(String errorCode) {
        return switch (errorCode) {
            case "ACCOUNT_TEMPORARILY_LOCKED" -> "Tài khoản đang bị khóa tạm do đăng nhập sai nhiều lần";
            case "ACCOUNT_CONTACT_FACILITY" -> "Tài khoản không đăng nhập được, cần liên hệ viện";
            case "DEVICE_ALREADY_BOUND" -> "Thiết bị đang gắn với tài khoản người cao tuổi khác; cần gỡ thiết bị trước";
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
