package com.eldercare.backend.notification.internal.delivery.service;

import com.eldercare.backend.administration.account.api.spi.PasswordResetSmsPort;
import com.eldercare.backend.notification.api.spi.SmsProvider;
import com.eldercare.backend.notification.api.spi.SmsResult;
import org.springframework.stereotype.Component;

/**
 * Gửi mã đặt lại mật khẩu của 002 qua nhà cung cấp tin nhắn (002 T105; FR-052a, Q-06, Q-399). Gửi thẳng tới số trong
 * hồ sơ, không qua chọn kênh và nhật ký gửi của thông báo (mã không được lưu); không gửi lại khi lỗi tạm, người dùng xin
 * mã mới. Nhà cung cấp "chưa cấu hình" thì chức năng ẩn.
 */
@Component
class PasswordResetSms implements PasswordResetSmsPort {

    private final SmsProvider provider;

    PasswordResetSms(SmsProvider provider) {
        this.provider = provider;
    }

    @Override
    public boolean available() {
        return !(provider instanceof UnconfiguredSmsProvider);
    }

    @Override
    public boolean send(String phoneNumber, String content) {
        return provider.send(phoneNumber, content).outcome() == SmsResult.Outcome.SENT;
    }
}
