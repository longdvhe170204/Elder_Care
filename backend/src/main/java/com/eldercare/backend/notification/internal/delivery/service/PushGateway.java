package com.eldercare.backend.notification.internal.delivery.service;

import java.util.Map;

/** Đẩy thông báo tới một thiết bị (FCM; research B-05, Q-305). */
public interface PushGateway {

    /**
     * Nội dung đẩy: tiêu đề không chứa sức khỏe (FR-034); nội dung đầy đủ xem trong ứng dụng.
     *
     * @param urgent đẩy ưu tiên cao (Khẩn cấp)
     */
    record Message(String title, String body, Map<String, String> data, boolean urgent) {}

    enum Outcome {
        DELIVERED,
        /** Token hết hạn hoặc không còn đăng ký: vô hiệu token. */
        INVALID_TOKEN,
        FAILED,
        NOT_CONFIGURED
    }

    Outcome push(String token, Message message);
}
