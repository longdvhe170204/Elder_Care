package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.events.MissingEndOfLifeWishReminder;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;

/**
 * 001 T067 (FR-023d, FR-023f; US7 #4; đồng hồ giả lập): Hoàn tất tiếp nhận mà chưa có phiếu nguyện vọng Hiệu lực thì
 * nhắc mỗi CFG-M01-06 (mặc định 1 ngày) kể từ lúc bắt đầu, cho tới khi có phiếu Hiệu lực hoặc hồ sơ sang trạng thái
 * cuối. Người nhận (Bác sĩ, Điều dưỡng phụ trách) và mức do 009 xếp.
 */
class MissingEndOfLifeWishReminderIT extends WishTestBase {

    private static final Duration PERIOD = Duration.ofDays(1);

    @Autowired
    TestClockControl clock;

    @Autowired
    ApplicationEvents events;

    @BeforeEach
    @AfterEach
    void realClock() {
        clock.reset();
    }

    private long reminderCount(long residentId) {
        return events.stream(MissingEndOfLifeWishReminder.class)
                .filter(e -> e.residentId() == residentId)
                .count();
    }

    @Test
    void FR_023d_remindsEveryPeriodAfterStart_numberingTheReminders() {
        long id = newResident();
        wishReminders.startReminding(id);

        wishReminders.runMissing();
        assertThat(reminderCount(id)).as("chưa tới một chu kỳ").isZero();

        clock.advanceBy(PERIOD.plusMinutes(1));
        wishReminders.runMissing();
        wishReminders.runMissing();
        assertThat(reminderCount(id)).as("chạy lại không nhắc thêm").isEqualTo(1);

        clock.advanceBy(PERIOD.plusMinutes(1));
        wishReminders.runMissing();
        assertThat(events.stream(MissingEndOfLifeWishReminder.class)
                        .filter(e -> e.residentId() == id)
                        .map(MissingEndOfLifeWishReminder::reminderNumber)
                        .toList())
                .containsExactly(1, 2);
    }

    @Test
    void FR_023d_startingTwiceDoesNotAddASecondReminderRow() {
        long id = newResident();

        wishReminders.startReminding(id);
        wishReminders.startReminding(id);

        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM periodic_reminder WHERE type = 'MISSING_END_OF_LIFE_WISH'"
                                + " AND resident_id = ?",
                        Integer.class,
                        id))
                .isEqualTo(1);
    }

    @Test
    void FR_023d_stopsOnceAnActiveWishExists() {
        long id = newResident();
        setStatus(id, ResidentStatus.IN_RESIDENCE.name());
        wishReminders.startReminding(id);
        clock.advanceBy(PERIOD.plusMinutes(1));
        wishReminders.runMissing();
        assertThat(reminderCount(id)).isEqualTo(1);

        record(command(id).build());
        clock.advanceBy(PERIOD.plusMinutes(1));
        wishReminders.runMissing();

        assertThat(reminderCount(id)).isEqualTo(1);
    }

    @Test
    void FR_023f_stopsWhenTheProfileReachesAFinalStatus() {
        long id = newResident();
        wishReminders.startReminding(id);
        clock.advanceBy(PERIOD.plusMinutes(1));
        wishReminders.runMissing();

        setStatus(id, ResidentStatus.DECEASED.name());
        clock.advanceBy(PERIOD.plusMinutes(1));
        wishReminders.runMissing();

        assertThat(reminderCount(id)).isEqualTo(1);
    }

    @Test
    void FR_023d_residentNeverCompletedAdmission_isNotReminded() {
        long id = newResident();

        clock.advanceBy(PERIOD.plusMinutes(1));
        wishReminders.runMissing();

        assertThat(reminderCount(id)).isZero();
    }
}
