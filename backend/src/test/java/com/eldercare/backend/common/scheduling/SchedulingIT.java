package com.eldercare.backend.common.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.support.JobLogCapture;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** R-10, FR-045 (000 T035): ShedLock chặn chạy chồng; mỗi lần chạy có id để làm căn cứ nhật ký. */
class SchedulingIT extends MySqlIntegrationTestBase {

    @Autowired
    LockProvider locks;

    @Autowired
    SchedulerRuns runs;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void R_10_sameJob_cannotRunTwiceAtOnce() {
        LockConfiguration config =
                new LockConfiguration(Instant.now(), "sample-job", Duration.ofMinutes(5), Duration.ZERO);

        SimpleLock first = locks.lock(config).orElseThrow();
        try {
            assertThat(locks.lock(config)).isEmpty();
        } finally {
            first.unlock();
        }
        locks.lock(config).orElseThrow().unlock();
    }

    @Test
    void FR_045_run_isLoggedWithUuid_toJobsLog() {
        try (JobLogCapture log = new JobLogCapture()) {
            String runId = runs.start("SAMPLE_JOB");
            runs.finish(runId, SchedulerRuns.Result.SUCCEEDED, 3);

            assertThat(UUID.fromString(runId)).isNotNull();
            assertThat(log.events(runId)).hasSize(2);
            assertThat(log.events(runId).getFirst())
                    .containsEntry("job", "SAMPLE_JOB")
                    .containsEntry("event", "START");
            assertThat(log.events(runId).getLast())
                    .containsEntry("event", "FINISH")
                    .containsEntry("result", "SUCCEEDED")
                    .containsEntry("processedCount", 3);
        }
    }
}
