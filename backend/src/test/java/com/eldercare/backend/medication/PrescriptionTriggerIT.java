package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Kiểm thử tích hợp trigger khóa và chống xóa vật lý của đơn thuốc. */
class PrescriptionTriggerIT extends MySqlIntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("FR-014, DBR-14: Trigger khóa các trường cố định của đơn")
    void cannot_modify_immutable_fields() {
        insertFixture(9902L, "Med Test 2", "20mg", "Dr. B");

        Long id = jdbc.queryForObject(
                "SELECT id FROM prescription WHERE resident_id = 9902 ORDER BY id DESC LIMIT 1", Long.class);

        assertThatThrownBy(() -> jdbc.update("UPDATE prescription SET dosage = 2.0 WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Fixed fields of saved prescription cannot be modified");

        int updated = jdbc.update(
                "UPDATE prescription SET instruction_note = 'Uống sau ăn no', version = version + 1 WHERE id = ?", id);
        assertThat(updated).isEqualTo(1);
    }

    private void insertFixture(long id, String name, String concentration, String prescriberName) {
        jdbc.update(
                "INSERT INTO resident_module_state (resident_id, module, data, version, updated_at)"
                        + " VALUES (?, 'M07', JSON_OBJECT(), 0, UTC_TIMESTAMP(6))"
                        + " ON DUPLICATE KEY UPDATE version = version",
                id);

        jdbc.update(
                "INSERT INTO catalog_item (id, catalog_type, module, code, name, sort_order, status, attributes, version)"
                        + " VALUES (?, 'M07.MEDICATION', 'M07', ?, ?, 0, 'ACTIVE',"
                        + " JSON_OBJECT('name', ?, 'concentration', ?, 'dosageForm', 'Tablet',"
                        + " 'defaultRoute', 'Oral', 'usageUnit', 'Viên', 'specialControl', FALSE,"
                        + " 'activeIngredientIds', JSON_ARRAY(701)), 0)"
                        + " ON DUPLICATE KEY UPDATE version = version",
                id,
                "M07-TRIGGER-" + id,
                name + " | " + concentration + " | Tablet",
                name,
                concentration);

        jdbc.update(
                "INSERT INTO prescription (resident_id, medication_id, active_ingredient_ids, dosage, dosage_unit, route,"
                        + " prescription_type, frequency_type, started_at, medication_source, prescriber_type,"
                        + " prescriber_name, prescriber_id, prescribed_at, status, creator_id, created_at, version)"
                        + " VALUES (?, ?, JSON_ARRAY(701), 1.0, 'Viên', 'Uống', 'PERIODIC', 'TIMED_SLOTS', NOW(),"
                        + " 'FACILITY', 'INTERNAL', ?, 1, CURDATE(), 'ACTIVE', 1, NOW(), 0)",
                id,
                id,
                prescriberName);
    }
}
