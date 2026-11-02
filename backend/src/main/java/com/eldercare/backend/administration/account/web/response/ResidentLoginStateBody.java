package com.eldercare.backend.administration.account.web.response;

import com.eldercare.backend.administration.account.api.dto.AccountStatus;
import com.eldercare.backend.administration.account.internal.auth.service.ResidentAccountService.LoginState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Thân {@code GET /resident-accounts/{residentId}}: đủ để người tạo mã biết có máy đang gắn không (FR-069). */
@Schema(name = "ResidentLoginState")
public record ResidentLoginStateBody(long accountId, AccountStatus status, Device boundDevice) {

    public record Device(Instant boundAt) {}

    public static ResidentLoginStateBody of(LoginState state) {
        return new ResidentLoginStateBody(
                state.accountId(), state.status(), state.boundAt() == null ? null : new Device(state.boundAt()));
    }
}
