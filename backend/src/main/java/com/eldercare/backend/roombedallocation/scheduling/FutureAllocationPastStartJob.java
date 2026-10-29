package com.eldercare.backend.roombedallocation.scheduling;

import com.eldercare.backend.common.scheduling.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.roombedallocation.internal.allocation.persistence.PastStartAllocationStore;
import com.eldercare.backend.roombedallocation.internal.allocation.service.PastStartAllocationProcessor;
import java.util.HashSet;
import java.util.Set;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** T044: chỉ cảnh báo, không tạo hoặc bắt đầu bản ghi phân bổ theo đồng hồ. */
@Component
public class FutureAllocationPastStartJob implements ScheduledJob {
    private final PastStartAllocationStore store;
    private final PastStartAllocationProcessor processor;
    private final ResidentApi residents;
    private final BusinessTime time;
    private final SchedulerRuns runs;
    private final Environment environment;
    private final boolean startup;

    public FutureAllocationPastStartJob(
            PastStartAllocationStore store,
            PastStartAllocationProcessor processor,
            ResidentApi residents,
            BusinessTime time,
            SchedulerRuns runs,
            Environment environment,
            @Value(
                            "${eldercare.jobs.report-future-allocation-past-start.on-startup:${eldercare.scheduling.enabled:true}}")
                    boolean startup) {
        this.store = store;
        this.processor = processor;
        this.residents = residents;
        this.time = time;
        this.runs = runs;
        this.environment = environment;
        this.startup = startup;
    }

    @Scheduled(
            cron = "${eldercare.jobs.report-future-allocation-past-start.cron:0 */15 * * * *}",
            zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = "REPORT_FUTURE_ALLOCATION_PAST_START")
    public void scheduled() {
        if (!environment.acceptsProfiles(Profiles.of("test"))) runNow();
    }

    @EventListener(ApplicationReadyEvent.class)
    @SchedulerLock(name = "REPORT_FUTURE_ALLOCATION_PAST_START")
    public void atStartup() {
        if (startup && !environment.acceptsProfiles(Profiles.of("test"))) runNow();
    }

    @Override
    public String name() {
        return "REPORT_FUTURE_ALLOCATION_PAST_START";
    }

    @Override
    public void runNow() {
        long run = runs.start(name());
        int count = 0;
        try {
            var admissions = new HashSet<>(residents.byStatus(Set.of(ResidentStatus.ADMISSION_IN_PROGRESS)));
            for (var row : store.due(time.now()))
                if (admissions.contains(row.residentId()) && processor.report(row)) count++;
            runs.finish(run, SchedulerRuns.Result.SUCCEEDED, count);
        } catch (RuntimeException error) {
            runs.finish(run, SchedulerRuns.Result.FAILED, count);
            throw error;
        }
    }
}
