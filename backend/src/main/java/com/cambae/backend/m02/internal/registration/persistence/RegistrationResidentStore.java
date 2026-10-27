package com.cambae.backend.m02.internal.registration.persistence;

import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đích khóa theo người {@code registration_resident} (data-model mục 2, DBR-33): mọi lệnh khai báo khóa dòng này trước,
 * tạo dòng nếu chưa có; giữ cờ thường trú cùng xã/phường của hồ sơ (FR-001, Q-215).
 */
@Repository
public class RegistrationResidentStore {

    private final JdbcTemplate jdbc;

    RegistrationResidentStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Khóa người cao tuổi tới hết transaction; trả cờ thường trú cùng xã/phường. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean lock(long residentId) {
        jdbc.update(
                "INSERT INTO registration_resident (resident_id) VALUES (?) ON DUPLICATE KEY UPDATE resident_id = resident_id",
                residentId);
        return jdbc.queryForObject(
                "SELECT local_permanent_resident FROM registration_resident WHERE resident_id = ? FOR UPDATE",
                Boolean.class,
                residentId);
    }

    /** Ghi xác nhận thường trú cùng xã/phường (đã khóa). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void confirmLocalPermanentResident(long residentId, String basisNote, long confirmedById, Instant at) {
        jdbc.update(
                "UPDATE registration_resident SET local_permanent_resident = TRUE, basis_note = ?, confirmed_by_id = ?,"
                        + " confirmed_at = ?, version = version + 1 WHERE resident_id = ?",
                basisNote,
                confirmedById,
                ResidenceRegistrationStore.utc(at),
                residentId);
    }
}
