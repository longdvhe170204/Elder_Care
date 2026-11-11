package com.eldercare.backend.common.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Job chạy bù lúc khởi động ({@code atStartup}, NFR-13) có người dùng hiện tại là Bộ lập lịch như lượt {@code @Scheduled}:
 * trước đây job M11 lúc khởi động ghi nhật ký lỗi "Không có người dùng hiện tại" và bỏ lượt (demo 2026-11-10).
 */
@Import(StartupJobSchedulerUserIT.Jobs.class)
class StartupJobSchedulerUserIT extends MySqlIntegrationTestBase {

    static class ProbeJob implements ScheduledJob {
        final AtomicBoolean scheduler = new AtomicBoolean();
        CurrentUser users;

        public String name() {
            return "PROBE";
        }

        public void runNow() {}

        public void atStartup() {
            scheduler.set(users.isScheduler());
        }

        /** Đọc qua phương thức: bean là proxy của aspect, trường của proxy không mang giá trị. */
        public boolean ranAsScheduler() {
            return scheduler.get();
        }
    }

    @TestConfiguration
    static class Jobs {
        @Bean
        ProbeJob probeJob(CurrentUser users) {
            var job = new ProbeJob();
            job.users = users;
            return job;
        }
    }

    @Autowired
    ProbeJob job;

    @Test
    void NFR_13_startupRun_actsAsScheduler() {
        job.atStartup();
        assertThat(job.ranAsScheduler()).isTrue();
    }
}
