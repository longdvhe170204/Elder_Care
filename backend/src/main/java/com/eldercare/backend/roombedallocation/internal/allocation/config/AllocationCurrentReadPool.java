package com.eldercare.backend.roombedallocation.internal.allocation.config;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Pool chỉ đọc độc lập để kiểm view sau khóa trong transaction RR, không chiếm connection của pool lệnh. */
@Component
public class AllocationCurrentReadPool implements DisposableBean {
    private final HikariDataSource reads = new HikariDataSource();

    public AllocationCurrentReadPool(
            DataSource source, @Value("${eldercare.m03.current-allocation-read-pool-size:2}") int size)
            throws SQLException {
        var main = source.unwrap(HikariDataSource.class);
        reads.setPoolName("m03-current-allocation-read");
        reads.setJdbcUrl(main.getJdbcUrl());
        reads.setUsername(main.getUsername());
        reads.setPassword(main.getPassword());
        reads.setDataSourceProperties(main.getDataSourceProperties());
        reads.setMaximumPoolSize(size);
        reads.setMinimumIdle(0);
        reads.setIdleTimeout(main.getIdleTimeout());
        reads.setConnectionTimeout(main.getConnectionTimeout());
        reads.setReadOnly(true);
        reads.setAutoCommit(true);
        reads.setTransactionIsolation("TRANSACTION_READ_COMMITTED");
    }

    /** Không dùng DataSourceUtils: không bind connection đọc vào transaction nguồn. */
    public Connection open() throws SQLException {
        return reads.getConnection();
    }

    @Override
    public void destroy() {
        reads.close();
    }
}
