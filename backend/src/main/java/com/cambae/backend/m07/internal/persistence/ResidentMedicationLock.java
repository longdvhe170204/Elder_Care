package com.cambae.backend.m07.internal.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Khóa bi quan theo hồ sơ người cao tuổi trên bảng {@code resident_medication} (thuoc_nct) theo research D-03.
 * Thứ tự khóa toàn hệ thống: ... -> cham_soc_nct (005) -> thuoc_nct (006) -> don_thuoc -> lieu_thuoc -> ...
 */
@Component
public class ResidentMedicationLock {

    private final JdbcTemplate jdbc;

    public ResidentMedicationLock(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Đảm bảo bản ghi tồn tại và khóa dòng tới hết transaction hiện tại.
     *
     * @param residentId logic id người cao tuổi
     */
    public void lock(long residentId) {
        jdbc.update(
                "INSERT INTO resident_medication (resident_id, version) VALUES (?, 0) "
                        + "ON DUPLICATE KEY UPDATE resident_id = resident_id",
                residentId);
        jdbc.queryForList(
                "SELECT resident_id FROM resident_medication WHERE resident_id = ? FOR UPDATE",
                Long.class,
                residentId);
    }
}
