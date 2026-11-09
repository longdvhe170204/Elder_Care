package com.eldercare.backend.notification.internal.delivery.service;

import com.eldercare.backend.notification.api.spi.SmsProvider;
import java.net.URI;
import java.time.Duration;

/**
 * Chọn nhà cung cấp tin nhắn theo cấu hình (009 T026a, Q-06): chưa có khóa API eSMS thì "chưa cấu hình" (chỉ thông báo
 * trong ứng dụng, chưa có tự đặt lại mật khẩu); có khóa thì gửi qua eSMS.
 */
public final class SmsProviders {

    static final String ESMS_PATH = "/MainService.svc/json/SendMultipleMessage_V4_post_json/";
    /** Thời gian chờ kỹ thuật khi gọi eSMS, không phải mốc nghiệp vụ (constitution XIII chỉ áp cho mốc nghiệp vụ). */
    static final int TIMEOUT_SECONDS = 10;

    private SmsProviders() {}

    public static SmsProvider from(String apiKey, String secretKey, String brandname, boolean sandbox, String baseUrl) {
        if (apiKey == null || apiKey.isBlank()) {
            return new UnconfiguredSmsProvider();
        }
        URI endpoint = URI.create(baseUrl.replaceAll("/+$", "") + ESMS_PATH);
        return new EsmsSmsProvider(
                endpoint, apiKey, secretKey, brandname, sandbox, Duration.ofSeconds(TIMEOUT_SECONDS));
    }
}
