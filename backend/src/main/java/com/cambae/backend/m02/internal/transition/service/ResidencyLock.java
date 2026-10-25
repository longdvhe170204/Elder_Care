package com.cambae.backend.m02.internal.transition.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Khóa theo người cho mọi lệnh 004 (research T-03): khóa dòng {@code resident_residency} bằng {@code FOR UPDATE}, tạo
 * dòng ở lần khóa đầu. Thứ tự khóa toàn hệ thống: {@code resident} (001, chỉ khi lệnh đi qua 001), rồi dòng này, rồi dòng
 * nghiệp vụ của 004 (hợp đồng, hồ sơ chờ, lượt vắng, hồ sơ kết thúc), rồi vị trí và giường (003). Lệnh dẫn tới Hủy
 * tiếp nhận đi qua 001 nên không gọi hàm này trước.
 */
@Component
public class ResidencyLock {

    private final JdbcTemplate jdbc;

    ResidencyLock(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Chạy trong transaction của lệnh; khóa giữ tới khi transaction kết thúc. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void lock(long residentId) {
        jdbc.update("INSERT IGNORE INTO resident_residency (resident_id) VALUES (?)", residentId);
        jdbc.queryForObject(
                "SELECT resident_id FROM resident_residency WHERE resident_id = ? FOR UPDATE", Long.class, residentId);
    }
}
