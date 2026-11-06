package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.family.api.events.ResidentVisitReminder;
import com.eldercare.backend.family.internal.scheduling.service.M10TimedTask;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 012 T069 (US9; FR-069; research F-18): người cao tuổi có tài khoản được nhắc lượt thăm Đã duyệt trong ngày: việc nửa
 * đêm cho lượt đã lập trước, lượt lập trong ngày nhắc ngay; người cao tuổi không có tài khoản không được nhắc.
 */
class FR_069_ResidentVisitReminderIT extends VisitTestBase {

    @Test
    void FR_069_midnightReminderForResidentWithAccount() throws Exception {
        users.resident(a);
        long visitId = id(
                register(office, mMember, MORNING_START, MORNING_END, "Lê Bé").andExpect(status().isCreated()));
        assertThat(reminders(visitId)).isEmpty();

        Instant midnight = at(day, LocalTime.MIDNIGHT);
        moveTo(midnight);
        tasks.stream().filter(t -> t.name().equals("VISIT_RESIDENT_REMINDER")).forEach(t -> t.run(midnight, midnight));

        List<ResidentVisitReminder> sent = reminders(visitId);
        assertThat(sent).hasSize(1);
        assertThat(sent.getFirst().notificationRequests().getFirst().recipientGroups())
                .containsExactly(new RecipientGroup.Resident(a));
        assertThat(sent.getFirst().visitors()).containsExactly("Lê M", "Lê Bé");
        assertThat(tasks.stream().filter(t -> t.name().equals("VISIT_RESIDENT_REMINDER")))
                .allMatch(t -> t.cadence() == M10TimedTask.Cadence.MIDNIGHT);
    }

    @Test
    void FR_069_sameDayVisitRemindedOnRegistration() throws Exception {
        users.resident(a);
        moveTo(at(day, LocalTime.of(7, 0)));
        long visitId =
                id(register(office, mMember, AFTERNOON_START, AFTERNOON_END).andExpect(status().isCreated()));
        assertThat(reminders(visitId)).hasSize(1);
    }

    @Test
    void FR_069_noReminderWithoutAccount() throws Exception {
        moveTo(at(day, LocalTime.of(7, 0)));
        long visitId =
                id(register(office, mMember, AFTERNOON_START, AFTERNOON_END).andExpect(status().isCreated()));
        assertThat(reminders(visitId)).isEmpty();
    }

    private List<ResidentVisitReminder> reminders(long visitId) {
        return events.stream(ResidentVisitReminder.class)
                .filter(r -> r.visitId() == visitId)
                .toList();
    }
}
