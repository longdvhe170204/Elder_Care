package com.eldercare.backend.administration.account.internal.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.http.ResponseCookie;

/**
 * Token phiên opaque 256 bit; máy chủ chỉ lưu SHA-256 ở {@code login_session.token_hash} (002 research A-02). Web nhận
 * token trong cookie {@code session}, app nhận trong thân phản hồi đăng nhập và gửi lại bằng {@code Authorization:
 * Bearer}.
 */
public final class SessionTokens {

    public static final String COOKIE = "session";
    private static final SecureRandom RANDOM = new SecureRandom();

    private SessionTokens() {}

    public static String generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Mã QR đăng nhập 128 bit (research A-15). */
    public static String generateCode() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static byte[] hash(String token) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Cookie phiên của web; không đặt Max-Age vì hạn phiên do máy chủ kiểm theo CFG-M15-10. */
    public static ResponseCookie cookie(String token) {
        return ResponseCookie.from(COOKIE, token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api")
                .build();
    }
}
