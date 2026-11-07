package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 006 T069: {@code V006_98} chuyển {@code prescription_issue}, {@code prescription_basis} sang {@code record_event} và
 * {@code dose_review}, {@code medication_absence_interval}, {@code reconciliation_correction_request} sang
 * {@code simple_record} (data-model 006 mục 0.1). Dòng cấp id mới, ghi {@code content.legacy}; khóa tên trường JSON khớp
 * record nội dung của module để đọc lại được.
 */
class SharedRecordsMigrationIT extends MySqlIntegrationTestBase {

    private static final String SUFFIX = "m006_98";
    private static final long MEDICATION_ID = 6_301L;
    private static final long PRESCRIPTION_ID = 6_302L;
    private static final long DOSE_ID = 6_303L;
    private static final long DOSE_RESULT_ID = 6_304L;
    private static final long RECONCILIATION_ID = 6_305L;
    private static final long OPEN_ABSENCE_ID = 6_306L;
    private static final long HANDOVER_ID = 6_307L;

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
                        + " VALUES (?, 'Thuốc bảng chung R15', '1 mg', 'TABLET', 'ORAL', 'tablet', 'ACTIVE')",
                MEDICATION_ID);
        db.update(
                "INSERT INTO prescription (id, resident_id, medication_id, dosage, dosage_unit, route,"
                        + " prescription_type, frequency_type, timed_slots, started_at, medication_source, prescriber_type,"
                        + " prescriber_id, status, created_at) VALUES (?, 6301, ?, 1, 'tablet', 'ORAL', 'PERIODIC',"
                        + " 'TIMED_SLOTS', JSON_ARRAY('08:00'), '2026-10-30 00:00:00', 'FACILITY', 'INTERNAL', 9001,"
                        + " 'ACTIVE', '2026-10-30 00:00:00')",
                PRESCRIPTION_ID,
                MEDICATION_ID);
        db.update(
                "INSERT INTO medication_dose (id, prescription_id, resident_id, medication_id, scheduled_time,"
                        + " window_from, window_to, dosage, dosage_unit, medication_source) VALUES (?, ?, 6301, ?,"
                        + " '2026-10-30 01:00:00', '2026-10-30 00:30:00', '2026-10-30 01:30:00', 1, 'tablet', 'FACILITY')",
                DOSE_ID,
                PRESCRIPTION_ID,
                MEDICATION_ID);
        db.update(
                "INSERT INTO dose_result (id, dose_id, resident_id, result_type, administered_at, recorded_at,"
                        + " recorder_id, authority_basis) VALUES (?, ?, 6301, 'ADMINISTERED', '2026-10-30 01:05:00',"
                        + " '2026-10-30 01:06:00', 9001, 'ASSIGNED')",
                DOSE_RESULT_ID,
                DOSE_ID);
        db.update(
                "INSERT INTO prescription_issue (prescription_id, issue_type, issue_code, allergy_item_id, severity,"
                        + " manual_review_required, confirmation_reason, confirmed_by_id, occurred_at) VALUES (?,"
                        + " 'ALLERGY_CONFLICT', 'ALLERGY', 41, 'NANG', 1, 'Đã cân nhắc', 9001, '2026-10-30 00:00:00')",
                PRESCRIPTION_ID);
        db.update(
                "INSERT INTO prescription_basis (prescription_id, command, facility_name, prescriber_name,"
                        + " prescribed_date, basis_type, document_file_id, recorder_id, recorded_at) VALUES (?, 'EXTEND',"
                        + " 'Bệnh viện A', 'Bác sĩ B', '2026-10-29', 'NEW_PRESCRIPTION', 55, 9001,"
                        + " '2026-10-30 02:00:00')",
                PRESCRIPTION_ID);
        db.update(
                "INSERT INTO dose_review (dose_result_id, floor_area_id, reviewer_candidate, status, note, version,"
                        + " created_at) VALUES (?, 11, JSON_ARRAY(9002), 'PENDING_REVIEW', 'Ghi sau mất kết nối', 2,"
                        + " '2026-10-30 01:10:00')",
                DOSE_RESULT_ID);
        db.update(
                "INSERT INTO medication_absence_interval (id, resident_id, absence_type, from_time, expected_to_time,"
                        + " trip_id) VALUES (?, 6301, 'TRIP_WITH_MED', '2026-10-30 03:00:00', '2026-10-30 09:00:00', 77)",
                OPEN_ABSENCE_ID);
        db.update("INSERT INTO medication_absence_interval (resident_id, absence_type, from_time, actual_to_time)"
                + " VALUES (6301, 'HOSPITAL', '2026-10-20 03:00:00', '2026-10-21 03:00:00')");
        db.update(
                "INSERT INTO medication_handover (id, resident_id, absence_interval_id, trip_id, recipient_type,"
                        + " dispensed_by_id, handover_time, status) VALUES (?, 6301, ?, 77, 'STAFF', 9002,"
                        + " '2026-10-30 03:00:00', 'HANDED_OVER')",
                HANDOVER_ID,
                OPEN_ABSENCE_ID);
        db.update(
                "INSERT INTO medication_reconciliation (id, resident_id, reason, reason_detail, status, created_by_id,"
                        + " created_at, confirmed_by_id, confirmed_at) VALUES (?, 6301, 'PROFESSIONAL_REQUEST', 'Phiếu',"
                        + " 'CONFIRMED', 9001, '2026-10-30 01:00:00', 9002, '2026-10-30 02:00:00')",
                RECONCILIATION_ID);
        db.update(
                "INSERT INTO reconciliation_correction_request (reconciliation_id, corrected_content_json, reason,"
                        + " created_by_id, status) VALUES (?, JSON_OBJECT('note', 'đúng'), 'Sai dòng', 9003,"
                        + " 'PENDING_CONFIRMATION')",
                RECONCILIATION_ID);

        Flyway.configure()
                .configuration(before.getConfiguration())
                .target("6.98")
                .load()
                .migrate();
    }

    @AfterAll
    static void dropSchema() {
        dropScratchSchema(SUFFIX);
    }

    private static Map<String, Object> simple(String kind) {
        return db.queryForMap("SELECT * FROM simple_record WHERE kind = ? ORDER BY id LIMIT 1", kind);
    }

    private static List<String> keys(String table, String kind) {
        return db.queryForList(
                "SELECT j.k FROM " + table + " t, JSON_TABLE(JSON_KEYS(t.content), '$[*]' COLUMNS (k VARCHAR(64)"
                        + " PATH '$')) j WHERE t.kind = ? GROUP BY j.k",
                String.class,
                kind);
    }

    @Test
    void T069_oldTablesAreDropped() {
        assertThat(db.queryForList(
                        "SELECT LOWER(TABLE_NAME) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()",
                        String.class))
                .doesNotContain(
                        "prescription_issue",
                        "prescription_basis",
                        "dose_review",
                        "medication_absence_interval",
                        "reconciliation_correction_request");
    }

    @Test
    void T069_issuesAndBasesBecomeConfirmedEventsOfThePrescription() {
        Map<String, Object> issue =
                db.queryForMap("SELECT * FROM record_event WHERE kind = 'M07.PRESCRIPTION_ISSUE' ORDER BY id LIMIT 1");
        assertThat(issue.get("entity_type")).isEqualTo("M07.PRESCRIPTION");
        assertThat(((Number) issue.get("entity_id")).longValue()).isEqualTo(PRESCRIPTION_ID);
        assertThat(issue.get("confirmed_at")).isNotNull();
        assertThat(keys("record_event", "M07.PRESCRIPTION_ISSUE"))
                .containsExactlyInAnyOrder(
                        "prescriptionId",
                        "reconciliationLineId",
                        "lineNumber",
                        "issueType",
                        "issueCode",
                        "duplicatePrescriptionId",
                        "allergyItemId",
                        "severity",
                        "activeIngredient",
                        "manualReviewRequired",
                        "confirmationReason",
                        "confirmedById",
                        "occurredAt",
                        "legacy");

        Map<String, Object> basis =
                db.queryForMap("SELECT * FROM record_event WHERE kind = 'M07.PRESCRIPTION_BASIS' ORDER BY id LIMIT 1");
        assertThat(((Number) basis.get("actor_id")).longValue()).isEqualTo(9001L);
        assertThat(db.queryForObject(
                        "SELECT JSON_UNQUOTE(content->'$.prescribedDate') FROM record_event WHERE id = ?",
                        String.class,
                        basis.get("id")))
                .isEqualTo("2026-10-29");
        assertThat(keys("record_event", "M07.PRESCRIPTION_BASIS")).contains("documentFileId", "recordedAt", "legacy");
    }

    @Test
    void T069_doseReviewKeepsStatusVersionAndUniqueKey() {
        Map<String, Object> review = simple("M07.DOSE_REVIEW");

        assertThat(review.get("status")).isEqualTo("PENDING_REVIEW");
        assertThat(((Number) review.get("version")).intValue()).isEqualTo(2);
        assertThat(review.get("unique_key")).isEqualTo("M07.DR:" + DOSE_RESULT_ID);
        assertThat(review.get("entity_type")).isEqualTo("M07.DOSE_RESULT");
        assertThat(db.queryForObject(
                        "SELECT JSON_UNQUOTE(content->'$.reviewerCandidate') FROM simple_record WHERE id = ?",
                        String.class,
                        review.get("id")))
                .isEqualTo("[9002]");
        assertThat(keys("simple_record", "M07.DOSE_REVIEW"))
                .containsExactlyInAnyOrder(
                        "doseResultId",
                        "prnUsageId",
                        "floorAreaId",
                        "reviewerCandidate",
                        "reviewerId",
                        "reviewedAt",
                        "note",
                        "legacy");
    }

    @Test
    void T069_absenceIntervalsKeepOpenKeyAndHandoverFollowsNewId() {
        List<Map<String, Object>> rows = db.queryForList(
                "SELECT id, status, unique_key, CAST(content->'$.legacy.id' AS UNSIGNED) AS legacy_id FROM simple_record"
                        + " WHERE kind = 'M07.ABSENCE_INTERVAL' AND resident_id = 6301 ORDER BY id");

        assertThat(rows).hasSize(2);
        Map<String, Object> open = rows.stream()
                .filter(r -> "OPEN".equals(r.get("status")))
                .findFirst()
                .orElseThrow();
        assertThat(open.get("unique_key")).isEqualTo("M07.AI:6301");
        assertThat(rows.stream()
                        .filter(r -> "CLOSED".equals(r.get("status")))
                        .findFirst()
                        .orElseThrow())
                .containsEntry("unique_key", null);
        assertThat(db.queryForObject(
                        "SELECT absence_interval_id FROM medication_handover WHERE id = ?", Long.class, HANDOVER_ID))
                .isEqualTo(((Number) open.get("id")).longValue());
    }

    @Test
    void T069_correctionRequestMovesWithItsCreator() {
        Map<String, Object> request = simple("M07.RECONCILIATION_CORRECTION_REQUEST");

        assertThat(request.get("status")).isEqualTo("PENDING_CONFIRMATION");
        assertThat(((Number) request.get("created_by")).longValue()).isEqualTo(9003L);
        assertThat(((Number) request.get("entity_id")).longValue()).isEqualTo(RECONCILIATION_ID);
        assertThat(db.queryForObject(
                        "SELECT JSON_UNQUOTE(content->'$.correctedContentJson') FROM simple_record WHERE id = ?",
                        String.class,
                        request.get("id")))
                .isEqualTo("{\"note\": \"đúng\"}");
    }
}
