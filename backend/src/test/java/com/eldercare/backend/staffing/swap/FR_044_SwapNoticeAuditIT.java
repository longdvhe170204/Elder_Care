package com.eldercare.backend.staffing.swap;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.staffing.api.events.SwapNotification;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/** FR-044 (mức, người nhận, không nêu lý do), FR-025 (nhắc một lần), FR-046 (nhật ký, trạng thái cuối không sửa). */
@RecordApplicationEvents
class FR_044_SwapNoticeAuditIT extends SwapTestBase {

    @Autowired
    ApplicationEvents events;

    @Autowired
    com.eldercare.backend.staffing.scheduling.M09MinuteJob minuteJob;

    private List<NotificationRequest> notices() {
        return events.stream(SwapNotification.class)
                .flatMap(e -> e.notificationRequests().stream())
                .toList();
    }

    @Test
    void FR_044_newRequestNotifiesRecipientAtLowLevelWithoutTheReason() throws Exception {
        long x = shift(base.plus(Duration.ofHours(7)), 8, null, giver);
        long id = createTakeOver(giver, x, recipient);
        List<NotificationRequest> sent = notices().stream()
                .filter(n -> n.eventKey().startsWith("SWAP_REQUESTED:" + id))
                .toList();
        assertThat(sent).hasSize(1);
        NotificationRequest n = sent.get(0);
        assertThat(n.level()).contains(NotificationLevel.LOW);
        assertThat(n.recipientGroups()).contains(new RecipientGroup.Staff(recipient.staffId(), "SWAP_REQUESTED"));
        assertThat(n.content().toString()).doesNotContain("riêng tư");
        assertThat(n.shortContent().orElse("")).doesNotContain("riêng tư");
    }
}
