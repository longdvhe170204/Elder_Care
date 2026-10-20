package com.cambae.backend.m15.account.scheduling;

import com.cambae.backend.common.scheduling.ScheduledJob;
import com.cambae.backend.m15.account.legal.internal.LicenseExpiryWarnings;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Job cảnh báo giấy phép sắp hết hạn, hằng ngày 06:00 (002 T066; research A-09; FR-039). */
@Component
public class WarnExpiringLicensesJob implements ScheduledJob {

    private final LicenseExpiryWarnings warnings;

    public WarnExpiringLicensesJob(LicenseExpiryWarnings warnings) {
        this.warnings = warnings;
    }

    @Scheduled(cron = "${eldercare.jobs.warn-expiring-licenses.cron:0 0 6 * * *}", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = LicenseExpiryWarnings.JOB)
    public void scheduled() {
        warnings.run();
    }

    @Override
    public String name() {
        return LicenseExpiryWarnings.JOB;
    }

    @Override
    public void runNow() {
        warnings.run();
    }
}
