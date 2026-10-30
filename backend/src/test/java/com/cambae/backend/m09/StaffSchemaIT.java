package com.cambae.backend.m09;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.support.MySqlIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** T004: chứng minh bất biến tại MySQL thật, dùng quyền ứng dụng. */
@Transactional
class StaffSchemaIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    private long staff() {
        jdbc.update("INSERT INTO staff_profile(full_name, position) VALUES ('[Nhân viên kiểm thử]', 'CAREGIVER')");
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Test
    void FR_001_staffCannotBeDeletedOrChangeStatusWithoutCommand() {
        long id = staff();
        assertThatThrownBy(() -> jdbc.update("DELETE FROM staff_profile WHERE id=?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE staff_profile SET status='RESIGNED' WHERE id=?", id))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_005_requiredDefaultCatalogsExist() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM department", Integer.class))
                .isEqualTo(4);
        assertThat(jdbc.queryForList("SELECT code FROM credential_type", String.class))
                .contains("DOCTOR_LICENSE", "NURSING_LICENSE", "FIRST_AID_TRAINING");
    }

    @Test
    void DBR_36_staffCannotHaveTwoOpenDepartments() {
        long id = staff();
        jdbc.update(
                "INSERT INTO staff_department(staff_id,department_id,starts_at,actor_id,reason) VALUES (?,1,'2026-10-23',1,'Kiểm thử')",
                id);
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO staff_department(staff_id,department_id,starts_at,actor_id,reason) VALUES (?,2,'2026-10-23',1,'Kiểm thử')",
                        id))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_001a_departmentLeadHistoryCanOnlyBeClosedOnce() {
        long id = staff();
        jdbc.update(
                "INSERT INTO department_lead_history(department_id,staff_id,starts_at,actor_id,reason) VALUES (1,?,'2026-10-23',1,'Kiểm thử')",
                id);
        long history = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("UPDATE department_lead_history SET ends_at='2026-10-24' WHERE id=?", history);
        assertThatThrownBy(() ->
                        jdbc.update("UPDATE department_lead_history SET ends_at='2026-10-25' WHERE id=?", history))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE department_lead_history SET reason='Ghi đè' WHERE id=?", history))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_006_credentialsCannotBeDeletedOrSilentlyEdited() {
        long id = staff();
        jdbc.update(
                "INSERT INTO credential(staff_id,credential_type_id,issue_date,expiry_date) VALUES (?,1,'2026-01-01','2026-12-31')",
                id);
        long credential = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM credential WHERE id=?", credential))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE credential SET issue_date='2025-01-01' WHERE id=?", credential))
                .isInstanceOf(DataAccessException.class);
        jdbc.update("UPDATE credential SET expiry_warning_at='2026-10-23' WHERE id=?", credential);
    }
}
