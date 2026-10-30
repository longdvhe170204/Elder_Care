package com.cambae.backend.m07;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.support.MySqlIntegrationTestBase;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Kiểm thử tích hợp trigger của bảng đơn thuốc (V006_02__prescription.sql, DBR-14, FR-014).
 * - Trigger trg_prescription_lock: chặn sửa trường cố định, chặn sửa đơn đã ngừng/hết hạn.
 * - Trigger trg_prescription_no_delete: chặn xóa vật lý bản ghi đơn thuốc.
 */
class PrescriptionTriggerIT extends MySqlIntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("DBR-14: Bảng prescription có đầy đủ trigger khóa và chặn xóa")
    void triggers_exist() throws SQLException {
        Set<String> triggers = new HashSet<>();
        try (Connection c = adminConnection();
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery(
                        "SELECT LOWER(TRIGGER_NAME) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA = DATABASE()")) {
            while (rs.next()) {
                triggers.add(rs.getString(1));
            }
        }

        assertThat(triggers).contains("trg_prescription_lock", "trg_prescription_no_delete");
    }

    @Test
    @DisplayName("FR-014, DBR-14: Trigger chặn xóa vật lý đơn thuốc")
    void cannot_delete_prescription() {
        jdbc.update(
                "INSERT INTO resident_medication (resident_id, reconciliation_due_at, reconciliation_reason, reconciliation_warned_at, version) "
                        + "VALUES (9901, NULL, NULL, NULL, 0) ON DUPLICATE KEY UPDATE version = version");

        jdbc.update(
                "INSERT INTO medication (id, name, concentration, dosage_form, default_route, usage_unit, special_control, status, version) "
                        + "VALUES (9901, 'Med Test', '10mg', 'Tablet', 'Oral', 'Viên', 0, 'ACTIVE', 0) ON DUPLICATE KEY UPDATE version = version");

        jdbc.update(
                "INSERT INTO prescription (resident_id, medication_id, dosage, dosage_unit, route, prescription_type, "
                        + "frequency_type, started_at, medication_source, prescriber_type, prescriber_name, prescriber_id, "
                        + "prescribed_at, status, creator_id, created_at, version) "
                        + "VALUES (9901, 9901, 1.0, 'Viên', 'Uống', 'PERIODIC', 'TIMED_SLOTS', NOW(), 'FACILITY', 'INTERNAL', "
                        + "'Dr. A', 1, CURDATE(), 'ACTIVE', 1, NOW(), 0)");

        Long id = jdbc.queryForObject(
                "SELECT id FROM prescription WHERE resident_id = 9901 ORDER BY id DESC LIMIT 1", Long.class);

        // Tài khoản ứng dụng không có quyền DELETE; thử bằng quản trị để chạm trigger
        assertThatThrownBy(() -> jdbc.update("DELETE FROM prescription WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> {
                    try (Connection c = adminConnection();
                            Statement st = c.createStatement()) {
                        st.executeUpdate("DELETE FROM prescription WHERE id = " + id);
                    }
                })
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("prescription cannot be deleted");
    }

    @Test
    @DisplayName("FR-014, DBR-14: Trigger trg_prescription_lock chặn sửa các trường cố định của đơn")
    void cannot_modify_immutable_fields() {
        jdbc.update(
                "INSERT INTO resident_medication (resident_id, reconciliation_due_at, reconciliation_reason, reconciliation_warned_at, version) "
                        + "VALUES (9902, NULL, NULL, NULL, 0) ON DUPLICATE KEY UPDATE version = version");

        jdbc.update(
                "INSERT INTO medication (id, name, concentration, dosage_form, default_route, usage_unit, special_control, status, version) "
                        + "VALUES (9902, 'Med Test 2', '20mg', 'Tablet', 'Oral', 'Viên', 0, 'ACTIVE', 0) ON DUPLICATE KEY UPDATE version = version");

        jdbc.update(
                "INSERT INTO prescription (resident_id, medication_id, dosage, dosage_unit, route, prescription_type, "
                        + "frequency_type, started_at, medication_source, prescriber_type, prescriber_name, prescriber_id, "
                        + "prescribed_at, status, creator_id, created_at, version) "
                        + "VALUES (9902, 9902, 1.0, 'Viên', 'Uống', 'PERIODIC', 'TIMED_SLOTS', NOW(), 'FACILITY', 'INTERNAL', "
                        + "'Dr. B', 1, CURDATE(), 'ACTIVE', 1, NOW(), 0)");

        Long id = jdbc.queryForObject(
                "SELECT id FROM prescription WHERE resident_id = 9902 ORDER BY id DESC LIMIT 1", Long.class);

        // Sửa liều (dosage) bị trigger chặn
        assertThatThrownBy(() -> jdbc.update("UPDATE prescription SET dosage = 2.0 WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Fixed fields of saved prescription cannot be modified");

        // Sửa ghi chú hướng dẫn (instruction_note) thì được phép
        int updated = jdbc.update(
                "UPDATE prescription SET instruction_note = 'Uống sau ăn no', version = version + 1 WHERE id = ?", id);
        assertThat(updated).isEqualTo(1);
    }
}
