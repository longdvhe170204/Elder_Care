package com.cambae.backend.m03.scheduling;

import com.cambae.backend.common.scheduling.*;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m03.internal.bed.persistence.BedHoldStore;
import com.cambae.backend.m03.internal.bed.service.HoldExpiryProcessor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** T044/G-09: hết hạn giữ chỗ mỗi 15 phút và lúc khởi động; không tạo phân bổ. */
@Component
public class HoldExpiryJob implements ScheduledJob {
    private final BedHoldStore store;
    private final HoldExpiryProcessor processor;
    private final BusinessTime time;
    private final SchedulerRuns runs;
    private final Environment environment;
    private final boolean startup;

    public HoldExpiryJob(
            BedHoldStore store,
            HoldExpiryProcessor processor,
            BusinessTime time,
            SchedulerRuns runs,
            Environment environment,
            @Value("${eldercare.jobs.expire-bed-holds.on-startup:true}") boolean startup) {
        this.store = store;
        this.processor = processor;
        this.time = time;
        this.runs = runs;
        this.environment = environment;
        this.startup = startup;
    }

    @Scheduled(cron = "${eldercare.jobs.expire-bed-holds.cron:0 */15 * * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = "EXPIRE_BED_HOLDS")
    public void scheduled() {
        if (!environment.acceptsProfiles(Profiles.of("test"))) runNow();
    }

    @EventListener(ApplicationReadyEvent.class)
    @SchedulerLock(name = "EXPIRE_BED_HOLDS")
    public void atStartup() {
        if (startup && !environment.acceptsProfiles(Profiles.of("test"))) runNow();
    }

    @Override
    public String name() {
        return "EXPIRE_BED_HOLDS";
    }

    @Override
    public void runNow() {
        long id = runs.start(name());
        int count = 0;
        try {
            for (var held : store.expired(time.now())) if (processor.expire(held, id)) count++;
            runs.finish(id, SchedulerRuns.Result.SUCCEEDED, count);
        } catch (RuntimeException error) {
            runs.finish(id, SchedulerRuns.Result.FAILED, count);
            throw error;
        }
    }
}
