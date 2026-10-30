package com.cambae.backend.m15.account.web.request;

import com.cambae.backend.m15.account.api.dto.AccessChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;

public record LoginBody(
        @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String username,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "password")
        String password,

        @NotNull
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                description = "RESIDENT_APP gắn thiết bị và trả thêm deviceToken (FR-069, FR-070); MOBILE_APP là app"
                        + " chung: kênh phiên gán theo loại tài khoản, nhân viên STAFF_APP, người thân FAMILY_APP"
                        + " (FR-013a), tài khoản người cao tuổi bị từ chối (đăng nhập bằng mã QR)")
        Channel channel) {

    /** Kênh khai khi đăng nhập: các kênh phiên ({@link AccessChannel}) và app chung {@code MOBILE_APP}. */
    public enum Channel {
        WEB,
        STAFF_APP,
        FAMILY_APP,
        RESIDENT_APP,
        MOBILE_APP;

        /** Kênh phiên đã định; rỗng với {@code MOBILE_APP} (gán theo loại tài khoản). */
        public Optional<AccessChannel> session() {
            return this == MOBILE_APP ? Optional.empty() : Optional.of(AccessChannel.valueOf(name()));
        }
    }
}
