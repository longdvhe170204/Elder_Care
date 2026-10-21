package com.cambae.backend.m13;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m13.api.dto.AcknowledgementMethod;
import com.cambae.backend.m13.api.dto.NotificationStatus;
import com.cambae.backend.m13.internal.domain.DeliveryOutcome;
import com.cambae.backend.m13.internal.domain.NotificationEventType;
import com.cambae.backend.m13.internal.domain.NotificationStatusRules;
import com.cambae.backend.m13.internal.domain.NotificationStatusRules.AttemptFact;
import com.cambae.backend.m13.internal.domain.NotificationStatusRules.EventFact;
import com.cambae.backend.m13.internal.domain.NotificationStatusRules.State;
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
    void FR_044_notSent_whenReleasedWithoutConditions() {
        State s = NotificationStatusRules.of(
                List.of(hold(0, T0.plusSeconds(60)), ev(NotificationEventType.NOT_SENT, 1)), List.of());

        assertThat(s.status()).contains(NotificationStatus.NOT_SENT);
    }

    @Test
    void FR_044_sendWithPendingAttempts_hasNoStatusYet() {
        State s = NotificationStatusRules.of(List.of(ev(NotificationEventType.SEND, 0)), List.of(PENDING));

        assertThat(s.status()).isEmpty();
        assertThat(s.sentAt()).contains(T0);
    }

    @Test
    void FR_044_sent_whenAnyChannelSent() {
        State s = NotificationStatusRules.of(
                List.of(ev(NotificationEventType.SEND, 0)),
                List.of(done(DeliveryOutcome.FAILED), done(DeliveryOutcome.SENT)));

        assertThat(s.status()).contains(NotificationStatus.SENT);
    }

    @Test
    void FR_044_deliveryFailed_whenEveryChannelDoneWithoutSent() {
        State s = NotificationStatusRules.of(
                List.of(ev(NotificationEventType.SEND, 0)),
                List.of(done(DeliveryOutcome.FAILED), done(DeliveryOutcome.CHANNEL_NOT_CONFIGURED)));

        assertThat(s.status()).contains(NotificationStatus.DELIVERY_FAILED);
    }

    @Test
    void FR_044_failedSoFarButOneChannelPending_hasNoStatusYet() {
        State s = NotificationStatusRules.of(
                List.of(ev(NotificationEventType.SEND, 0)), List.of(done(DeliveryOutcome.FAILED), PENDING));

        assertThat(s.status()).isEmpty();
    }

    @Test
    void FR_044_viewed_afterSent() {
        State s = NotificationStatusRules.of(
                List.of(ev(NotificationEventType.SEND, 0), ev(NotificationEventType.VIEW, 5)),
                List.of(done(DeliveryOutcome.SENT)));

        assertThat(s.status()).contains(NotificationStatus.VIEWED);
        assertThat(s.viewedAt()).contains(T0.plusSeconds(300));
    }

    @Test
    void FR_044_acknowledged_fromSentOrViewed() {
        State s = NotificationStatusRules.of(
                List.of(
                        ev(NotificationEventType.SEND, 0),
                        ev(NotificationEventType.VIEW, 1),
                        ack(AcknowledgementMethod.IN_APP, 2)),
                List.of(done(DeliveryOutcome.SENT)));

        assertThat(s.status()).contains(NotificationStatus.ACKNOWLEDGED);
        assertThat(s.acknowledgedAt()).contains(T0.plusSeconds(120));
        assertThat(s.acknowledgementMethod()).contains(AcknowledgementMethod.IN_APP);
    }

    @Test
    void FR_044_acknowledgedByCall_evenWhenDeliveryFailed() {
        State s = NotificationStatusRules.of(
                List.of(ev(NotificationEventType.SEND, 0), ack(AcknowledgementMethod.BY_CALL, 10)),
                List.of(done(DeliveryOutcome.FAILED)));

        assertThat(s.status()).contains(NotificationStatus.ACKNOWLEDGED);
    }

    @Test
    void FR_044_unreachable_overridesViewedAndFailed() {
        State viewed = NotificationStatusRules.of(
                List.of(
                        ev(NotificationEventType.SEND, 0),
                        ev(NotificationEventType.VIEW, 1),
                        ev(NotificationEventType.UNREACHABLE, 20)),
                List.of(done(DeliveryOutcome.SENT)));
        State failed = NotificationStatusRules.of(
                List.of(ev(NotificationEventType.SEND, 0), ev(NotificationEventType.UNREACHABLE, 20)),
                List.of(done(DeliveryOutcome.FAILED)));

        assertThat(viewed.status()).contains(NotificationStatus.UNREACHABLE);
        assertThat(failed.status()).contains(NotificationStatus.UNREACHABLE);
    }

    @Test
    void FR_044_lateAcknowledgement_afterUnreachable() {
        State s = NotificationStatusRules.of(
                List.of(
                        ev(NotificationEventType.SEND, 0),
                        ev(NotificationEventType.UNREACHABLE, 20),
                        ack(AcknowledgementMethod.LATE, 30)),
                List.of(done(DeliveryOutcome.SENT)));

        assertThat(s.status()).contains(NotificationStatus.ACKNOWLEDGED);
        assertThat(s.acknowledgementMethod()).contains(AcknowledgementMethod.LATE);
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

    @Test
    void FR_044_releasedFromHold_followsDelivery() {
        State s = NotificationStatusRules.of(
                List.of(hold(0, T0.plusSeconds(60)), ev(NotificationEventType.SEND, 1)),
                List.of(done(DeliveryOutcome.SENT)));

        assertThat(s.status()).contains(NotificationStatus.SENT);
        assertThat(s.sentAt()).contains(T0.plusSeconds(60));
    }
}
