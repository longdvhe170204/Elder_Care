package com.cambae.backend.m02.scheduling;

import com.cambae.backend.common.scheduling.ScheduledJob;
import com.cambae.backend.m02.internal.transition.service.M02DailyRunner;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job hằng ngày 00:15 của M02 ({@code ViecHangNgayM02}; research T-11; FR-077, BR-M02-19) và lần chạy bù lúc khởi động.
 * Profile test tắt lịch và lần chạy lúc khởi động; test gọi {@link M02DailyRunner#catchUp()}.
 */
@Component
public class M02DailyJob implements ScheduledJob {

    private final M02DailyRunner runner;
    private final boolean onStartup;

    public M02DailyJob(M02DailyRunner runner, @Value("${eldercare.jobs.m02-daily.on-startup:true}") boolean onStartup) {
        this.runner = runner;
        this.onStartup = onStartup;
    }

    @Scheduled(cron = "${eldercare.jobs.m02-daily.cron:0 15 0 * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = M02DailyRunner.JOB)
    public void scheduled() {
        runner.catchUp();
    }

    @EventListener(ApplicationReadyEvent.class)
    @SchedulerLock(name = M02DailyRunner.JOB)
    public void atStartup() {
        if (onStartup) {
            runner.catchUp();
        }
    }

    @Override
    public String name() {
        return M02DailyRunner.JOB;
    }

    @Override
    public void runNow() {
        runner.catchUp();
    }
}
