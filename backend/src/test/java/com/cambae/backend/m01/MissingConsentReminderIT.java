package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.time.TestClockControl;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.api.events.MissingConsentReminder;
import com.cambae.backend.m01.internal.consent.service.MissingConsentReminderService;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;

/**
 * 001 T054 (FR-012, SC-013; đồng hồ giả lập): khi Hoàn tất tiếp nhận mà thiếu bản đồng ý Hiệu lực thì nhắc Hành chính
 * mỗi CFG-M01-06 (mặc định 1 ngày) kể từ lúc bắt đầu, cho tới khi có bản Hiệu lực hoặc hồ sơ sang trạng thái cuối.
 * Lần Hoàn tất tiếp nhận đã cảnh báo người thực hiện nên lần nhắc đầu là sau một chu kỳ. Hồ sơ không qua Hoàn tất tiếp
 * nhận (không có dòng nhắc) không bị nhắc.
 */
class MissingConsentReminderIT extends ConsentTestBase {

    private static final Duration PERIOD = Duration.ofDays(1);

    @Autowired
    TestClockControl clock;

    @Autowired
    MissingConsentReminderService reminders;

    @Autowired
    ApplicationEvents events;

    @BeforeEach
    @AfterEach
    void realClock() {
        clock.reset();
    }

    private long reminderCount(long residentId) {
        return events.stream(MissingConsentReminder.class)
                .filter(e -> e.residentId() == residentId)
                .count();
    }

    @Test
    void FR_012_remindsEveryPeriodAfterStart_numberingTheReminders() {
        long id = newResident();
        reminders.startReminding(id);

        reminders.run();
        assertThat(reminderCount(id)).as("chưa tới một chu kỳ").isZero();

        clock.advanceBy(PERIOD.minusMinutes(1));
        reminders.run();
        assertThat(reminderCount(id)).isZero();

        clock.advanceBy(Duration.ofMinutes(2));
        reminders.run();
        reminders.run();
        assertThat(reminderCount(id)).as("chạy lại không nhắc thêm").isEqualTo(1);

        clock.advanceBy(PERIOD.minusMinutes(5));
        reminders.run();
        assertThat(reminderCount(id)).isEqualTo(1);

        clock.advanceBy(Duration.ofMinutes(10));
        reminders.run();
        assertThat(events.stream(MissingConsentReminder.class)
                        .filter(e -> e.residentId() == id)
                        .map(MissingConsentReminder::reminderNumber)
                        .toList())
                .containsExactly(1, 2);
    }

    @Test
    void FR_012_stopsOnceAnActiveConsentExists() {
        long id = newResident();
        reminders.startReminding(id);
        clock.advanceBy(PERIOD.plusMinutes(1));
        reminders.run();
        assertThat(reminderCount(id)).isEqualTo(1);

        record(id, byResident(Set.of()));
        clock.advanceBy(PERIOD.plusMinutes(1));
        reminders.run();

        assertThat(reminderCount(id)).isEqualTo(1);
    }

    @Test
    void FR_012_stopsWhenTheProfileReachesAFinalStatus() {
        long id = newResident();
        reminders.startReminding(id);
        clock.advanceBy(PERIOD.plusMinutes(1));
        reminders.run();

        setStatus(id, ResidentStatus.RESIDENCY_ENDED.name());
        clock.advanceBy(PERIOD.plusMinutes(1));
        reminders.run();

        assertThat(reminderCount(id)).isEqualTo(1);
    }

    @Test
    void FR_012_remindsForTheFourOpenStatuses_notForAdmissionInProgress() {
        for (ResidentStatus status : new ResidentStatus[] {
            ResidentStatus.IN_RESIDENCE,
            ResidentStatus.TEMPORARILY_ABSENT,
            ResidentStatus.OFF_SITE_ACTIVITY,
            ResidentStatus.HOSPITALIZED
        }) {
            long id = newResident();
            setStatus(id, status.name());
            reminders.startReminding(id);
            clock.advanceBy(PERIOD.plusMinutes(1));
            reminders.run();
            assertThat(reminderCount(id)).as(status.name()).isEqualTo(1);
        }
        long admission = newResident();
        setStatus(admission, ResidentStatus.ADMISSION_IN_PROGRESS.name());
        reminders.startReminding(admission);
        clock.advanceBy(PERIOD.plusMinutes(1));
        reminders.run();
        assertThat(reminderCount(admission)).isZero();
    }

    @Test
    void FR_012_residentNeverCompletedAdmission_isNotReminded() {
        long id = newResident();

        clock.advanceBy(PERIOD.plusMinutes(1));
        reminders.run();

        assertThat(reminderCount(id)).isZero();
    }

    @Test
    void FR_012_startingTwiceKeepsOneReminderRow() {
        long id = newResident();

        reminders.startReminding(id);
        reminders.startReminding(id);

        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM periodic_reminder WHERE type = 'MISSING_CONSENT' AND resident_id = ?",
                        Integer.class,
                        id))
                .isEqualTo(1);
    }
}
