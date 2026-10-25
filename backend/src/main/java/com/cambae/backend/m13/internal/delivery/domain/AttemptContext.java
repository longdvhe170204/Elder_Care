package com.cambae.backend.m13.internal.delivery.domain;

import com.cambae.backend.common.notification.NotificationLevel;
import java.util.Optional;

/** Dữ liệu một lần gửi cần để gửi: kênh, địa chỉ, tài khoản người nhận, mức, tiêu đề. */
public record AttemptContext(
        long attemptId,
        long notificationId,
        String channel,
        Optional<String> address,
        Optional<Long> accountId,
        NotificationLevel level,
        Optional<String> title,
        Optional<String> content) {

    public boolean urgent() {
        return level == NotificationLevel.URGENT;
    }
}
