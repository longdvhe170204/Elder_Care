package com.eldercare.backend.common.scheduling;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bộ lập lịch (research R-10): {@code @Scheduled} cùng ShedLock để một job không chạy chồng khi
 * kích hoạt tay hoặc khởi động lại. Khóa tính theo giờ của MySQL vì là thông số kỹ thuật, không phải mốc nghiệp vụ.
 * Khóa job là dòng {@code technical_lock} có {@code lock_key} là tên job, dùng thêm ba cột {@code lock_until}, {@code
 * locked_at}, {@code locked_by} (gộp bảng {@code shedlock} 2026-11-13, research R-15 của 000).
 *
 * <p>Công tắc chung {@value #ENABLED}: {@code false} (profile test) thì không job nào chạy theo lịch, và mặc định của
 * {@code eldercare.jobs.<job>.on-startup} cũng lấy theo công tắc này; test gọi job trực tiếp hoặc qua {@code runJobs}.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class SchedulingConfig {

    public static final String ENABLED = "eldercare.scheduling.enabled";

    @Bean
    LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .withTableName("technical_lock")
                .withColumnNames(
                        new JdbcTemplateLockProvider.ColumnNames("lock_key", "lock_until", "locked_at", "locked_by"))
                .usingDbTime()
                .build());
    }

    @Configuration
    @EnableScheduling
    @ConditionalOnBooleanProperty(name = ENABLED, matchIfMissing = true)
    static class Enabled {}
}
