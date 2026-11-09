package com.eldercare.backend.notification.internal.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.notification.api.spi.SmsResult;
import com.eldercare.backend.support.fake.notification.FakeSmsProvider;
import org.junit.jupiter.api.Test;

/** 002 T105 (FR-052a, Q-06): chưa cấu hình nhà cung cấp tin nhắn thì ẩn "Quên mật khẩu"; chỉ SENT là đã gửi. */
class PasswordResetSmsTest {

    @Test
    void Q_06_unconfiguredProvider_unavailable_sentOnlyWhenProviderAccepts() {
        assertThat(new PasswordResetSms(new UnconfiguredSmsProvider()).available())
                .isFalse();

        FakeSmsProvider provider = new FakeSmsProvider()
                .thenReturn(SmsResult.of(SmsResult.Outcome.TEMPORARY_FAILURE), SmsResult.of(SmsResult.Outcome.SENT));
        PasswordResetSms sms = new PasswordResetSms(provider);
        assertThat(sms.available()).isTrue();
        assertThat(sms.send("0912000111", "Ma 123456")).isFalse();
        assertThat(sms.send("0912000111", "Ma 123456")).isTrue();
        assertThat(provider.sent()).hasSize(2);
    }
}
