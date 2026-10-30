package com.eldercare.backend.notification.internal.domain;

import com.eldercare.backend.notification.api.dto.AcknowledgementMethod;
import com.eldercare.backend.notification.api.dto.NotificationStatus;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Trạng thái thông báo theo bảng FR-044 ({@code TrangThaiThongBao}; research B-09, data-model mục 4): hàm thuần trên
 * sự kiện ghi thêm và kết quả lần gửi. Cột trạng thái của {@code notification} chỉ lưu kết quả của hàm này để truy vấn nhanh.
 */
public final class NotificationStatusRules {

    /** Một sự kiện của thông báo. */
    public record EventFact(
            NotificationEventType type,
            Optional<AcknowledgementMethod> method,
            Optional<Instant> scheduledSendAt,
            Instant at) {}

    /** Một lần gửi; kết quả rỗng là chưa gửi xong. */
    public record AttemptFact(Optional<DeliveryOutcome> outcome) {}

    /** Trạng thái hiện hành; {@code status} rỗng khi đã gửi đi mà chưa kênh nào có kết quả quyết định. */
    public record State(
            Optional<NotificationStatus> status,
            Optional<Instant> scheduledSendAt,
            Optional<Instant> sentAt,
            Optional<Instant> viewedAt,
            Optional<Instant> acknowledgedAt,
            Optional<AcknowledgementMethod> acknowledgementMethod,
            boolean hidden) {}

    private NotificationStatusRules() {}

    public static State of(List<EventFact> events, List<AttemptFact> attempts) {
        List<EventFact> ordered =
                events.stream().sorted(Comparator.comparing(EventFact::at)).toList();
        Optional<EventFact> ack = first(ordered, NotificationEventType.ACKNOWLEDGE);
        Optional<EventFact> send = first(ordered, NotificationEventType.SEND);
        Optional<EventFact> view = first(ordered, NotificationEventType.VIEW);
        Optional<Instant> scheduled = ordered.stream()
                .filter(e -> e.type() == NotificationEventType.HOLD || e.type() == NotificationEventType.RESCHEDULE)
                .reduce((a, b) -> b)
                .flatMap(EventFact::scheduledSendAt);
        boolean hidden = has(ordered, NotificationEventType.HIDE);

        return new State(
                status(ordered, send.isPresent(), view.isPresent(), ack.isPresent(), attempts),
                scheduled,
                send.map(EventFact::at),
                view.map(EventFact::at),
                ack.map(EventFact::at),
                ack.flatMap(EventFact::method),
                hidden);
    }

    private static Optional<NotificationStatus> status(
            List<EventFact> events, boolean sent, boolean viewed, boolean acknowledged, List<AttemptFact> attempts) {
        if (acknowledged) {
            return Optional.of(NotificationStatus.ACKNOWLEDGED);
        }
        if (has(events, NotificationEventType.UNREACHABLE)) {
            return Optional.of(NotificationStatus.UNREACHABLE);
        }
        if (has(events, NotificationEventType.NOT_SENT)) {
            return Optional.of(NotificationStatus.NOT_SENT);
        }
        if (!sent) {
            return has(events, NotificationEventType.HOLD)
                    ? Optional.of(NotificationStatus.HELD_FOR_QUIET_HOURS)
                    : Optional.empty();
        }
        if (viewed) {
            return Optional.of(NotificationStatus.VIEWED);
        }
        if (attempts.stream()
                .anyMatch(a -> a.outcome().filter(DeliveryOutcome::delivered).isPresent())) {
            return Optional.of(NotificationStatus.SENT);
        }
        if (!attempts.isEmpty() && attempts.stream().allMatch(a -> a.outcome().isPresent())) {
            return Optional.of(NotificationStatus.DELIVERY_FAILED);
        }
        return Optional.empty();
    }

    private static Optional<EventFact> first(List<EventFact> events, NotificationEventType type) {
        return events.stream().filter(e -> e.type() == type).findFirst();
    }

    private static boolean has(List<EventFact> events, NotificationEventType type) {
        return events.stream().anyMatch(e -> e.type() == type);
    }
}
