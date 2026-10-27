package com.cambae.backend.m02.internal.registration.persistence;

import com.cambae.backend.m02.internal.registration.domain.ReminderType;
import java.sql.Date;
import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Khóa nhắc {@code registration_reminder (type, resident_id, date)} (data-model mục 4; research Z-06): việc của job và
 * listener giành khóa trước khi phát nhắc, cùng transaction với lần phát, nên mỗi (loại, người, ngày) nhắc một lần kể cả
 * khi chạy lại hay chạy song song. Chỉ ghi bảng kỹ thuật, không tạo khai báo (Q-306).
 */
@Repository
public class RegistrationReminderStore {

    private final JdbcTemplate jdbc;

    RegistrationReminderStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** {@code true} nếu đây là lần nhắc đầu tiên của (loại, người, ngày). */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean claim(ReminderType type, long residentId, LocalDate day) {
        return jdbc.update(
                        "INSERT IGNORE INTO registration_reminder (type, resident_id, `date`) VALUES (?, ?, ?)",
                        type.name(),
                        residentId,
                        Date.valueOf(day))
                == 1;
    }
}
