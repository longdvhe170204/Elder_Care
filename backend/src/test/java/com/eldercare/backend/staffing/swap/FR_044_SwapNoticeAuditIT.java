package com.eldercare.backend.staffing.swap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.staffing.api.events.SwapNotification;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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

    @Test
    void FR_046_applyingASwapIsAuditedAndAFinalRequestCannotBeChangedAgain() throws Exception {
        long x = shift(base.plus(Duration.ofHours(7)), 8, null, giver);
        long id = createTakeOver(giver, x, recipient);
        send("/api/v1/shift-swaps/" + id + "/accept", recipient, null).andExpect(status().isOk());
        send("/api/v1/shift-swaps/" + id + "/approve", head, null).andExpect(status().isOk());
        Integer audited = jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE entity_type='M09.SHIFT_SWAP' AND entity_id=?",
                Integer.class,
                Long.toString(id));
        assertThat(audited).isGreaterThanOrEqualTo(1);
        // Trạng thái cuối: mọi lệnh sau đó bị chặn.
        send("/api/v1/shift-swaps/" + id + "/cancel", giver, Map.of("reason", "[Rút]"))
                .andExpect(status().isConflict());
        send("/api/v1/shift-swaps/" + id + "/accept", recipient, null).andExpect(status().isConflict());
        send("/api/v1/shift-swaps/" + id + "/reject", head, Map.of("reason", "[Từ chối]"))
                .andExpect(status().isConflict());
    }

    @Test
    void FR_025_reminderIsSentOnceWhenTheShiftIsWithinTheConfiguredWindow() throws Exception {
        Instant start = base.plus(Duration.ofHours(7));
        long x = shift(start, 8, null, giver);
        long id = createTakeOver(giver, x, recipient);
        // Còn khoảng 10 giờ tới ca (CFG-M09-11 mặc định 24 giờ); lệch vài phút để khỏi trùng tick của test khác.
        clock.advanceBy(
                Duration.between(time.now(), start.minus(Duration.ofHours(10)).minus(Duration.ofMinutes(3))));
        minuteJob.runNow();
        long reminders = notices().stream()
                .filter(n -> n.eventKey().startsWith("REQUEST_DUE_SOON:" + id))
                .count();
        assertThat(reminders).isEqualTo(1);
        assertThat(notices().stream()
                        .filter(n -> n.eventKey().startsWith("REQUEST_DUE_SOON:" + id))
                        .findFirst()
                        .orElseThrow()
                        .level())
                .contains(NotificationLevel.MEDIUM);
        clock.advanceBy(Duration.ofMinutes(5));
        minuteJob.runNow();
        assertThat(notices().stream()
                        .filter(n -> n.eventKey().startsWith("REQUEST_DUE_SOON:" + id))
                        .count())
                .isEqualTo(1);
    }
}
