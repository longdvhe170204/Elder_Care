package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.m01.api.dto.Gender;
import com.cambae.backend.m02.api.events.M02Reminder;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.ApplicationEvents;

/**
 * 004 T022, T027 (US2; FR-017, BR-M02-03, CFG-M02-03): hồ sơ Đang chờ không cập nhật quá 30 ngày thì Hành chính được nhắc
 * xác nhận gia đình còn nhu cầu, mỗi ngày một lần; lệnh "Xác nhận còn nhu cầu" (hay sửa tình huống) đặt lại mốc; hệ thống
 * không tự hủy hồ sơ vì quá hạn.
 */
@AutoConfigureMockMvc
@WithMockUser
class BR_M02_03_WaitingEntryReminderIT extends WaitingListTestBase {

    private static final String TYPE = "WAITING_ENTRY_UPDATE";

    @Autowired
    ApplicationEvents applicationEvents;

    private List<M02Reminder> remindersOf(long entryId) {
        return applicationEvents.stream(M02Reminder.class)
                .filter(reminder -> TYPE.equals(reminder.type()) && reminder.subjectId() == entryId)
                .toList();
    }

    /** Hồ sơ chờ có mốc cập nhật cách đây {@code days} ngày kể từ ngày đồng hồ giả lập (12:00). */
    private long staleEntry(LocalDate day, int days) throws Exception {
        noonOf(day);
        long id = newEntry(Gender.MALE, day, "BASIC").get("id").asLong();
        jdbc.update(
                "UPDATE waiting_list_entry SET last_confirmed_at = ? WHERE id = ?",
                LocalDateTime.ofInstant(time.now().minus(Duration.ofDays(days)).minusSeconds(60), ZoneOffset.UTC),
                id);
        return id;
    }

    @Test
    void BR_M02_03_overThirtyDays_remindsOncePerDay() throws Exception {
        LocalDate day = today().plusDays(40);
        long id = staleEntry(day, 31);

        dailyRunner.catchUp();
        assertThat(remindersOf(id)).hasSize(1);
        assertThat(remindersOf(id).get(0).on()).isEqualTo(day);

        dailyRunner.catchUp();
        assertThat(remindersOf(id)).as("cùng ngày không nhắc lần hai").hasSize(1);

        noonOf(day.plusDays(1));
        dailyRunner.catchUp();
        assertThat(remindersOf(id)).hasSize(2);
        assertThat(entry(id).get("status").asString())
                .as("không tự hủy vì quá hạn")
                .isEqualTo("WAITING");
    }

    @Test
    void BR_M02_03_withinThirtyDays_doesNotRemind() throws Exception {
        long id = staleEntry(today().plusDays(41), 29);

        dailyRunner.catchUp();

        assertThat(remindersOf(id)).isEmpty();
    }

    @Test
    void BR_M02_03_confirmStillNeeded_restartsTheThirtyDays() throws Exception {
        LocalDate day = today().plusDays(42);
        long id = staleEntry(day, 45);
        user.set(office.user());
        mvc.perform(post("/api/v1/waiting-list-entries/" + id + "/confirm-still-needed"))
                .andExpect(status().isOk());

        dailyRunner.catchUp();

        assertThat(remindersOf(id)).isEmpty();
    }

    @Test
    void BR_M02_03_contactedEntryIsNotReminded_sinceItIsAlreadyBeingHandled() throws Exception {
        long id = staleEntry(today().plusDays(43), 60);
        jdbc.update("UPDATE waiting_list_entry SET status = 'CONTACTED', held_bed_id = ? WHERE id = ?", 777_003L, id);

        dailyRunner.catchUp();

        assertThat(remindersOf(id)).isEmpty();
    }
}
