package com.cambae.backend.m13.internal.delivery.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m13.internal.delivery.domain.AttemptContext;
import com.cambae.backend.m13.internal.delivery.domain.SendResult;
import com.cambae.backend.m13.internal.delivery.persistence.DeliveryStore;
import com.cambae.backend.m13.internal.domain.DeliveryOutcome;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Kênh trong ứng dụng và cổng người thân (research B-05): thông báo đã vào hộp là đã gửi (FR-018); đồng thời đẩy tới
 * mọi thiết bị còn hiệu lực của tài khoản, kết quả đẩy không quyết định "Đã gửi". Token hết hạn bị vô hiệu.
 */
@Component
class InAppSender implements ChannelSender {

    static final String BODY = "Mở ứng dụng để xem thông báo.";

    private final DeliveryStore store;
    private final PushGateway push;
    private final BusinessTime time;

    InAppSender(DeliveryStore store, PushGateway push, BusinessTime time) {
        this.store = store;
        this.push = push;
        this.time = time;
    }

    @Override
    public Set<String> channels() {
        return Set.of("IN_APP", "FAMILY_PORTAL");
    }

    @Override
    public SendResult send(AttemptContext a) {
        List<String> tokens = a.accountId().map(store::activeTokens).orElse(List.of());
        PushGateway.Message message = new PushGateway.Message(
                a.title().orElse("Thông báo mới"),
                BODY,
                Map.of(
                        "notificationId",
                        Long.toString(a.notificationId()),
                        "level",
                        a.level().name()),
                a.urgent());
        int delivered = 0;
        for (String token : tokens) {
            PushGateway.Outcome outcome;
            try {
                outcome = push.push(token, message);
            } catch (RuntimeException e) {
                outcome = PushGateway.Outcome.FAILED;
            }
            if (outcome == PushGateway.Outcome.INVALID_TOKEN) {
                store.invalidateToken(token, time.now());
            } else if (outcome == PushGateway.Outcome.DELIVERED) {
                delivered++;
            }
        }
        return new SendResult(
                DeliveryOutcome.SENT,
                tokens.isEmpty() ? Optional.empty() : Optional.of("push " + delivered + "/" + tokens.size()));
    }
}
