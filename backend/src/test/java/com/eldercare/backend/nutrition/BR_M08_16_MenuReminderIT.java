package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.nutrition.api.events.MenuPublicationReminder;
import com.eldercare.backend.nutrition.internal.schedule.service.M08JobService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 011 T017 (US3, BR-M08-16, FR-027, FR-030; quickstart Q17, Q18): 00:00 thứ Hai thực đơn Công bố → Đã áp dụng; tại
 * CFG-M08-07 (2 ngày trước tuần) tuần kế tiếp chưa có thực đơn Công bố thì nhắc bếp, Quản lý viện và Bác sĩ khi đang Chờ
 * công bố; chạy lại không nhắc trùng.
 */
class BR_M08_16_MenuReminderIT extends MenuTestBase {

    @Autowired
    M08JobService jobs;

    @Test
    void FR_030_publishedMenu_becomesAppliedWhenWeekStarts() throws Exception {
        LocalDate week = futureWeek();
        long menu = publishedMenu(week, plain);

        jobs.runDailyTasks(week.minusDays(1), time.now());
        assertThat(menuStatus(menu)).isEqualTo("PUBLISHED");
        jobs.runDailyTasks(week, time.now());
        assertThat(menuStatus(menu)).isEqualTo("APPLIED");
        jobs.runDailyTasks(week.plusDays(1), time.now());
        assertThat(menuStatus(menu)).isEqualTo("APPLIED");
    }

    @Test
    void BR_M08_16_noPublishedMenuTwoDaysBefore_remindsOnce() throws Exception {
        LocalDate week = futureWeek();

        jobs.runDailyTasks(week.minusDays(3), time.now());
        assertThat(reminders(week)).isEmpty();
        jobs.runDailyTasks(week.minusDays(2), time.now());
        jobs.runDailyTasks(week.minusDays(2), time.now());
        jobs.runDailyTasks(week.minusDays(1), time.now());

        assertThat(reminders(week)).singleElement().satisfies(reminder -> {
            assertThat(reminder.awaitingPublication()).isFalse();
            assertThat(reminder.notificationRequests())
                    .flatExtracting(r -> r.recipientGroups())
                    .contains(new RecipientGroup.FacilityRole(Role.KITCHEN_STAFF))
                    .contains(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER))
                    .doesNotContain(new RecipientGroup.FacilityRole(Role.DOCTOR));
        });
    }

    @Test
    void BR_M08_16_pendingMenu_alsoRemindsDoctor_publishedMenu_noReminder() throws Exception {
        LocalDate pendingWeek = futureWeek();
        long menu = createMenu(pendingWeek);
        putContent(menu, fullWeek(pendingWeek, defaultDiet, plain)).andExpect(status().isOk());
        submit(menu, codes(menu)).andExpect(status().isOk());
        LocalDate publishedWeek = futureWeek();
        publishedMenu(publishedWeek, plain);

        jobs.runDailyTasks(pendingWeek.minusDays(2), time.now());
        jobs.runDailyTasks(publishedWeek.minusDays(2), time.now());

        assertThat(reminders(pendingWeek)).singleElement().satisfies(reminder -> {
            assertThat(reminder.awaitingPublication()).isTrue();
            assertThat(reminder.notificationRequests())
                    .flatExtracting(r -> r.recipientGroups())
                    .contains(new RecipientGroup.FacilityRole(Role.DOCTOR));
        });
        assertThat(reminders(publishedWeek)).isEmpty();
    }

    private List<MenuPublicationReminder> reminders(LocalDate week) {
        return events.stream(MenuPublicationReminder.class)
                .filter(r -> r.weekStart().equals(week))
                .toList();
    }
}
