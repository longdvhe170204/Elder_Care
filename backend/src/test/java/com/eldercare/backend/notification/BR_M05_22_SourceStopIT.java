package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.incident.api.events.CallReminderStopRequested;
import com.eldercare.backend.incident.api.events.WishReconfirmationCompleted;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * 009 US1 (T017; research B-08; module-interfaces 009 mục 2): 007 yêu cầu dừng lượt nhắc gọi. Sự cố hạ mức khỏi Khẩn cấp
 * ({@code CallReminderStopRequested}, BR-M05-22) và đã có kết quả xác nhận lại nguyện vọng ({@code
 * WishReconfirmationCompleted}, 007 FR-050b d) dừng lượt Chờ kết quả mà mọi thông báo gộp vào đều của nguồn đó, báo người
 * được nhắc không cần gọi, không tạo lượt mới cho chuỗi; lượt còn thông báo của nguồn khác giữ nguyên.
 */
class BR_M05_22_SourceStopIT extends CallReminderTestBase {

    @Test
    void BR_M05_22_incidentDowngraded_stopsReminder_remindedToldNoNeedToCall() {
        TestUser lead = users.staff(Role.NURSING_STAFF);
        TestUser late = users.staff(Role.NURSING_STAFF);
        duties.shiftLead(floor, lead.staffId());
        long incident = TestUsers.nextId();
        send(urgent(
                new SourceRecord("M05.INCIDENT", Long.toString(incident)),
                "ha-muc-" + incident,
                new RecipientGroup.Staff(late.staffId(), "X")));
        advance(PAST_CFG);
        creation.run();
        long id = reminders("STAFF", late.staffId()).getFirst();

        tx.executeWithoutResult(s -> events.publishEvent(new CallReminderStopRequested("M05.INCIDENT", incident)));

        await().atMost(Duration.ofSeconds(10))
                .until(() -> "STOPPED".equals(state(id).get("status")));
        assertThat(state(id)).containsEntry("stop_reason", "SOURCE_DOWNGRADED");
        awaitReminderNotices(id, lead.staffId(), "LOW", 1);
        assertThat(reminders("STAFF", late.staffId())).containsExactly(id);
    }

    @Test
    void FR_050b_wishReconfirmed_stopsReminderOfThatCriticalAlert() {
        TestUser lead = users.staff(Role.NURSING_STAFF);
        TestUser late = users.staff(Role.NURSING_STAFF);
        duties.shiftLead(floor, lead.staffId());
        long alert = TestUsers.nextId();
        send(urgent(
                new SourceRecord("M05.ALERT", Long.toString(alert)),
                "nguy-kich-" + alert,
                new RecipientGroup.Staff(late.staffId(), "X")));
        advance(PAST_CFG);
        creation.run();
        long id = reminders("STAFF", late.staffId()).getFirst();

        tx.executeWithoutResult(s -> events.publishEvent(new WishReconfirmationCompleted(alert)));

        await().atMost(Duration.ofSeconds(10))
                .until(() -> "STOPPED".equals(state(id).get("status")));
        assertThat(state(id)).containsEntry("stop_reason", "WISH_RECONFIRMED");
        awaitReminderNotices(id, lead.staffId(), "LOW", 1);
    }

    @Test
    void BR_M05_22_reminderAlsoHoldsAnotherSource_keepsAwaiting() {
        TestUser lead = users.staff(Role.NURSING_STAFF);
        TestUser late = users.staff(Role.NURSING_STAFF);
        duties.shiftLead(floor, lead.staffId());
        long incident = TestUsers.nextId();
        long other = TestUsers.nextId();
        send(urgent(
                new SourceRecord("M05.INCIDENT", Long.toString(incident)),
                "gop-ha-muc-" + incident,
                new RecipientGroup.Staff(late.staffId(), "X")));
        advance(PAST_CFG);
        creation.run();
        long id = reminders("STAFF", late.staffId()).getFirst();
        send(urgent(
                new SourceRecord("M05.INCIDENT", Long.toString(other)),
                "gop-khac-" + other,
                new RecipientGroup.Staff(late.staffId(), "X")));
        advance(PAST_CFG);
        creation.run();
        assertThat(reminders("STAFF", late.staffId())).containsExactly(id);

        tx.executeWithoutResult(s -> events.publishEvent(new CallReminderStopRequested("M05.INCIDENT", incident)));

        await().during(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(5))
                .until(() -> "AWAITING_RESULT".equals(state(id).get("status")));
    }
}
