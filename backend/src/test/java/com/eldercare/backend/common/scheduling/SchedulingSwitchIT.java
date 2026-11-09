package com.eldercare.backend.common.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.time.web.TestClockController.TestJob;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.config.ScheduledTaskHolder;

/**
 * Công tắc chung {@code eldercare.scheduling.enabled} (profile test đặt {@code false}): không job nào chạy theo lịch hay
 * lúc khởi động trong test, kể cả job module thêm sau; test gọi job trực tiếp hoặc qua {@code runJobs} (research R-10).
 * Trước đây mỗi job phải tự ghi {@code cron: "-"} ở profile test, job của m03, m07 quên nên chạy giữa lúc test.
 */
class SchedulingSwitchIT extends MySqlIntegrationTestBase {

    @Autowired
    ApplicationContext context;

    @Test
    void R_10_testProfile_registersNoScheduledTask() {
        List<String> tasks = context.getBeansOfType(ScheduledTaskHolder.class).values().stream()
                .flatMap(holder -> holder.getScheduledTasks().stream())
                .map(Object::toString)
                .toList();

        assertThat(tasks).as("job còn lịch chạy trong test").isEmpty();
    }

    @Test
    void NFR_13_everyRunJobsValue_hasARegisteredJob() {
        List<String> registered = context.getBeansOfType(ScheduledJob.class).values().stream()
                .map(ScheduledJob::name)
                .toList();

        assertThat(registered)
                .as("giá trị runJobs của api-000 phải có job cài")
                .containsAll(Arrays.stream(TestJob.values()).map(Enum::name).toList());
    }
}
