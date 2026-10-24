package com.cambae.backend.m13.internal.delivery.service;

import com.cambae.backend.m13.api.spi.SmsProvider;
import com.cambae.backend.m13.api.spi.SmsResult;
import com.cambae.backend.m13.internal.delivery.domain.AttemptContext;
import com.cambae.backend.m13.internal.delivery.domain.ChannelSelection;
import com.cambae.backend.m13.internal.delivery.domain.SendResult;
import com.cambae.backend.m13.internal.domain.DeliveryOutcome;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Kênh tin nhắn (FR-016, FR-018, research B-05): gửi nội dung của lần gửi (nội dung rút gọn hoặc tin tổng hợp) qua
 * {@link SmsProvider}. Lỗi ghi lý do có tiền tố {@link #TEMPORARY} hoặc {@link #PERMANENT}; chỉ lỗi tạm được gửi lại.
 */
@Component
class SmsSender implements ChannelSender {

    static final String TEMPORARY = "TEMPORARY";
    static final String PERMANENT = "PERMANENT";

    private final SmsProvider provider;

    SmsSender(SmsProvider provider) {
        this.provider = provider;
    }

    @Override
    public Set<String> channels() {
        return Set.of(ChannelSelection.SMS);
    }

    @Override
    public SendResult send(AttemptContext attempt) {
        if (attempt.address().isEmpty()) {
            return SendResult.of(DeliveryOutcome.NO_ADDRESS);
        }
        SmsResult r = provider.send(attempt.address().get(), attempt.content().orElse(""));
        return switch (r.outcome()) {
            case SENT -> SendResult.of(DeliveryOutcome.SENT);
            case NOT_CONFIGURED -> SendResult.of(DeliveryOutcome.CHANNEL_NOT_CONFIGURED);
            case TEMPORARY_FAILURE -> failed(TEMPORARY, r);
            case PERMANENT_FAILURE -> failed(PERMANENT, r);
        };
    }

    private static SendResult failed(String kind, SmsResult r) {
        String detail = r.providerCode().or(r::detail).map(s -> ":" + s).orElse("");
        return new SendResult(DeliveryOutcome.FAILED, Optional.of(kind + detail));
    }
}
