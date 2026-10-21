package com.cambae.backend.m15.account.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** Phản hồi đăng nhập bằng mã QR: token phiên, token thiết bị trả một lần cho nút SOS (research A-15). */
public record QrLoginResponse(
        String token,

        @Schema(description = "Trả một lần; app lưu trong kho an toàn")
        String deviceToken,

        UserInfo user) {}
