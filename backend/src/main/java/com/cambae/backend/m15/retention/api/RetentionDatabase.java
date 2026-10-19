package com.cambae.backend.m15.retention.api;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Kết nối bằng tài khoản {@code eldercare_retention}, tài khoản duy nhất được xóa bảng nhóm 3 và nhật ký (research R-05,
 * R-11; FR-051). Chỉ job loại bỏ dữ liệu hết hạn và {@link RetentionProvider#delete} dùng. Không đăng ký thành bean
 * {@code DataSource} để không thay DataSource chính của ứng dụng; pool chỉ mở kết nối khi job chạy.
 */
@Component
public class RetentionDatabase implements DisposableBean {

    private final HikariDataSource dataSource = new HikariDataSource();
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    RetentionDatabase(
            @Value("${eldercare.datasource.retention.url}") String url,
            @Value("${eldercare.datasource.retention.username}") String username,
            @Value("${eldercare.datasource.retention.password}") String password) {
        dataSource.setPoolName("retention");
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setMaximumPoolSize(2);
        dataSource.setMinimumIdle(0);
        // Cùng thiết lập thời gian, mã ký tự với DataSource chính (application.yml, R-04)
        dataSource.addDataSourceProperty("connectionTimeZone", "UTC");
        dataSource.addDataSourceProperty("forceConnectionTimeZoneToSession", "true");
        dataSource.addDataSourceProperty("characterEncoding", "UTF-8");
        dataSource.addDataSourceProperty("connectionCollation", "utf8mb4_0900_ai_ci");
        this.jdbc = new JdbcTemplate(dataSource);
        this.transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    public JdbcTemplate jdbc() {
        return jdbc;
    }

    /** Transaction trên kết nối {@code eldercare_retention}; mỗi chuỗi bản ghi xóa trong một transaction (BR-M15-13). */
    public TransactionTemplate transaction() {
        return transaction;
    }

    @Override
    public void destroy() {
        dataSource.close();
    }
}
