package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 006 T069: {@code V006_97} gộp sáu bảng giao nhận vào {@code medication_handover} (data-model 006 mục 0.1). Lần giao giữ
 * id; dòng thuốc giao, liều mang theo, dòng nhận lại vào cột {@code lines}; nhận lại, lệnh không mang thuốc, tiếp nhận
 * cung ứng cấp id mới sau id lớn nhất của cả bốn bảng, ghi {@code legacy} (A chốt 2026-11-05, như {@code V001_95}).
 */
class HandoverConsolidationMigrationIT extends MySqlIntegrationTestBase {

    private static final String SUFFIX = "m006_97";
    private static final long MEDICATION_ID = 6_201L;
    private static final long PRESCRIPTION_ID = 6_202L;
    private static final long DOSE_ID = 6_203L;
    private static final long TRIP_ID = 6_204L;
    /** Lần giao đang mở và id trùng nhau ở mọi bảng nguồn. */
    private static final long SHARED_ID = 6_250L;
    /** Lần giao đã nhận lại. */
    private static final long RETURNED_HANDOVER_ID = 6_251L;
    /** Lần tiếp nhận cung ứng có id lớn nhất. */
    private static final long HIGH_INTAKE_ID = 6_290L;

    private static JdbcTemplate db;
    private static long catalogMedicationId;

    @BeforeAll
    static void migrate() {
        Flyway before = Flyway.configure()
                .configuration(scratchFlyway(SUFFIX).getConfiguration())
                .target("6.15")
                .load();
        before.migrate();
        db = new JdbcTemplate(before.getConfiguration().getDataSource());

        db.update(
                "INSERT INTO medication (id, name, concentration, dosage_form, default_route, usage_unit, status)"
                        + " VALUES (?, 'Thuốc giao nhận R15', '5 mg', 'TABLET', 'ORAL', 'tablet', 'ACTIVE')",
                MEDICATION_ID);
        db.update(
                "INSERT INTO prescription (id, resident_id, medication_id, dosage, dosage_unit, route,"
                        + " prescription_type, frequency_type, timed_slots, started_at, medication_source, prescriber_type,"
                        + " prescriber_id, status, created_at) VALUES (?, 6201, ?, 1, 'tablet', 'ORAL', 'PERIODIC',"
                        + " 'TIMED_SLOTS', JSON_ARRAY('08:00'), '2026-10-30 00:00:00', 'FACILITY', 'INTERNAL', 9001,"
                        + " 'ACTIVE', '2026-10-30 00:00:00')",
                PRESCRIPTION_ID,
                MEDICATION_ID);
        db.update(
                "INSERT INTO medication_dose (id, prescription_id, resident_id, medication_id, scheduled_time,"
                        + " window_from, window_to, dosage, dosage_unit, medication_source) VALUES (?, ?, 6201, ?,"
                        + " '2026-10-30 01:00:00', '2026-10-30 00:30:00', '2026-10-30 01:30:00', 1, 'tablet', 'FACILITY')",
                DOSE_ID,
                PRESCRIPTION_ID,
                MEDICATION_ID);
        insertHandover(SHARED_ID, "HANDED_OVER");
        insertHandover(RETURNED_HANDOVER_ID, "RETURNED");
        db.update(
                "INSERT INTO medication_handover_item (id, handover_id, prescription_id, quantity,"
                        + " family_medication_batch_id, medication_source) VALUES (?, ?, ?, 2, NULL, 'FACILITY'),"
                        + " (?, ?, ?, 3, 77, 'FAMILY_SENT')",
                SHARED_ID,
                SHARED_ID,
                PRESCRIPTION_ID,
                SHARED_ID + 1,
                SHARED_ID,
                PRESCRIPTION_ID);
        db.update(
                "INSERT INTO medication_handover_dose (id, handover_id, dose_id) VALUES (?, ?, ?)",
                SHARED_ID,
                SHARED_ID,
                DOSE_ID);
        db.update(
                "INSERT INTO medication_return (id, handover_id, recorded_by_id, return_time, reason, created_at)"
                        + " VALUES (?, ?, 9003, '2026-10-30 10:00:00', 'Về sớm', '2026-10-30 10:00:01')",
                SHARED_ID,
                RETURNED_HANDOVER_ID);
        db.update(
                "INSERT INTO medication_return_item (id, return_id, prescription_id, quantity) VALUES (?, ?, ?, 1)",
                SHARED_ID,
                SHARED_ID,
                PRESCRIPTION_ID);
        db.update(
                "INSERT INTO no_medication_order (id, resident_id, trip_id, reason, created_by_id, created_at)"
                        + " VALUES (?, 6201, ?, 'Không cần thuốc', 9004, '2026-10-30 05:00:00')",
                SHARED_ID,
                TRIP_ID);
        db.update(
                "INSERT INTO medication_absence_interval (resident_id, absence_type, from_time, trip_id,"
                        + " no_medication_order_id) VALUES (6201, 'TRIP_NO_MED', '2026-10-30 05:00:00', ?, ?)",
                TRIP_ID,
                SHARED_ID);
        // V006_95 đổi medication_id của tiếp nhận cung ứng bằng UPDATE mà trigger chỉ-thêm chặn: dòng tiếp nhận chỉ có
        // sau mốc đó (plan 006 mục "T069: id mới sau gộp").
        Flyway.configure()
                .configuration(before.getConfiguration())
                .target("6.95")
                .load()
                .migrate();
        catalogMedicationId = db.queryForObject(
                "SELECT id FROM catalog_item WHERE catalog_type = 'M07.MEDICATION'"
                        + " AND CAST(JSON_UNQUOTE(attributes->'$.legacy.id') AS UNSIGNED) = ?",
                Long.class,
                MEDICATION_ID);
        insertIntake(SHARED_ID);
        insertIntake(HIGH_INTAKE_ID);

        Flyway.configure()
                .configuration(before.getConfiguration())
                .target("6.97")
                .load()
                .migrate();
    }

    private static void insertHandover(long id, String status) {
        db.update(
                "INSERT INTO medication_handover (id, resident_id, trip_id, recipient_type, recipient_name,"
                        + " relationship, dispensed_by_id, handover_time, status, created_at) VALUES (?, 6201, ?,"
                        + " 'RELATIVE', 'Người nhận', 'Con', 9002, '2026-10-30 04:00:00', ?, '2026-10-30 04:00:01')",
                id,
                TRIP_ID,
                status);
    }

    private static void insertIntake(long id) {
        db.update(
                "INSERT INTO supplied_medication_intake (id, procurement_line_id, procurement_request_id, resident_id,"
                        + " prescription_id, medication_id, medication_name, concentration, dosage_form,"
                        + " quantity_received, unit, expiry_date, item_condition, intake_result, discrepancy, reason,"
                        + " deliverer_name, receiver_id, received_at, created_at) VALUES (?, 801, 802, 6201, ?, ?,"
                        + " 'Thuốc giao nhận R15', '5 mg', 'TABLET', 30, 'tablet', '2027-01-31', 'Nguyên vẹn',"
                        + " 'NOT_PASSED', 'Thiếu 2 viên', 'Đổi lại', 'Người giao', 9005, '2026-10-30 06:00:00',"
                        + " '2026-10-30 06:00:01')",
                id,
                PRESCRIPTION_ID,
                catalogMedicationId);
    }

    @AfterAll
    static void dropSchema() {
        dropScratchSchema(SUFFIX);
    }

    private static Map<String, Object> migrated(String kind, String table, long legacyId) {
        return db.queryForMap(
                "SELECT * FROM medication_handover WHERE kind = ?"
                        + " AND JSON_UNQUOTE(legacy->'$.table') = ? AND CAST(legacy->'$.id' AS UNSIGNED) = ?",
                kind,
                table,
                legacyId);
    }

    private static long id(Map<String, Object> row) {
        return ((Number) row.get("id")).longValue();
    }

    @Test
    void T069_childTablesAreDropped() {
        assertThat(db.queryForList(
                        "SELECT LOWER(TABLE_NAME) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()",
                        String.class))
                .doesNotContain(
                        "medication_handover_item",
                        "medication_handover_dose",
                        "medication_return",
                        "medication_return_item",
                        "no_medication_order",
                        "supplied_medication_intake")
                .contains("medication_handover");
    }

    @Test
    void T069_handoverKeepsIdAndCarriesItemsAndDosesInLines() {
        Map<String, Object> row = db.queryForMap("SELECT * FROM medication_handover WHERE id = ?", SHARED_ID);

        assertThat(row.get("kind")).isEqualTo("HANDOVER");
        assertThat(row.get("legacy")).isNull();
        assertThat(db.queryForList(
                        "SELECT CAST(j.id AS SIGNED) FROM medication_handover h, JSON_TABLE(h.`lines`, '$.items[*]'"
                                + " COLUMNS (id BIGINT PATH '$.id')) j WHERE h.id = ? ORDER BY j.id",
                        Long.class,
                        SHARED_ID))
                .containsExactly(SHARED_ID, SHARED_ID + 1);
        assertThat(db.queryForObject(
                        "SELECT JSON_UNQUOTE(`lines`->'$.items[1].familyMedicationBatchId') FROM medication_handover"
                                + " WHERE id = ?",
                        String.class,
                        SHARED_ID))
                .isEqualTo("77");
        assertThat(db.queryForObject(
                        "SELECT id FROM medication_handover WHERE kind = 'HANDOVER'"
                                + " AND ? MEMBER OF (`lines`->'$.doses[*].doseId')",
                        Long.class,
                        DOSE_ID))
                .isEqualTo(SHARED_ID);
    }

    @Test
    void T069_returnGetsNewIdLegacyItemsAndLock() {
        Map<String, Object> row = migrated("RETURN", "medication_return", SHARED_ID);

        assertThat(id(row)).isGreaterThan(HIGH_INTAKE_ID);
        assertThat(((Number) row.get("handover_id")).longValue()).isEqualTo(RETURNED_HANDOVER_ID);
        assertThat(((Number) row.get("resident_id")).longValue()).isEqualTo(6201L);
        assertThat(((Number) row.get("recorded_by_id")).longValue()).isEqualTo(9003L);
        assertThat(row.get("reason")).isEqualTo("Về sớm");
        assertThat(row.get("locked_at")).isNotNull();
        assertThat(db.queryForObject(
                        "SELECT JSON_LENGTH(`lines`->'$.items') FROM medication_handover WHERE id = ?",
                        Integer.class,
                        id(row)))
                .isEqualTo(1);
    }

    @Test
    void T069_noMedicationOrderGetsNewIdAndAbsenceFollowsIt() {
        Map<String, Object> row = migrated("NO_MEDICATION", "no_medication_order", SHARED_ID);

        assertThat(id(row)).isGreaterThan(HIGH_INTAKE_ID);
        assertThat(row.get("reason")).isEqualTo("Không cần thuốc");
        assertThat(((Number) row.get("created_by_id")).longValue()).isEqualTo(9004L);
        assertThat(row.get("locked_at")).isNotNull();
        assertThat(db.queryForObject(
                        "SELECT no_medication_order_id FROM medication_absence_interval WHERE trip_id = ?",
                        Long.class,
                        TRIP_ID))
                .isEqualTo(id(row));
    }

    @Test
    void T069_suppliedIntakeGetsNewIdAboveEveryOldIdAndKeepsAllFields() {
        Map<String, Object> row = migrated("SUPPLIED_INTAKE", "supplied_medication_intake", SHARED_ID);

        assertThat(id(row)).isGreaterThan(HIGH_INTAKE_ID);
        assertThat(((Number) row.get("procurement_line_id")).longValue()).isEqualTo(801L);
        assertThat(((Number) row.get("procurement_request_id")).longValue()).isEqualTo(802L);
        assertThat(((Number) row.get("prescription_id")).longValue()).isEqualTo(PRESCRIPTION_ID);
        assertThat(row.get("medication_name")).isEqualTo("Thuốc giao nhận R15");
        assertThat(row.get("quantity_received").toString()).isEqualTo("30.00");
        assertThat(row.get("intake_result")).isEqualTo("NOT_PASSED");
        assertThat(row.get("discrepancy")).isEqualTo("Thiếu 2 viên");
        assertThat(row.get("reason")).isEqualTo("Đổi lại");
        assertThat(row.get("deliverer_name")).isEqualTo("Người giao");
        assertThat(row.get("locked_at")).isNotNull();
        List<Long> intakeIds =
                db.queryForList("SELECT id FROM medication_handover WHERE kind = 'SUPPLIED_INTAKE'", Long.class);
        assertThat(intakeIds).hasSize(2).doesNotContain(SHARED_ID, HIGH_INTAKE_ID);
    }

    @Test
    void T069_uniqueKeysAndLocksStillHold() {
        assertThatThrownBy(() -> db.update(
                        "INSERT INTO medication_handover (kind, resident_id, trip_id, reason, created_by_id, created_at,"
                                + " locked_at) VALUES ('NO_MEDICATION', 6201, ?, 'Trùng', 9004, NOW(6), NOW(6))",
                        TRIP_ID))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> db.update(
                        "INSERT INTO medication_handover (kind, resident_id, handover_id, recorded_by_id, return_time,"
                                + " created_at, locked_at) VALUES ('RETURN', 6201, ?, 9003, NOW(6), NOW(6), NOW(6))",
                        RETURNED_HANDOVER_ID))
                .isInstanceOf(DataAccessException.class);
        long returnId = id(migrated("RETURN", "medication_return", SHARED_ID));
        assertThatThrownBy(() -> db.update("UPDATE medication_handover SET reason = 'x' WHERE id = ?", returnId))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> db.update(
                        "INSERT INTO medication_handover (kind, resident_id, trip_id, reason, created_by_id, created_at)"
                                + " VALUES ('NO_MEDICATION', 6201, 6299, 'Không khóa', 9004, NOW(6))"))
                .isInstanceOf(DataAccessException.class);
    }
}
