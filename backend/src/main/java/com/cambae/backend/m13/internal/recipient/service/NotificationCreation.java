package com.cambae.backend.m13.internal.recipient.service;

import com.cambae.backend.m13.api.dto.RecipientType;
import com.cambae.backend.m13.internal.delivery.service.DeliveryDispatcher;
import com.cambae.backend.m13.internal.intake.domain.RequestFlag;
import com.cambae.backend.m13.internal.intake.domain.ValidatedRequest;
import com.cambae.backend.m13.internal.persistence.NotificationStore;
import com.cambae.backend.m13.internal.persistence.NotificationStore.NewNotification;
import com.cambae.backend.m13.internal.recipient.domain.ResolvedRecipient;
import com.cambae.backend.m13.internal.recipient.service.RecipientResolver.Resolution;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Tạo thông báo cho một yêu cầu hợp lệ trong transaction tiếp nhận (research B-03 bước 2): xác định người nhận, ghi một
 * thông báo mỗi người (FR-012), kết quả từng nhóm, dấu không có người nhận, thiếu mã trường hợp; lần gửi dự kiến trong ứng
 * dụng hoặc cổng người thân cho người có tài khoản, gửi sau commit. Chọn kênh ngoài ứng dụng, giờ yên tĩnh làm ở US3, US4.
 */
@Service
public class NotificationCreation {

    private final RecipientResolver resolver;
    private final NotificationStore notifications;
    private final DeliveryDispatcher dispatcher;

    NotificationCreation(RecipientResolver resolver, NotificationStore notifications, DeliveryDispatcher dispatcher) {
        this.resolver = resolver;
        this.notifications = notifications;
        this.dispatcher = dispatcher;
    }

    /** Xác định người nhận trước khi lưu yêu cầu: kết quả nhóm và dấu ghi cùng lần thêm yêu cầu (bảng chỉ cho thêm). */
    public Resolution resolve(ValidatedRequest v, Instant at) {
        return resolver.resolve(v, at);
    }

    /** Dấu của yêu cầu sau khi thêm dấu không có người nhận (FR-011), thiếu mã trường hợp (FR-001a). */
    public static ValidatedRequest withFlags(ValidatedRequest v, Resolution resolution) {
        Set<RequestFlag> flags = EnumSet.noneOf(RequestFlag.class);
        flags.addAll(v.flags());
        if (resolution.noRecipient()) {
            flags.add(RequestFlag.NO_RECIPIENT);
        }
        if (resolution.missingCase()) {
            flags.add(RequestFlag.MISSING_Q354_CASE);
        }
        return new ValidatedRequest(
                v.request(), v.result(), v.rejectionReason(), v.level(), v.content(), v.informationSubject(), flags);
    }

    /** Ghi một thông báo mỗi người nhận của yêu cầu đã lưu, lên lịch gửi sau commit. */
    public void create(long requestId, Resolution resolution, ValidatedRequest v, Instant at) {
        List<Long> attempts = new ArrayList<>();
        for (ResolvedRecipient p : resolution.recipients()) {
            long id = notifications.insert(new NewNotification(
                    requestId,
                    p.type(),
                    p.id(),
                    p.accountId(),
                    p.basis(),
                    p.visibleParts(),
                    p.identityOnly(),
                    v.level(),
                    at,
                    p.q354Case()));
            notifications.markSent(id, at);
            if (p.accountId() != null) {
                String channel = p.type() == RecipientType.FAMILY_MEMBER ? "FAMILY_PORTAL" : "IN_APP";
                attempts.add(notifications.planFirstAttempt(id, channel, null, at));
            }
        }
        dispatcher.dispatch(attempts);
    }
}
