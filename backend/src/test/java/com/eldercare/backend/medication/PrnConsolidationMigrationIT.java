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
 * 006 T069: {@code V006_96} gộp {@code prn_administration} vào {@code dose_result} (data-model 006 mục 0.1). Theo quyết
 * định của A 2026-11-05 (như {@code V001_95}): dòng PRN cấp id mới, ghi {@code legacy = {table, id}}; nhật ký, đính chính
 * cũ không nối sang id mới, và id mới không được trùng id PRN cũ để lịch sử cũ không gắn nhầm.
 */
class PrnConsolidationMigrationIT extends MySqlIntegrationTestBase {

    private static final String SUFFIX = "m006_96";
    private static final long MEDICATION_ID = 6_101L;
    private static final long PRESCRIPTION_ID = 6_102L;
    private static final long DOSE_ID = 6_103L;
    /** Kết quả liều thường và lần dùng PRN cũ có cùng id. */
    private static final long SHARED_ID = 6_150L;
    /** Lần dùng PRN cũ có id lớn hơn mọi kết quả liều. */
    private static final long HIGH_PRN_ID = 6_190L;

    private static JdbcTemplate db;

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
                        + " VALUES (?, 'Thuốc PRN R15', '500 mg', 'TABLET', 'ORAL', 'tablet', 'ACTIVE')",
                MEDICATION_ID);
        db.update(
                "INSERT INTO prescription (id, resident_id, medication_id, dosage, dosage_unit, route,"
                        + " prescription_type, frequency_type, timed_slots, started_at, medication_source, prescriber_type,"
                        + " prescriber_id, status, created_at) VALUES (?, 6101, ?, 1, 'tablet', 'ORAL', 'PERIODIC',"
                        + " 'TIMED_SLOTS', JSON_ARRAY('08:00'), '2026-10-30 00:00:00', 'FACILITY', 'INTERNAL', 9001,"
                        + " 'ACTIVE', '2026-10-30 00:00:00')",
                PRESCRIPTION_ID,
                MEDICATION_ID);
        db.update(
                "INSERT INTO medication_dose (id, prescription_id, resident_id, medication_id, scheduled_time,"
                        + " window_from, window_to, dosage, dosage_unit, medication_source) VALUES (?, ?, 6101, ?,"
                        + " '2026-10-30 01:00:00', '2026-10-30 00:30:00', '2026-10-30 01:30:00', 1, 'tablet', 'FACILITY')",
                DOSE_ID,
                PRESCRIPTION_ID,
                MEDICATION_ID);
        db.update(
                "INSERT INTO dose_result (id, dose_id, resident_id, result_type, administered_at, recorded_at,"
                        + " recorder_id, authority_basis) VALUES (?, ?, 6101, 'ADMINISTERED', '2026-10-30 01:05:00',"
                        + " '2026-10-30 01:06:00', 9001, 'ASSIGNED')",
                SHARED_ID,
                DOSE_ID);
        insertPrn(SHARED_ID, "Đau đầu", null);
        insertPrn(HIGH_PRN_ID, "Sốt", "2026-10-30 03:01:00");
        db.update(
                "INSERT INTO dose_review (prn_usage_id, floor_area_id, status) VALUES (?, 11, 'PENDING_REVIEW')",
                HIGH_PRN_ID);
        insertCorrection("M07.DOSE_RESULT", SHARED_ID, "{\"note\": \"Liều thường đã sửa\"}");
        insertCorrection("M07.PRN_ADMINISTRATION", SHARED_ID, "{\"note\": \"PRN đã sửa\"}");

        Flyway.configure()
                .configuration(before.getConfiguration())
                .target("6.96")
                .load()
                .migrate();
    }

    private static void insertPrn(long id, String reason, String offlineDeviceTime) {
        db.update(
                "INSERT INTO prn_administration (id, prescription_id, resident_id, floor_area_id, administered_at,"
                        + " dosage, dosage_unit, reason, adverse_reaction, note, administered_by_id, reported_by,"
                        + " exceeded_limit, offline_device_time, recorded_after_disconnect, created_at)"
                        + " VALUES (?, ?, 6101, 11, '2026-10-30 03:00:00', 1.5, 'tablet', ?, 'Buồn nôn', 'Ghi chú',"
                        + " 9002, 'Người báo', 1, ?, ?, '2026-10-30 03:02:00')",
                id,
                PRESCRIPTION_ID,
                reason,
                offlineDeviceTime,
                offlineDeviceTime != null);
    }

    private static void insertCorrection(String recordType, long originalId, String content) {
        db.update(
                "INSERT INTO correction (record_type, original_record_id, type, corrected_content, reason, actor_id,"
                        + " creation_method, recorded_at, sequence_no) VALUES (?, ?, 'AMEND_CONTENT', ?, 'Sửa', 9001,"
                        + " 'A', '2026-10-30 04:00:00', 1)",
                recordType,
                originalId,
                content);
    }

    @AfterAll
    static void dropSchema() {
        dropScratchSchema(SUFFIX);
    }

    private static Map<String, Object> prnRow(long legacyId) {
        return db.queryForMap(
                "SELECT * FROM dose_result WHERE kind = 'PRN'"
                        + " AND JSON_UNQUOTE(legacy->'$.table') = 'prn_administration'"
                        + " AND CAST(legacy->'$.id' AS UNSIGNED) = ?",
                legacyId);
    }

    @Test
    void T069_prnTableIsDropped() {
        assertThat(db.queryForList(
                        "SELECT LOWER(TABLE_NAME) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()",
                        String.class))
                .doesNotContain("prn_administration");
    }

    @Test
    void T069_prnRowsMoveToDoseResultWithLegacyAndAllColumns() {
        Map<String, Object> row = prnRow(HIGH_PRN_ID);

        assertThat(row.get("dose_id")).isNull();
        assertThat(((Number) row.get("prescription_id")).longValue()).isEqualTo(PRESCRIPTION_ID);
        assertThat(((Number) row.get("resident_id")).longValue()).isEqualTo(6101L);
        assertThat(((Number) row.get("floor_area_id")).longValue()).isEqualTo(11L);
        assertThat(row.get("result_type")).isEqualTo("ADMINISTERED");
        assertThat(row.get("dosage").toString()).isEqualTo("1.500");
        assertThat(row.get("dosage_unit")).isEqualTo("tablet");
        assertThat(row.get("reason")).isEqualTo("Sốt");
        assertThat(row.get("adverse_reaction")).isEqualTo("Buồn nôn");
        assertThat(row.get("note")).isEqualTo("Ghi chú");
        assertThat(((Number) row.get("recorder_id")).longValue()).isEqualTo(9002L);
        assertThat(row.get("reported_by_name")).isEqualTo("Người báo");
        assertThat(row.get("exceeded_limit")).isEqualTo(true);
        assertThat(row.get("device_recorded_at")).isNotNull();
        assertThat(row.get("offline_flag")).isEqualTo(true);
        assertThat(row.get("synced_after_permission_loss")).isEqualTo(true);
        assertThat(row.get("authority_basis")).isNull();
        assertThat(db.queryForObject("SELECT COUNT(*) FROM dose_result WHERE kind = 'PRN'", Long.class))
                .isEqualTo(2L);
    }

    @Test
    void T069_newPrnIdsDoNotReuseAnyOldPrnOrDoseResultId() {
        List<Long> newIds = db.queryForList("SELECT id FROM dose_result WHERE kind = 'PRN'", Long.class);

        assertThat(newIds).allSatisfy(id -> assertThat(id).isGreaterThan(HIGH_PRN_ID));
        db.update(
                "INSERT INTO dose_result (kind, prescription_id, resident_id, result_type, administered_at,"
                        + " recorded_at, recorder_id, dosage, dosage_unit, exceeded_limit, reason)"
                        + " VALUES ('PRN', ?, 6101, 'ADMINISTERED', '2026-10-31 01:00:00', '2026-10-31 01:00:00', 9002,"
                        + " 1, 'tablet', 0, 'Đau')",
                PRESCRIPTION_ID);
        Long next = db.queryForObject("SELECT MAX(id) FROM dose_result WHERE kind = 'PRN'", Long.class);
        assertThat(next).isGreaterThan(HIGH_PRN_ID);
    }

    @Test
    void T069_scheduledResultWithSameIdKeepsItsOwnHistory() {
        Map<String, Object> scheduled = db.queryForMap("SELECT kind, dose_id FROM dose_result WHERE id = ?", SHARED_ID);
        assertThat(scheduled.get("kind")).isEqualTo("SCHEDULED");
        assertThat(((Number) scheduled.get("dose_id")).longValue()).isEqualTo(DOSE_ID);

        // Đính chính cũ của liều thường giữ nguyên; đính chính cũ của PRN không nối sang dòng nào đang có loại PRN.
        assertThat(db.queryForList(
                        "SELECT corrected_content->>'$.note' FROM correction WHERE record_type = 'M07.DOSE_RESULT'"
                                + " AND original_record_id = ?",
                        String.class,
                        SHARED_ID))
                .containsExactly("Liều thường đã sửa");
        long newPrnId = ((Number) prnRow(SHARED_ID).get("id")).longValue();
        assertThat(newPrnId).isNotEqualTo(SHARED_ID);
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM correction WHERE record_type = 'M07.PRN_ADMINISTRATION'"
                                + " AND original_record_id = ?",
                        Long.class,
                        newPrnId))
                .isZero();
    }

    @Test
    void T069_doseReviewPointsToTheNewPrnId() {
        long newPrnId = ((Number) prnRow(HIGH_PRN_ID).get("id")).longValue();

        assertThat(db.queryForObject("SELECT COUNT(*) FROM dose_review WHERE prn_usage_id = ?", Long.class, newPrnId))
                .isEqualTo(1L);
    }

    @Test
    void T069_kindChecksAndAppendOnlyTriggersApplyToPrnRows() {
        assertThatThrownBy(() -> db.update(
                        "INSERT INTO dose_result (kind, resident_id, result_type, administered_at, recorded_at, dosage,"
                                + " dosage_unit, exceeded_limit) VALUES ('PRN', 6101, 'ADMINISTERED',"
                                + " '2026-10-31 01:00:00', '2026-10-31 01:00:00', 1, 'tablet', 0)"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> db.update(
                        "INSERT INTO dose_result (kind, resident_id, result_type, recorded_at, authority_basis)"
                                + " VALUES ('SCHEDULED', 6101, 'MISSED', '2026-10-31 01:00:00', 'SYSTEM')"))
                .isInstanceOf(DataAccessException.class);
        long prnId = ((Number) prnRow(HIGH_PRN_ID).get("id")).longValue();
        assertThatThrownBy(() -> db.update("UPDATE dose_result SET note = 'x' WHERE id = ?", prnId))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> db.update("DELETE FROM dose_result WHERE id = ?", prnId))
                .isInstanceOf(DataAccessException.class);
    }
}
