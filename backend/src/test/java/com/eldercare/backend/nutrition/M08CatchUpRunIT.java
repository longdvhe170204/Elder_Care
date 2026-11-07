package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.nutrition.internal.conflict.service.M08EventListener;
import com.eldercare.backend.nutrition.internal.schedule.service.M08DailyTask;
import com.eldercare.backend.nutrition.internal.schedule.service.M08JobMarkers;
import com.eldercare.backend.nutrition.internal.schedule.service.M08JobService;
import com.eldercare.backend.nutrition.internal.schedule.service.M08MinuteTask;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

/** 011 T006, research M-08, M-15, NFR-04: khung job chạy bù và listener idempotent. */
@Import(M08CatchUpRunIT.Tasks.class)
class M08CatchUpRunIT extends MySqlIntegrationTestBase {

    static final List<String> RAN = new ArrayList<>();

    @TestConfiguration
    static class Tasks {
        @Bean
        M08MinuteTask recordingMinuteTask() {
            return new M08MinuteTask() {
                @Override
                public String key() {
                    return "TEST_RECORDING";
                }

                @Override
                public void run(Instant now) {
                    RAN.add("minute@" + now);
                }
            };
        }

        @Bean
        M08MinuteTask failingMinuteTask() {
            return new M08MinuteTask() {
                @Override
                public String key() {
                    return "TEST_FAILING";
                }

                @Override
                public void run(Instant now) {
                    throw new IllegalStateException("boom");
                }
            };
        }

        @Bean
        M08DailyTask recordingDailyTask() {
            return new M08DailyTask() {
                @Override
                public String key() {
                    return "TEST_DAILY";
                }

                @Override
                public void run(LocalDate today, Instant now) {
                    RAN.add("daily@" + today);
                }
            };
        }
    }

    @Autowired
    M08JobService jobs;

    @Autowired
    M08JobMarkers markers;

    @Autowired
    M08EventListener listener;

    @Autowired
    TransactionTemplate tx;

    @Test
    void NFR_04_oneFailingTaskDoesNotStopOthers_andJobReportsFailure() {
        RAN.clear();
        Instant now = Instant.parse("2026-11-10T01:00:00Z");

        assertThatThrownBy(() -> jobs.runMinuteTasks(now)).isInstanceOf(IllegalStateException.class);

        assertThat(RAN).contains("minute@" + now);
    }

    @Test
    void NFR_04_dailyTasksRunWithTodayAndNow() {
        RAN.clear();

        jobs.runDailyTasks(LocalDate.parse("2026-11-10"), Instant.parse("2026-11-09T17:00:00Z"));

        assertThat(RAN).containsExactly("daily@2026-11-10");
    }

    @Test
    void NFR_04_jobMarkerIsWrittenOncePerCheckpoint_soCatchUpDoesNotRepeat() {
        String checkpoint = "cp-" + UUID.randomUUID();

        Boolean first = tx.execute(s -> markers.firstTime("TEST", "DAILY_MEAL", 1L, checkpoint));
        Boolean again = tx.execute(s -> markers.firstTime("TEST", "DAILY_MEAL", 1L, checkpoint));
        Boolean otherRow = tx.execute(s -> markers.firstTime("TEST", "DAILY_MEAL", 2L, checkpoint));

        assertThat(first).isTrue();
        assertThat(again).isFalse();
        assertThat(otherRow).isTrue();
    }

    @Test
    void M_08_listenerRunsWorkOncePerEventKey() {
        String key = "EVT-" + UUID.randomUUID();
        List<String> done = new ArrayList<>();

        listener.once(key, () -> done.add("a"));
        listener.once(key, () -> done.add("b"));
        listener.once("EVT-" + UUID.randomUUID(), () -> done.add("c"));

        assertThat(done).containsExactly("a", "c");
    }

    @Test
    void M_08_listenerLongEventKeyIsStillIdempotent() {
        String key = "EVT-" + "x".repeat(200) + UUID.randomUUID();
        List<String> done = new ArrayList<>();

        listener.once(key, () -> done.add("a"));
        listener.once(key, () -> done.add("b"));

        assertThat(done).containsExactly("a");
    }

    @Test
    void M_08_failedWorkRollsBackMarker_soRedeliveryRunsAgain() {
        String key = "EVT-" + UUID.randomUUID();
        List<String> done = new ArrayList<>();

        assertThatThrownBy(() -> listener.once(key, () -> {
                    throw new IllegalStateException("fail");
                }))
                .isInstanceOf(IllegalStateException.class);
        listener.once(key, () -> done.add("retry"));

        assertThat(done).containsExactly("retry");
    }
}
