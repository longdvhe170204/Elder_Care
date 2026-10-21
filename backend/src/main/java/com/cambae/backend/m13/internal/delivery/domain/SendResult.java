package com.cambae.backend.m13.internal.delivery.domain;

import com.cambae.backend.m13.internal.domain.DeliveryOutcome;
import java.util.Optional;

/** Kết quả gửi của một kênh, ghi vào {@code delivery_result}. */
public record SendResult(DeliveryOutcome outcome, Optional<String> reason) {

    public static SendResult of(DeliveryOutcome outcome) {
        return new SendResult(outcome, Optional.empty());
    }
}
