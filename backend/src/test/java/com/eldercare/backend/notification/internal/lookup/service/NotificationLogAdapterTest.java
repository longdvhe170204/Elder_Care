package com.eldercare.backend.notification.internal.lookup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.incident.api.dto.IncidentNotificationView;
import com.eldercare.backend.notification.api.NotificationLookupApi;
import com.eldercare.backend.notification.api.dto.NotificationStatus;
import com.eldercare.backend.notification.api.dto.NotifiedPersonView;
import com.eldercare.backend.notification.api.dto.NotifiedPersonView.CallOutcome;
import com.eldercare.backend.notification.api.dto.NotifiedPersonView.CallReminderSummary;
import com.eldercare.backend.notification.api.dto.NotifiedPersonView.ChannelResult;
import com.eldercare.backend.notification.api.dto.RecipientType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 009 cài {@code NotificationLogPort} của 007 (FR-040, FR-048b, Q-388; module-interfaces 007 mục 2.4): một dòng cho mỗi
 * người nhận trên mỗi kênh, giữ mã của 009, kèm đủ chuỗi nhắc gọi (cả lượt gọi người thân kế tiếp).
 */
class NotificationLogAdapterTest {

    private static final Instant SENT = Instant.parse("2026-11-07T01:00:00Z");
    private static final Instant VIEWED = Instant.parse("2026-11-07T01:05:00Z");

    private final NotificationLookupApi lookup = mock(NotificationLookupApi.class);
    private final NotificationLogAdapter adapter = new NotificationLogAdapter(lookup);

    private static NotifiedPersonView person(
            RecipientType type, long id, List<ChannelResult> channels, List<CallReminderSummary> reminders) {
        return new NotifiedPersonView(
                id * 10,
                type,
                id,
                List.of("PrimaryContact"),
                NotificationStatus.SENT,
                channels,
                Optional.of(SENT),
                Optional.of(VIEWED),
                Optional.empty(),
                Optional.empty(),
                reminders,
                false);
    }

    @Test
    void FR_040_oneRowPerRecipientAndChannel_keepsSentAndViewed() {
        when(lookup.bySource("M05.INCIDENT", "7"))
                .thenReturn(List.of(person(
                        RecipientType.FAMILY_MEMBER,
                        5,
                        List.of(
                                new ChannelResult("PORTAL", "SENT", SENT),
                                new ChannelResult("SMS", "FAILED", SENT),
                                new ChannelResult("SMS", "SENT", SENT.plusSeconds(60))),
                        List.of())));

        List<IncidentNotificationView> rows = adapter.bySource("M05.INCIDENT", 7);

        assertThat(rows).extracting(IncidentNotificationView::channel).containsExactly("PORTAL", "SMS");
        assertThat(rows).allSatisfy(r -> {
            assertThat(r.recipientType()).isEqualTo("FAMILY_MEMBER");
            assertThat(r.recipientId()).isEqualTo(5);
            assertThat(r.sentAt()).contains(SENT);
            assertThat(r.viewedAt()).contains(VIEWED);
        });
    }

    @Test
    void Q_388_callChainIncludesNextFamilyMemberAndResults() {
        Instant called = SENT.plusSeconds(600);
        when(lookup.bySource("M05.INCIDENT", "8"))
                .thenReturn(List.of(person(
                        RecipientType.FAMILY_MEMBER,
                        5,
                        List.of(),
                        List.of(
                                new CallReminderSummary(
                                        1,
                                        RecipientType.FAMILY_MEMBER,
                                        5,
                                        "DONE",
                                        List.of(new CallOutcome("NOT_REACHED", called))),
                                new CallReminderSummary(
                                        2,
                                        RecipientType.FAMILY_MEMBER,
                                        6,
                                        "DONE",
                                        List.of(new CallOutcome("REACHED", called)))))));

        IncidentNotificationView row = adapter.bySource("M05.INCIDENT", 8).getFirst();

        // Chưa có lần gửi nào qua kênh ngoài: vẫn là thông báo trong ứng dụng
        assertThat(row.channel()).isEqualTo("IN_APP");
        assertThat(row.callReminders())
                .extracting(IncidentNotificationView.CallReminderSummary::personToCallId)
                .containsExactly(5L, 6L);
        assertThat(row.callReminders().getLast().personToCallType()).isEqualTo("FAMILY_MEMBER");
        assertThat(row.callReminders().getLast().results())
                .containsExactly(new IncidentNotificationView.CallOutcome("REACHED", called));
    }
}
