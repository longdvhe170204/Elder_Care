package com.cambae.backend.m02.scheduling;

import com.cambae.backend.common.scheduling.ScheduledJob;
import com.cambae.backend.m02.internal.registration.service.RegistrationDailyRunner;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job khai báo hằng ngày 06:00 của 018 ({@code ViecHangNgayM02KhaiBao}; research Z-06; FR-002, FR-006, FR-008) và lần chạy
 * bù lúc khởi động: nhắc, chuyển Hết hạn theo giờ, ghi nghĩa vụ và khóa nhắc; không lập khai báo (Q-306). Tách khỏi
 * {@link M02DailyJob} của 004 (giờ chạy khác). Test tắt lịch và lần chạy lúc khởi động, gọi
 * {@link RegistrationDailyRunner#catchUp()}.
 */
@Component
public class ResidenceRegistrationJob implements ScheduledJob {

    private final RegistrationDailyRunner runner;
    private final boolean onStartup;

    public ResidenceRegistrationJob(
            RegistrationDailyRunner runner,
            @Value("${eldercare.jobs.m02-registration.on-startup:true}") boolean onStartup) {
        this.runner = runner;
        this.onStartup = onStartup;
    }

    @Scheduled(cron = "${eldercare.jobs.m02-registration.cron:0 0 6 * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = RegistrationDailyRunner.JOB)
    public void scheduled() {
        runner.catchUp();
    }

    @EventListener(ApplicationReadyEvent.class)
    @SchedulerLock(name = RegistrationDailyRunner.JOB)
    public void atStartup() {
        if (onStartup) {
            runner.catchUp();
        }
    }

    @Override
    public String name() {
        return RegistrationDailyRunner.JOB;
    }

    @Override
    public void runNow() {
        runner.catchUp();
    }
}
