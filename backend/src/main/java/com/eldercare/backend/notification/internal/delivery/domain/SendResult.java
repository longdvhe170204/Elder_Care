package com.eldercare.backend.notification.internal.delivery.domain;

import com.eldercare.backend.notification.internal.domain.DeliveryOutcome;
import java.util.Optional;

/** Kết quả gửi của một kênh, ghi thành kết quả lần gửi ({@code record_event} loại {@code M13.DELIVERY_RESULT}). */
public record SendResult(DeliveryOutcome outcome, Optional<String> reason) {

    public static SendResult of(DeliveryOutcome outcome) {
        return new SendResult(outcome, Optional.empty());
    }
}
