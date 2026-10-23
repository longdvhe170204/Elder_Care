package com.cambae.backend.m13.internal.delivery.service;

import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.m13.api.dto.RecipientType;
import com.cambae.backend.m13.internal.channel.persistence.ChannelStore;
import com.cambae.backend.m13.internal.delivery.domain.ChannelSelection;
import com.cambae.backend.m13.internal.delivery.domain.ChannelSelection.Planned;
import com.cambae.backend.m13.internal.delivery.domain.ChannelSelection.Recipient;
import com.cambae.backend.m13.internal.delivery.domain.SendResult;
import com.cambae.backend.m13.internal.delivery.domain.SmsBatching;
import com.cambae.backend.m13.internal.delivery.persistence.DeliveryStore;
import com.cambae.backend.m13.internal.delivery.persistence.SmsWindowStore;
import com.cambae.backend.m13.internal.delivery.persistence.SmsWindowStore.Window;
import com.cambae.backend.m13.internal.persistence.NotificationStore;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * Lên lịch các lần gửi của một thông báo vừa gửi đi (lúc tiếp nhận hoặc lúc giải phóng giờ yên tĩnh; FR-016 → FR-020,
 * research B-05): theo {@link ChannelSelection}; lần gửi biết trước kết quả ghi ngay; tin nhắn Trung bình dồn vào khung
 * gộp đang mở, không có khung thì gửi ngay và mở khung CFG-M13-03 (FR-016b). Trả các lần gửi cần phát sau commit.
 *
 * <p>Chưa có {@code CaTrucApi} của 008 nên mọi nhân viên coi như đang trong ca (FR-016a chờ E).
 */
@Service
public class OutOfAppPlanner {

    private final NotificationStore notifications;
    private final DeliveryStore deliveries;
    private final ChannelStore channels;
    private final SmsWindowStore windows;
    private final SmsWindowCloser closer;
    private final ParameterApi parameters;

    OutOfAppPlanner(
            NotificationStore notifications,
            DeliveryStore deliveries,
            ChannelStore channels,
            SmsWindowStore windows,
            SmsWindowCloser closer,
            ParameterApi parameters) {
        this.notifications = notifications;
        this.deliveries = deliveries;
        this.channels = channels;
        this.windows = windows;
        this.closer = closer;
        this.parameters = parameters;
    }

    /** Người nhận của một thông báo, đủ để chọn kênh. */
    public record Target(
            long notificationId, RecipientType type, long recipientId, boolean hasAccount, Optional<String> phone) {

        String key() {
            return type.name() + ":" + recipientId;
        }
    }

    public List<Long> plan(Target t, NotificationLevel level, boolean invalidOutOfAppContent, Instant at) {
        List<Long> dispatch = new ArrayList<>();
        Recipient r = new Recipient(t.type(), t.hasAccount(), t.phone(), true);
        for (Planned p : ChannelSelection.plan(level, r, invalidOutOfAppContent, channels.channels())) {
            String address =
                    ChannelSelection.SMS.equals(p.channel()) ? t.phone().orElse(null) : null;
            if (p.immediate().isPresent()) {
                long attempt = notifications.planFirstAttempt(t.notificationId(), p.channel(), address, at);
                deliveries.insertResult(attempt, SendResult.of(p.immediate().get()), at);
                continue;
            }
            if (ChannelSelection.SMS.equals(p.channel()) && level == NotificationLevel.MEDIUM) {
                batchedOrNow(t, address, at).ifPresent(dispatch::add);
                continue;
            }
            dispatch.add(notifications.planFirstAttempt(t.notificationId(), p.channel(), address, at));
        }
        notifications.recompute(t.notificationId());
        return dispatch;
    }

    /** Tin nhắn Trung bình: dồn vào khung đang mở (rỗng), hoặc gửi ngay và mở khung (id lần gửi). */
    private Optional<Long> batchedOrNow(Target t, String address, Instant at) {
        Optional<Window> open = windows.openFor(t.key());
        if (open.isPresent() && !SmsBatching.batch(open.map(Window::endsAt), at)) {
            closer.close(open.get().id(), at);
            open = windows.openFor(t.key());
        }
        if (open.isPresent()) {
            notifications.planFirstAttempt(
                    t.notificationId(),
                    ChannelSelection.SMS,
                    address,
                    at,
                    open.get().id());
            return Optional.empty();
        }
        long attempt = notifications.planFirstAttempt(t.notificationId(), ChannelSelection.SMS, address, at);
        try {
            windows.open(t.key(), at, at.plus(windowLength()), attempt);
        } catch (DuplicateKeyException e) {
            // Lượt tiếp nhận khác vừa mở khung cho người này: tin này vẫn gửi ngay, khung kia giữ nguyên
        }
        return Optional.of(attempt);
    }

    private java.time.Duration windowLength() {
        return parameters.read(ParameterCode.CFG_M13_03).asDuration();
    }
}
