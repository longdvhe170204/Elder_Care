package com.cambae.backend.m13.internal.delivery.config;

import com.cambae.backend.m13.api.spi.SmsProvider;
import com.cambae.backend.m13.internal.delivery.service.FcmPushGateway;
import com.cambae.backend.m13.internal.delivery.service.PushGateway;
import com.cambae.backend.m13.internal.delivery.service.UnconfiguredPushGateway;
import com.cambae.backend.m13.internal.delivery.service.UnconfiguredSmsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cổng đẩy FCM (T001, T008): có tệp khóa dịch vụ ({@code FIREBASE_CREDENTIALS}, không ghi vào kho mã) thì dùng Firebase
 * Admin SDK, thiếu thì bản "chưa cấu hình". Nhà cung cấp tin nhắn: "chưa cấu hình" tới khi chốt nhà cung cấp (Q-06, T026a).
 */
@Configuration
class PushGatewayConfig {

    @Bean
    PushGateway pushGateway(@Value("${eldercare.notification.fcm.credentials-file:}") String credentialsFile)
            throws IOException {
        if (credentialsFile.isBlank()) {
            return new UnconfiguredPushGateway();
        }
        try (InputStream in = Files.newInputStream(Path.of(credentialsFile))) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(in))
                    .build();
            FirebaseApp app = FirebaseApp.getApps().stream()
                    .filter(a -> a.getName().equals("eldercare"))
                    .findFirst()
                    .orElseGet(() -> FirebaseApp.initializeApp(options, "eldercare"));
            return new FcmPushGateway(FirebaseMessaging.getInstance(app));
        }
    }

    @Bean
    SmsProvider smsProvider() {
        return new UnconfiguredSmsProvider();
    }
}
