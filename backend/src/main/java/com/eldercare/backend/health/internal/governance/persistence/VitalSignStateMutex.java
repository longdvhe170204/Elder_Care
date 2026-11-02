package com.eldercare.backend.health.internal.governance.persistence;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Mutex MySQL bảo vệ lần tạo đầu của dòng trạng thái M06 trên nhiều tiến trình. */
@Component
public class VitalSignStateMutex {
    private final JdbcTemplate jdbc;

    public VitalSignStateMutex(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void acquire(String key) {
        Integer acquired = jdbc.queryForObject("SELECT GET_LOCK(?, 30)", Integer.class, key);
        if (!Integer.valueOf(1).equals(acquired)) {
            throw new CannotAcquireLockException("Không lấy được khóa trạng thái chỉ số: " + key);
        }
    }

    public void release(String key) {
        jdbc.queryForObject("SELECT RELEASE_LOCK(?)", Integer.class, key);
    }
}
