package com.cambae.backend.m15.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m15.account.auth.internal.SessionTokens;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

/** 002 T011 (research A-02): token opaque 256 bit, chỉ lưu SHA-256; cookie web {@code HttpOnly}, {@code Secure}, {@code SameSite=Strict}. */
class SessionTokensTest {

    @Test
    void A_02_token_is256RandomBits() {
        String token = SessionTokens.generate();

        assertThat(Base64.getUrlDecoder().decode(token)).hasSize(32);
        assertThat(SessionTokens.generate()).isNotEqualTo(token);
    }

    @Test
    void A_02_storedValue_isSha256OfToken() throws Exception {
        String token = SessionTokens.generate();

        assertThat(SessionTokens.hash(token))
                .isEqualTo(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    void A_02_webCookie_isHttpOnlySecureSameSiteStrict() {
        ResponseCookie cookie = SessionTokens.cookie("abc");

        assertThat(cookie.getName()).isEqualTo("session");
        assertThat(cookie.getValue()).isEqualTo("abc");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getPath()).isEqualTo("/api");
    }
}
