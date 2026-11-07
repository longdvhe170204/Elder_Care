package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.internal.activity.service.ActivityScheduling;
import com.eldercare.backend.care.internal.scheduling.service.*;
import com.eldercare.backend.care.scheduling.CareDailyJob;
import com.eldercare.backend.common.scheduling.SchedulerRuns;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.env.MockEnvironment;

/**
 * 014 T005 (research L-15): việc hằng ngày của 014 chạy trong khung CARE_DAILY của 005 (ShedLock, chạy bù lúc khởi
 * động); mốc đã chạy theo (việc, ngày) ở {@code reminder_marker}, chạy lại không làm lại ngày đã xong.
 */
class ChayBuM04HoatDongIT extends MySqlIntegrationTestBase {
    @Autowired
    CareDailyWorkRunner runner;

    @Autowired
    SchedulerRuns runs;

    @Autowired
    List<CareDailyWork> dailyWork;

    private final Instant at = Instant.parse("2026-11-04T17:10:00Z"); // 00:10 ngày 05/11 giờ Việt Nam

    @Test
    void L_15_activityDailyStepsJoinTheSharedFrameWithActivityPrefixedNames() {
        assertThat(dailyWork)
                .filteredOn(w -> w.name().startsWith(ActivityScheduling.PREFIX))
                .allSatisfy(w -> assertThat(w.name()).matches("M04HD_[A-Z_]+"));
        assertThat(ActivityScheduling.dueDates(LocalDate.of(2026, 11, 5), Optional.empty()))
                .containsExactly(LocalDate.of(2026, 11, 4));
    }

    @Test
    void L_15_missedDaysRunOnceEachInDateOrderAfterRestart() {
        String name = ActivityScheduling.PREFIX + "TEST_CATCHUP_" + System.nanoTime() % 100000;
        var days = new ArrayList<LocalDate>();
        CareDailyWork step = new CareDailyWork() {
            public String name() {
                return name;
            }

            public List<LocalDate> dueDates(LocalDate today) {
                // Máy tắt từ 01/11: mốc cuối có dữ liệu là 01/11, hôm nay 05/11.
                return ActivityScheduling.dueDates(today, Optional.of(LocalDate.of(2026, 11, 1)));
            }

            public int run(LocalDate date, Instant now) {
                days.add(date);
                return 1;
            }
        };
        var time = new BusinessTime(Clock.fixed(at, BusinessTime.ZONE));
        try {
            new CareDailyJob(List.of(step), runner, runs, time, new MockEnvironment()).runNow();
            // Khởi động lại: bản job mới, cùng mốc trong reminder_marker.
            new CareDailyJob(List.of(step), runner, runs, time, new MockEnvironment()).runNow();
            assertThat(days)
                    .containsExactly(
                            LocalDate.of(2026, 11, 1),
                            LocalDate.of(2026, 11, 2),
                            LocalDate.of(2026, 11, 3),
                            LocalDate.of(2026, 11, 4));
        } finally {
            cleanup(
                    "DELETE FROM reminder_marker WHERE kind='M04.DAILY_WORK' AND JSON_UNQUOTE(JSON_EXTRACT(details,'$.job'))='"
                            + name + "'");
        }
    }

    private void cleanup(String sql) {
        try (var connection = adminConnection();
                var statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (java.sql.SQLException error) {
            throw new IllegalStateException(error);
        }
    }
}
