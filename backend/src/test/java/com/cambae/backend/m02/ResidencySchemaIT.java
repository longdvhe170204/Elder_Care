package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.support.MySqlIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 004 T005 (data-model mục 2, 11; FR-072, FR-077, BR-M02-19): bảng khóa theo người, bảng chạy theo ngày và bảng nhắc
 * có khóa chính đúng để lệnh, việc và lần nhắc không trùng.
 */
class ResidencySchemaIT extends MySqlIntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void FR_072_residencyRow_isOnePerResident_andClosedAtDefaultsToOpen() {
        jdbc.update("INSERT INTO resident_residency (resident_id) VALUES (910001)");

        assertThatThrownBy(() -> jdbc.update("INSERT INTO resident_residency (resident_id) VALUES (910001)"))
                .isInstanceOf(DuplicateKeyException.class);
        assertThat(jdbc.queryForObject(
                        "SELECT closed_at IS NULL FROM resident_residency WHERE resident_id = 910001", Boolean.class))
                .isTrue();
        assertThat(jdbc.queryForObject(
                        "SELECT version FROM resident_residency WHERE resident_id = 910001", Integer.class))
                .isZero();
    }

    @Test
    void FR_077_dailyRun_eachTaskRunsOncePerDay() {
        jdbc.update("INSERT INTO daily_run (task, `date`, result) VALUES ('ABSENCE_DAYS', '2031-03-04', 'SUCCEEDED')");

        assertThat(
                        jdbc.update(
                                "INSERT IGNORE INTO daily_run (task, `date`, result) VALUES ('ABSENCE_DAYS', '2031-03-04', 'SUCCEEDED')"))
                .isZero();
        jdbc.update("INSERT INTO daily_run (task, `date`, result) VALUES ('ABSENCE_DAYS', '2031-03-05', 'SKIPPED')");
        jdbc.update("INSERT INTO daily_run (task, `date`, result) VALUES ('CONTRACTS', '2031-03-04', 'SUCCEEDED')");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM daily_run WHERE `date` IN ('2031-03-04', '2031-03-05')", Integer.class))
                .isEqualTo(3);
    }

    @Test
    void FR_072_reminder_oneRowPerTypeAndSubject() {
        jdbc.update(
                "INSERT INTO m02_reminder (type, subject_id, last_reminded_at, `count`) VALUES ('POST_DEATH_TASKS', 77, '2031-03-04 01:00:00.000000', 1)");

        assertThatThrownBy(
                        () -> jdbc.update(
                                "INSERT INTO m02_reminder (type, subject_id, last_reminded_at, `count`) VALUES ('POST_DEATH_TASKS', 77, '2031-03-05 01:00:00.000000', 2)"))
                .isInstanceOf(DuplicateKeyException.class);
        jdbc.update(
                "INSERT INTO m02_reminder (type, subject_id, last_reminded_at, `count`) VALUES ('CONTRACT_OVERDUE', 77, '2031-03-04 01:00:00.000000', 1)");
    }
}
