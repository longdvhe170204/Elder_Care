package com.eldercare.backend.notification.internal.delivery.service;

import com.eldercare.backend.notification.api.spi.SmsProvider;
import com.eldercare.backend.notification.api.spi.SmsResult;
import com.eldercare.backend.notification.api.spi.SmsResult.Outcome;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Bộ chuyển eSMS ({@code NhaCungCapTinNhanSms}; 009 T026a, research B-05; chủ dự án chọn eSMS 2026-11-09): gửi tin chăm
 * sóc khách hàng có brandname ({@code SmsType = 2}) qua {@code SendMultipleMessage_V4_post_json}. Mã {@code CodeResult}
 * 100 là đã gửi (eSMS đã nhận yêu cầu); lỗi mạng, HTTP 5xx, mã 99 là lỗi tạm để 009 gửi lại theo CFG-M13-05 (FR-018); mã
 * khác (ví dụ 101 khóa sai, 104 brandname sai, 146 mẫu tin chưa đăng ký) là lỗi vĩnh viễn kèm mã. {@code sandbox} bật thì
 * eSMS chỉ kiểm yêu cầu, không trừ tiền.
 */
public class EsmsSmsProvider implements SmsProvider {

    private static final Logger log = LoggerFactory.getLogger(EsmsSmsProvider.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();

    static final String ACCEPTED = "100";
    static final String CONNECTION_ERROR = "99";
    /** Tin chăm sóc khách hàng (mã OTP, thông báo); tin quảng cáo không dùng. */
    static final String CUSTOMER_CARE = "2";

    private final URI endpoint;
    private final String apiKey;
    private final String secretKey;
    private final String brandname;
    private final boolean sandbox;
    private final Duration timeout;
    private final HttpClient http;

    public EsmsSmsProvider(
            URI endpoint, String apiKey, String secretKey, String brandname, boolean sandbox, Duration timeout) {
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.brandname = brandname;
        this.sandbox = sandbox;
        this.timeout = timeout;
        this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public SmsResult send(String phoneNumber, String content) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("ApiKey", apiKey);
        body.put("SecretKey", secretKey);
        body.put("Phone", phoneNumber);
        body.put("Content", content);
        body.put("Brandname", brandname);
        body.put("SmsType", CUSTOMER_CARE);
        body.put("IsUnicode", isAscii(content) ? "0" : "1");
        body.put("Sandbox", sandbox ? "1" : "0");
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.warn("Không gọi được eSMS: {}", e.toString());
            return new SmsResult(Outcome.TEMPORARY_FAILURE, Optional.empty(), Optional.of("IO_ERROR"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new SmsResult(Outcome.TEMPORARY_FAILURE, Optional.empty(), Optional.of("INTERRUPTED"));
        }
        if (response.statusCode() >= 500) {
            return new SmsResult(
                    Outcome.TEMPORARY_FAILURE, Optional.empty(), Optional.of("HTTP_" + response.statusCode()));
        }
        return result(response);
    }

    private static SmsResult result(HttpResponse<String> response) {
        JsonNode json;
        try {
            json = JSON.readTree(response.body());
        } catch (JacksonException e) {
            return new SmsResult(
                    Outcome.PERMANENT_FAILURE, Optional.empty(), Optional.of("HTTP_" + response.statusCode()));
        }
        String code = text(json, "CodeResult").orElse(null);
        Optional<String> message = text(json, "ErrorMessage");
        if (ACCEPTED.equals(code)) {
            return new SmsResult(Outcome.SENT, text(json, "SMSID"), Optional.empty());
        }
        if (code == null) {
            return new SmsResult(
                    Outcome.PERMANENT_FAILURE, Optional.empty(), Optional.of("HTTP_" + response.statusCode()));
        }
        Outcome outcome = CONNECTION_ERROR.equals(code) ? Outcome.TEMPORARY_FAILURE : Outcome.PERMANENT_FAILURE;
        return new SmsResult(outcome, Optional.of(code), message);
    }

    private static Optional<String> text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        return node == null || node.isNull() ? Optional.empty() : Optional.of(node.asString());
    }

    private static boolean isAscii(String content) {
        return content.chars().allMatch(c -> c < 128);
    }
}
