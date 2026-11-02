package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.medication.internal.family.domain.FamilyMedicationPrescription;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationItem;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/** 006 T069: {@code V006_95} chép dữ liệu M07 sang bảng chung rồi bỏ các bảng cũ theo R-15. */
class MedicationTableConsolidationMigrationIT extends MySqlIntegrationTestBase {

    private static final String SUFFIX = "m006_95";
    private static JdbcTemplate db;
    private static final long medicationId = 6_001L;
    private static final long prescriptionId = 6_002L;
    private static final long reconciliationId = 6_003L;
    private static final long reconciliationItemId = 6_004L;
    private static final long familyBatchId = 6_005L;
    private static final long familyBindingId = 6_006L;
    private static Instant legacyBindingFrom;
    private static Instant legacyBatchReceivedAt;

    @BeforeAll
    static void migrateLegacyData() {
        Flyway beforeConsolidation = Flyway.configure()
                .configuration(scratchFlyway(SUFFIX).getConfiguration())
                .target("6.15")
                .load();
        beforeConsolidation.migrate();
        db = new JdbcTemplate(beforeConsolidation.getConfiguration().getDataSource());

        db.update("INSERT INTO resident_medication (resident_id, reconciliation_due_at, reconciliation_reason,"
                + " reconciliation_warned_at, version) VALUES (6001, '2026-10-31 08:00:00', 'ADMISSION',"
                + " '2026-10-30 08:00:00', 2)");
        db.update("INSERT INTO resident_medication (resident_id, version) VALUES (6002, 0)");
        db.update("INSERT INTO medication_reconciliation (id, resident_id, reason, reason_detail, status,"
                + " created_by_id, created_at, confirmed_by_id, confirmed_at) VALUES"
                + " (6010, 6010, 'PROFESSIONAL_REQUEST', 'Đã xác nhận trước migration', 'CONFIRMED',"
                + " 9001, '2026-10-30 01:00:00', 9002, '2026-10-30 03:00:00'),"
                + " (6011, 6011, 'PROFESSIONAL_REQUEST', 'Đã hủy trước migration', 'CANCELLED',"
                + " 9001, '2026-10-30 01:00:00', NULL, NULL)");
        db.update("INSERT INTO m07_job_run (job_key, checkpoint, result, completed_at)"
                + " VALUES ('RECONCILIATION_WARNING', '6001', 'sent', '2026-10-30 08:00:00')");
        db.update(
                "INSERT INTO medication (id, name, concentration, dosage_form, default_route, usage_unit, status)"
                        + " VALUES (?, 'Thuốc kiểm thử R15', '10 mg', 'TABLET', 'ORAL', 'tablet', 'ACTIVE')",
                medicationId);
        db.update(
                "INSERT INTO medication_active_ingredient (medication_id, allergen_id) VALUES (?, 701), (?, 702)",
                medicationId,
                medicationId);
        db.update(
                "INSERT INTO prescription (id, resident_id, medication_id, dosage, dosage_unit, route,"
                        + " prescription_type, frequency_type, timed_slots, started_at, medication_source, prescriber_type,"
                        + " prescriber_id, status, created_at) VALUES (?, 6001, ?, 1, 'tablet', 'ORAL', 'PERIODIC',"
                        + " 'TIMED_SLOTS', JSON_ARRAY('08:00'), '2026-10-30 00:00:00', 'FACILITY', 'INTERNAL', 9001,"
                        + " 'ACTIVE', '2026-10-30 00:00:00')",
                prescriptionId,
                medicationId);
        db.update(
                "INSERT INTO prescription_active_ingredient (prescription_id, allergen_id) VALUES (?, 701), (?, 702)",
                prescriptionId,
                prescriptionId);
        db.update(
                "INSERT INTO medication_reconciliation (id, resident_id, reason, reason_detail, status, created_by_id, created_at)"
                        + " VALUES (?, 6001, 'PROFESSIONAL_REQUEST', 'Kiểm thử R-15', 'OPEN', 9001, '2026-10-30 01:00:00')",
                reconciliationId);
        db.update(
                "INSERT INTO reconciliation_item (id, reconciliation_id, item_order, source, prescription_id, decision,"
                        + " reason, created_by_id, created_at) VALUES (?, ?, 1, 'CURRENT_PRESCRIPTION', ?, 'CONTINUE',"
                        + " 'Giữ đơn', 9001, '2026-10-30 01:01:00')",
                reconciliationItemId,
                reconciliationId,
                prescriptionId);
        db.update(
                "UPDATE reconciliation_item SET new_prescription_json = JSON_OBJECT('dosage', 2) WHERE id = ?",
                reconciliationItemId);
        db.update(
                "INSERT INTO family_medication_batch (id, resident_id, medication_id, initial_quantity, unit,"
                        + " has_prescription, deliverer_type, deliverer_name, receiver_id, received_at, status)"
                        + " VALUES (?, 6001, ?, 10, 'tablet', 1, 'FREE', 'Người giao', 9001,"
                        + " '2026-10-30 02:00:00', 'IN_USE')",
                familyBatchId,
                medicationId);
        db.update(
                "INSERT INTO family_medication_prescription (id, batch_id, prescription_id, bound_from)"
                        + " VALUES (?, ?, ?, '2026-10-30 02:01:00')",
                familyBindingId,
                familyBatchId,
                prescriptionId);

        // TIMESTAMP ghi theo múi giờ session của server; so thời điểm thực, không suy UTC từ chuỗi SQL.
        legacyBindingFrom = Instant.ofEpochSecond(db.queryForObject(
                "SELECT UNIX_TIMESTAMP(bound_from) FROM family_medication_prescription WHERE id = ?",
                Long.class,
                familyBindingId));
        legacyBatchReceivedAt = Instant.ofEpochSecond(db.queryForObject(
                "SELECT UNIX_TIMESTAMP(received_at) FROM family_medication_batch WHERE id = ?",
                Long.class,
                familyBatchId));

        Flyway.configure()
                .configuration(beforeConsolidation.getConfiguration())
                .target("6.95")
                .load()
                .migrate();
    }

    @AfterAll
    static void dropSchema() {
        dropScratchSchema(SUFFIX);
    }

    @Test
    void pendingTablesAreConsolidatedAndDropped() {
        List<String> tables = db.queryForList(
                "SELECT LOWER(TABLE_NAME) FROM information_schema.TABLES"
                        + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE'",
                String.class);
        assertThat(tables)
                .doesNotContain(
                        "resident_medication",
                        "m07_job_run",
                        "medication",
                        "medication_active_ingredient",
                        "prescription_active_ingredient",
                        "reconciliation_item",
                        "family_medication_prescription")
                .contains("catalog_item", "resident_module_state", "reminder_marker");
    }

    @Test
    void medicationAndIngredientsMoveToCatalogAndPrescriptionJson() {
        assertThat(db.queryForMap(
                        "SELECT JSON_UNQUOTE(attributes->'$.name') AS name, status,"
                                + " attributes->'$.activeIngredientIds' AS ingredients,"
                                + " attributes->'$.legacy.id' AS legacy_id FROM catalog_item"
                                + " WHERE catalog_type = 'M07.MEDICATION' AND attributes->'$.legacy.id' = ?",
                        medicationId))
                .containsEntry("name", "Thuốc kiểm thử R15")
                .containsEntry("status", "ACTIVE");
        assertThat(db.queryForObject(
                        "SELECT JSON_CONTAINS(active_ingredient_ids, '[701, 702]') FROM prescription WHERE id = ?",
                        Long.class,
                        prescriptionId))
                .isEqualTo(1L);
    }

    @Test
    void residentStateAndJobCheckpointMoveToSharedTables() {
        assertThat(db.queryForMap("SELECT m07_reconciliation_due_at AS deadline FROM resident_module_state"
                        + " WHERE resident_id = 6002 AND module = 'M07'"))
                .containsEntry("deadline", null);
        assertThat(db.queryForMap("SELECT version, m07_reconciliation_due_at AS generated_due_at,"
                        + " JSON_UNQUOTE(data->'$.reconciliationReason') AS reason"
                        + " FROM resident_module_state WHERE resident_id = 6001 AND module = 'M07'"))
                .containsEntry("version", 2)
                .containsKey("generated_due_at")
                .containsEntry("reason", "ADMISSION");
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM reminder_marker WHERE kind = 'M07.JOB.RECONCILIATION_WARNING'"
                                + " AND entity_type = 'M07.JOB' AND entity_id = 0 AND marker = '6001'",
                        Long.class))
                .isEqualTo(1L);
    }

    @Test
    void childRowsMoveToParentJsonColumns() {
        assertThat(db.queryForObject(
                        "SELECT JSON_UNQUOTE(`lines`->'$[0].reason') FROM medication_reconciliation WHERE id = ?",
                        String.class,
                        reconciliationId))
                .isEqualTo("Giữ đơn");
        assertThat(db.queryForObject(
                        "SELECT CAST(JSON_UNQUOTE(`lines`->'$[0].id') AS UNSIGNED)"
                                + " FROM medication_reconciliation WHERE id = ?",
                        Long.class,
                        reconciliationId))
                .isEqualTo(reconciliationItemId);
        assertThat(db.queryForObject(
                        "SELECT CAST(JSON_UNQUOTE(prescription_bindings->'$[0].prescriptionId') AS UNSIGNED)"
                                + " FROM family_medication_batch WHERE id = ?",
                        Long.class,
                        familyBatchId))
                .isEqualTo(prescriptionId);
    }

    @Test
    void T069_migratedJsonCanBeReadByTheDomain() throws Exception {
        var mapper = JsonMapper.builder().findAndAddModules().build();
        List<ReconciliationItem> lines = mapper.readValue(
                db.queryForObject(
                        "SELECT `lines` FROM medication_reconciliation WHERE id = ?", String.class, reconciliationId),
                new TypeReference<List<ReconciliationItem>>() {});
        assertThat(lines).hasSize(1);
        assertThat(lines.getFirst().getReconciliationId()).isEqualTo(reconciliationId);
        assertThat(lines.getFirst().getCreatedAt()).isEqualTo(Instant.parse("2026-10-30T01:01:00Z"));
        assertThat(mapper.readTree(lines.getFirst().getNewPrescriptionJson())
                        .path("dosage")
                        .asInt())
                .isEqualTo(2);
        List<FamilyMedicationPrescription> bindings = mapper.readValue(
                db.queryForObject(
                        "SELECT prescription_bindings FROM family_medication_batch WHERE id = ?",
                        String.class,
                        familyBatchId),
                new TypeReference<List<FamilyMedicationPrescription>>() {});
        assertThat(bindings).hasSize(1);
        assertThat(bindings.getFirst().getBoundFrom()).isEqualTo(legacyBindingFrom);
        assertThat(bindings.getFirst().getBoundTo()).isNull();
        assertThat(Instant.parse(db.queryForObject(
                        "SELECT DATE_FORMAT(received_at, '%Y-%m-%dT%H:%i:%s.%fZ') FROM family_medication_batch WHERE id = ?",
                        String.class, familyBatchId)))
                .isEqualTo(legacyBatchReceivedAt);
    }

    @Test
    void F_01_confirmedReconciliationCannotBypassTheRowLock() {
        db.update("INSERT INTO medication_reconciliation (id, resident_id, reason, reason_detail, status,"
                + " created_by_id, created_at, `lines`) VALUES (6008, 6008, 'PROFESSIONAL_REQUEST',"
                + " 'F-01', 'OPEN', 9001, '2026-10-30 01:00:00', JSON_ARRAY())");
        assertThatThrownBy(() -> db.update("UPDATE medication_reconciliation SET status = 'CONFIRMED',"
                        + " confirmed_by_id = 9002, confirmed_at = '2026-10-30 03:00:00' WHERE id = 6008"))
                .isInstanceOf(DataAccessException.class);
        db.update("UPDATE medication_reconciliation SET status = 'CONFIRMED', confirmed_by_id = 9002,"
                + " confirmed_at = '2026-10-30 03:00:00', locked_at = '2026-10-30 03:00:00' WHERE id = 6008");
        assertThatThrownBy(
                        () -> db.update("UPDATE medication_reconciliation SET `lines` = JSON_ARRAY() WHERE id = 6008"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> db.update("DELETE FROM medication_reconciliation WHERE id = 6008"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void F_01_legacyFinalTicketsKeepTheirStateAndLock() {
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM medication_reconciliation WHERE id = 6010"
                                + " AND status = 'CONFIRMED' AND locked_at = confirmed_at AND JSON_LENGTH(`lines`) = 0",
                        Long.class))
                .isEqualTo(1L);
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM medication_reconciliation WHERE id = 6011"
                                + " AND status = 'CANCELLED' AND locked_at IS NULL AND JSON_LENGTH(`lines`) = 0",
                        Long.class))
                .isEqualTo(1L);
        assertThatThrownBy(() ->
                        db.update("UPDATE medication_reconciliation SET reason_detail = 'Đổi sau hủy' WHERE id = 6011"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> db.update("DELETE FROM medication_reconciliation WHERE id = 6011"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void F_01_terminalHandoverCannotBeInsertedWithoutALock() {
        assertThatThrownBy(() -> db.update("INSERT INTO medication_handover (resident_id, recipient_type,"
                        + " dispensed_by_id, handover_time, status, created_at) VALUES (6009, 'RELATIVE', 9001,"
                        + " '2026-10-30 03:00:00', 'RETURNED', '2026-10-30 03:00:00')"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void F_03_medicationTablesNoLongerUseTimestamp3() {
        assertThat(db.queryForList(
                        "SELECT CONCAT(TABLE_NAME, '.', COLUMN_NAME) FROM information_schema.COLUMNS"
                                + " WHERE TABLE_SCHEMA = DATABASE() AND DATA_TYPE = 'timestamp'"
                                + " AND TABLE_NAME IN ('prn_administration', 'family_medication_batch',"
                                + " 'medication_absence_interval', 'medication_handover', 'medication_handover_dose',"
                                + " 'medication_return', 'no_medication_order', 'supplied_medication_intake')"
                                + " ORDER BY TABLE_NAME, ORDINAL_POSITION",
                        String.class))
                .isEmpty();
    }
}
