package com.eldercare.backend.administration.retention.internal.persistence;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** Cài {@link RetentionDatabase}: pool Hikari riêng cho tài khoản {@code eldercare_retention}, chỉ mở kết nối khi job chạy. */
@Component
class RetentionDatabaseConnection implements RetentionDatabase, DisposableBean {

    private final HikariDataSource dataSource = new HikariDataSource();
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    RetentionDatabaseConnection(
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

    @Override
    public JdbcTemplate jdbc() {
        return jdbc;
    }

    /** Transaction trên kết nối {@code eldercare_retention}; mỗi chuỗi bản ghi xóa trong một transaction (BR-M15-13). */
    @Override
    public TransactionTemplate transaction() {
        return transaction;
    }

    @Override
    public void destroy() {
        dataSource.close();
    }
}
