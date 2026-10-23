package com.cambae.backend.m09.internal.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m09.internal.persistence.M09JobRuns;
import com.cambae.backend.m09.internal.service.M09TimedTask.Cadence;
import java.time.*;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** T008, N-13: chạy bù mốc gần nhất; từng tác vụ xét các dòng nguồn quá hạn và chống lặp theo nguồn. */
@Service
public class M09Jobs {
    private final M09JobRuns runs;
    private final BusinessTime time;
    private final ObjectProvider<M09TimedTask> tasks;
    private final TransactionTemplate transaction;

    public M09Jobs(
            M09JobRuns runs,
            BusinessTime time,
            ObjectProvider<M09TimedTask> tasks,
            PlatformTransactionManager manager) {
        this.runs = runs;
        this.time = time;
        this.tasks = tasks;
        this.transaction = new TransactionTemplate(manager);
    }

    public void runDue(Cadence cadence) {
        Instant actual = time.now();
        Instant scheduled = scheduledAt(cadence, actual);
        String job = "M09_" + cadence.name();
        try {
            transaction.executeWithoutResult(status -> {
                if (!runs.claim(job, scheduled, actual)) return;
                tasks.orderedStream().filter(t -> t.cadence() == cadence).forEach(t -> t.run(scheduled, actual));
                runs.succeed(job, scheduled, time.now());
            });
        } catch (RuntimeException exception) {
            transaction.executeWithoutResult(status -> runs.fail(job, scheduled, actual, time.now()));
            throw exception;
        }
    }

    private Instant scheduledAt(Cadence cadence, Instant actual) {
        return switch (cadence) {
            case MINUTE -> actual.truncatedTo(ChronoUnit.MINUTES);
            case HOURLY -> actual.truncatedTo(ChronoUnit.HOURS);
            case DAILY -> {
                var local = actual.atZone(BusinessTime.ZONE);
                var due = local.toLocalDate().atTime(0, 10).atZone(BusinessTime.ZONE);
                yield (due.toInstant().isAfter(actual) ? due.minusDays(1) : due).toInstant();
            }
        };
    }
}
