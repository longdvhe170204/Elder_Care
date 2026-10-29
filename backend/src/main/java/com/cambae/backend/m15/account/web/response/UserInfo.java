package com.cambae.backend.m15.account.web.response;

import com.cambae.backend.m15.account.api.dto.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Schema {@code UserInfo} của {@code GET /me} và phản hồi đăng nhập; {@code fullName} theo hồ sơ liên kết (T032a).
 * {@code sessionIdleTimeoutSeconds}: CFG-M15-10 của kênh phiên, chỉ {@code GET /me} trả, null khi kênh không tự đăng xuất
 * (T047a).
 */
@Schema(name = "UserInfo")
public record UserInfo(
        long accountId,
        AccountType type,
        String fullName,
        List<String> roles,
        boolean mustChangePassword,
        Long sessionIdleTimeoutSeconds) {}
