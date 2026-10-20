package com.cambae.backend.common.time.web;

import com.cambae.backend.common.scheduling.ScheduledJob;
import com.cambae.backend.common.time.TestClockControl;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** {@code /api/v1/testing/clock} theo contracts/api-000.yaml, chỉ có ở profile test, demo (NFR-13). */
@RestController
@Profile({"test", "demo"})
@RequestMapping("/api/v1/testing/clock")
public class TestClockController {

    private final TestClockControl clock;
    private final List<ScheduledJob> jobs;

    public TestClockController(TestClockControl clock, List<ScheduledJob> jobs) {
        this.clock = clock;
        this.jobs = jobs;
    }

    @GetMapping
    public ClockResponse read() {
        return current();
    }

    @PostMapping
    public ClockResponse change(@RequestBody ClockRequest request) {
        List<ScheduledJob> toRun = new ArrayList<>();
        if (request.runJobs() != null) {
            for (TestJob requested : request.runJobs()) {
                // Job áp dụng yêu cầu, nhắc yêu cầu (000 T093, T094) đăng ký khi cài; chưa có thì 422
                toRun.add(jobs.stream()
                        .filter(job -> job.name().equals(requested.name()))
                        .findFirst()
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.UNPROCESSABLE_CONTENT, "Job chưa được cài: " + requested)));
            }
        }
        if (request.setTo() != null) {
            clock.setTo(request.setTo().toInstant());
        }
        if (request.advanceBy() != null) {
            clock.advanceBy(Duration.parse(request.advanceBy()));
        }
        // Chạy sau khi đặt giờ, theo thứ tự yêu cầu
        toRun.forEach(ScheduledJob::runNow);
        return current();
    }

    private ClockResponse current() {
        return new ClockResponse(OffsetDateTime.ofInstant(clock.instant(), clock.getZone()), clock.isSimulated());
    }

    public record ClockRequest(OffsetDateTime setTo, String advanceBy, List<TestJob> runJobs) {}

    /** Job chạy ngay theo yêu cầu của test (contracts/api-000.yaml). */
    public enum TestJob {
        APPLY_DUE_REQUESTS,
        REMIND_PENDING_REQUESTS,
        PURGE_EXPIRED_DATA,
        MARK_INACTIVE_ACCOUNTS,
        WARN_EXPIRING_LICENSES,
        LOCK_AFTER_RESIDENCY_END
    }

    public record ClockResponse(OffsetDateTime now, boolean simulated) {}
}
