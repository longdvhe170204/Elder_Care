package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.notification.api.dto.AcknowledgementMethod;
import com.eldercare.backend.notification.api.dto.NotificationStatus;
import com.eldercare.backend.notification.internal.domain.DeliveryOutcome;
import com.eldercare.backend.notification.internal.domain.NotificationEventType;
import com.eldercare.backend.notification.internal.domain.NotificationStatusRules;
import com.eldercare.backend.notification.internal.domain.NotificationStatusRules.AttemptFact;
import com.eldercare.backend.notification.internal.domain.NotificationStatusRules.EventFact;
import com.eldercare.backend.notification.internal.domain.NotificationStatusRules.State;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * FR-044, research B-09 (009 T009): trạng thái thông báo là hàm thuần trên sự kiện ghi thêm và kết quả lần gửi, khớp
 * bảng chuyển data-model mục 4.
 */
class NotificationStatusRulesTest {

    private static final Instant T0 = Instant.parse("2026-10-21T01:00:00Z");

    private static EventFact ev(NotificationEventType type, int minute) {
        return new EventFact(type, Optional.empty(), Optional.empty(), T0.plusSeconds(60L * minute));
    }

    private static EventFact hold(int minute, Instant until) {
        return new EventFact(
                NotificationEventType.HOLD, Optional.empty(), Optional.of(until), T0.plusSeconds(60L * minute));
    }

    private static EventFact ack(AcknowledgementMethod m, int minute) {
        return new EventFact(
                NotificationEventType.ACKNOWLEDGE, Optional.of(m), Optional.empty(), T0.plusSeconds(60L * minute));
    }

    private static AttemptFact done(DeliveryOutcome o) {
        return new AttemptFact(Optional.of(o));
    }

    private static final AttemptFact PENDING = new AttemptFact(Optional.empty());

    @Test
    void FR_044_heldForQuietHours_untilReleased() {
        Instant until = T0.plusSeconds(3600);
        State s = NotificationStatusRules.of(List.of(hold(0, until)), List.of());

        assertThat(s.status()).contains(NotificationStatus.HELD_FOR_QUIET_HOURS);
        assertThat(s.scheduledSendAt()).contains(until);
    }

    @Test
    void FR_044_rescheduleMovesScheduledTime() {
        Instant later = T0.plusSeconds(7200);
        EventFact resched = new EventFact(
                NotificationEventType.RESCHEDULE, Optional.empty(), Optional.of(later), T0.plusSeconds(60));
        State s = NotificationStatusRules.of(List.of(hold(0, T0.plusSeconds(3600)), resched), List.of());

        assertThat(s.scheduledSendAt()).contains(later);
    }

    @Test
    void FR_036_hideKeepsStatus_andSetsHidden() {
        State s = NotificationStatusRules.of(
                List.of(
                        ev(NotificationEventType.SEND, 0),
                        ev(NotificationEventType.VIEW, 1),
                        ev(NotificationEventType.HIDE, 2)),
                List.of(done(DeliveryOutcome.SENT)));

        assertThat(s.status()).contains(NotificationStatus.VIEWED);
        assertThat(s.hidden()).isTrue();
    }
}
