package com.cambae.backend.m02.internal.registration.persistence;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Đọc, ghi bảng {@code residence_registration}. Quy ước tên (plan 018 "Phase 1 hoàn thành", glossary 24.5): mọi lệnh ghi
 * dòng khai báo mới đặt tên {@code insert…} để {@code ResidenceRegistrationArchTest} bắt được đường đi từ Bộ lập lịch; lệnh
 * lập khai báo thêm ở T008.
 */
@Repository
public class ResidenceRegistrationStore {

    private final JdbcTemplate jdbc;

    ResidenceRegistrationStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Người cao tuổi của khai báo, rỗng nếu không có khai báo {@code id}. */
    public Optional<Long> residentOf(long id) {
        return jdbc
                .query("SELECT resident_id FROM residence_registration WHERE id = ?", (rs, i) -> rs.getLong(1), id)
                .stream()
                .findFirst();
    }
}
