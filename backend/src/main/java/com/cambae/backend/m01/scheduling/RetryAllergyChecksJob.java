package com.cambae.backend.m01.scheduling;

import com.cambae.backend.common.scheduling.ScheduledJob;
import com.cambae.backend.m01.internal.health.service.AllergyCheckRetry;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Job phát lại kiểm tra dị ứng, mỗi 15 phút (001 T031; research H-12). */
@Component
public class RetryAllergyChecksJob implements ScheduledJob {

    private final AllergyCheckRetry retry;

    public RetryAllergyChecksJob(AllergyCheckRetry retry) {
        this.retry = retry;
    }

    @Scheduled(cron = "${eldercare.jobs.retry-allergy-checks.cron:0 */15 * * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = AllergyCheckRetry.JOB)
    public void scheduled() {
        retry.run();
    }

    @Override
    public String name() {
        return AllergyCheckRetry.JOB;
    }

    @Override
    public void runNow() {
        retry.run();
    }
}
