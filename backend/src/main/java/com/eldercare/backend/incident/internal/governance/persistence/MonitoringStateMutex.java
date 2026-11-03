package com.eldercare.backend.incident.internal.governance.persistence;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Mutex MySQL bảo vệ lần tạo đầu của dòng trạng thái M05 trên nhiều tiến trình. */
@Component
public class MonitoringStateMutex {
    private final JdbcTemplate jdbc;

    public MonitoringStateMutex(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void acquire(String key) {
        Integer acquired = jdbc.queryForObject("SELECT GET_LOCK(?, 30)", Integer.class, key);
        if (!Integer.valueOf(1).equals(acquired)) {
            throw new CannotAcquireLockException("Không lấy được khóa trạng thái theo dõi: " + key);
        }
    }

    public void release(String key) {
        jdbc.queryForObject("SELECT RELEASE_LOCK(?)", Integer.class, key);
    }
}
