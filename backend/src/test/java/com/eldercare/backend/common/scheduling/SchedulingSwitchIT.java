package com.eldercare.backend.common.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.test.util.AopTestUtils;

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
    void R_10_testProfile_noJobRunsOnStartup() throws IllegalAccessException {
        List<String> runsOnStartup = new ArrayList<>();
        for (ScheduledJob bean : context.getBeansOfType(ScheduledJob.class).values()) {
            Object job = AopTestUtils.getUltimateTargetObject(bean);
            for (Field field : job.getClass().getDeclaredFields()) {
                if (field.getType() == boolean.class
                        && field.getName().toLowerCase().endsWith("startup")) {
                    field.setAccessible(true);
                    if (field.getBoolean(job)) {
                        runsOnStartup.add(job.getClass().getSimpleName());
                    }
                }
            }
        }

        assertThat(runsOnStartup).as("job chạy lúc khởi động trong test").isEmpty();
    }
}
