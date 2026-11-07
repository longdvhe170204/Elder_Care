package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 001 T084 nhóm 1 (research R-15 mục 5, data-model 001 mục 0.1): {@code V001_95} chép bốn danh mục
 * ({@code care_level}, {@code allergy_severity}, {@code information_source}, {@code care_need}) sang
 * {@code catalog_item}, đổi id ở {@code health_item}, {@code assessment_care_need} theo ánh xạ rồi mới xóa bảng cũ.
 * Hai kịch bản trên schema tạm: (A) cơ sở dữ liệu mới, migration tới {@code 1.10}, ghi dữ liệu cũ, chạy {@code 1.95};
 * (B) cơ sở dữ liệu đã áp dụng mọi migration khác (gồm 003 → 007) trừ {@code 1.95}, có dữ liệu dị nguyên của 001 và id dị
 * nguyên trong danh mục thuốc của 006, rồi chạy {@code 1.95} sau cùng: dị nguyên và tham chiếu của 006 không đổi.
 */
class CatalogConsolidationMigrationIT extends MySqlIntegrationTestBase {

    private static final String SUFFIX_A = "m001_95a";
    private static final String SUFFIX_B = "m001_95b";
    private static final String SUFFIX_C = "m001_95c";

    /** Dạng dữ liệu của 006 lúc V001_95 chạy (flyway.out-of-order): chưa có, bảng con trước V006_95, cột JSON sau V006_95. */
    private enum Stage006 {
        NONE,
        LINKS,
        JSON
    }

    private static final Path MIGRATIONS = Path.of("src", "main", "resources", "db", "migration");
    private static final List<String> OLD_TABLES = List.of(
            "care_level",
            "allergy_severity",
            "information_source",
            "care_need",
            "m01_catalog_id_map",
            "consent_family_member",
            "assessment_care_need",
            "periodic_reminder",
            "return_overdue_alert",
            "end_of_life_wish",
            "end_of_life_wish_opinion",
            "treatment_wish",
            "treatment_wish_notify",
            "treatment_wish_opinion",
            "resident_status_history",
            "care_level_history",
            "m01_history_id_map",
            "reassessment_notice",
            "initial_health_record",
            "m01_ihr_id_map",
            "allergen",
            "m01_allergen_id_map",
            "m01_prescription_link_copy");

    /** Nội dung một dòng lịch sử trạng thái cũ ({@code o}) và mới ({@code n}), để so tham chiếu theo nội dung khi id đổi. */
    private static final String OLD_STATUS_ROW =
            "CONCAT_WS('|', o.resident_id, IFNULL(o.from_status, '-'), o.to_status,"
                    + " o.command, o.occurred_at, o.performed_by, o.by_system, IFNULL(o.reason, '-'), IFNULL(o.basis_type, '-'),"
                    + " IFNULL(o.basis_id, '-'), IFNULL(CAST(o.condition_waiver AS CHAR), '-'))";

    private static final String NEW_STATUS_ROW = "CONCAT_WS('|', n.entity_id, IFNULL(n.value_before, '-'), n.value,"
            + " n.command, n.starts_at, n.actor_id, n.by_system, IFNULL(n.reason, '-'), IFNULL(n.basis_type, '-'),"
            + " IFNULL(n.basis_id, '-'), IF(JSON_TYPE(n.details -> '$.conditionWaiver') IS NULL"
            + " OR JSON_TYPE(n.details -> '$.conditionWaiver') = 'NULL', '-', CAST(n.details -> '$.conditionWaiver' AS CHAR)))";

    private static final String OLD_STATUS = "resident_status_history o";
    private static final String NEW_STATUS = "status_history n";
    private static final String NEW_STATUS_ON = " AND n.entity_type = 'M01.RESIDENT' AND n.attribute = 'STATUS'";

    /** Dữ liệu cũ đọc ra dạng tên, để so sau khi id đổi. */
    private record Snapshot(
            Map<Long, String> severityNameByItem,
            Map<Long, String> sourceNameByItem,
            Map<Long, Set<String>> careNeedNamesByAssessment,
            List<String> catalogRows,
            int healthItems,
            int assessmentCareNeeds,
            Map<Long, List<Long>> consentMembers,
            List<String> reminders,
            List<String> alerts,
            List<String> wishes,
            int treatmentFiles,
            List<String> statusRows,
            List<String> careLevelRows,
            List<String> historyRefs,
            long maxStatusHistoryId,
            List<String> notices,
            long maxNoticeId,
            List<String> healthRecords,
            List<String> healthRecordRefs,
            long maxHealthRecordId,
            Map<Long, String> allergenNameByItem) {}

    @AfterAll
    static void dropScratchSchemas() {
        dropScratchSchema(SUFFIX_A);
        dropScratchSchema(SUFFIX_B);
        dropScratchSchema(SUFFIX_C);
    }

    @Test
    void freshDatabase_oldRowsMoveToCatalogItemWithMappedIds() {
        Flyway upTo110 = Flyway.configure()
                .configuration(scratchFlyway(SUFFIX_A).getConfiguration())
                .target("1.10")
                .load();
        upTo110.migrate();
        JdbcTemplate db = new JdbcTemplate(upTo110.getConfiguration().getDataSource());
        seedOldData(db, Stage006.NONE);
        Snapshot before = snapshot(db);

        Flyway.configure()
                .configuration(upTo110.getConfiguration())
                .target("latest")
                .load()
                .migrate();

        assertMigrated(db, before);
    }

    @Test
    void databaseWith006Data_isMigratedOutOfOrder_andRemapsAllergenIdsKeptBy006() throws IOException {
        Flyway base = scratchFlyway(SUFFIX_B);
        Flyway applied = Flyway.configure()
                .configuration(base.getConfiguration())
                .locations("filesystem:" + withoutV001_95("v001_95_b").toAbsolutePath())
                .load();
        applied.migrate();
        JdbcTemplate db = new JdbcTemplate(applied.getConfiguration().getDataSource());
        seedOldData(db, Stage006.JSON);
        Snapshot before = snapshot(db);
        long allergen = penicillin(db);
        int catalogBefore = db.queryForObject("SELECT COUNT(*) FROM catalog_item", Integer.class);
        String lockTrigger = prescriptionLockTrigger(db);
        assertThat(medicationIngredients(db)).containsExactly(allergen);
        assertThat(prescriptionIngredients(db)).containsExactly(allergen);

        // Sau mọi migration khác: chạy riêng V001_95 theo thứ tự muộn (out-of-order), trường hợp (3)
        Flyway.configure()
                .configuration(base.getConfiguration())
                .outOfOrder(true)
                .load()
                .migrate();

        assertMigrated(db, before);
        long mapped = mappedAllergen(db, allergen);
        assertThat(mapped).isNotEqualTo(allergen);
        assertThat(medicationIngredients(db))
                .as("activeIngredientIds của M07.MEDICATION")
                .containsExactly(mapped);
        assertThat(db.queryForObject(
                        "SELECT version FROM catalog_item WHERE catalog_type = 'M07.MEDICATION' AND code = 'M07-test'",
                        Integer.class))
                .as("đổi attributes thì tăng version")
                .isEqualTo(1);
        assertThat(prescriptionIngredients(db))
                .as("prescription.active_ingredient_ids")
                .containsExactly(mapped);
        assertThat(db.queryForObject(
                        "SELECT CONCAT(DATA_TYPE, '|', IS_NULLABLE) FROM information_schema.COLUMNS"
                                + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'prescription'"
                                + " AND COLUMN_NAME = 'active_ingredient_ids'",
                        String.class))
                .isEqualTo("json|NO");
        assertThat(prescriptionLockTrigger(db))
                .as("trg_prescription_lock nguyên văn")
                .isEqualTo(lockTrigger);
        assertThatThrownBy(() -> db.update("UPDATE prescription SET active_ingredient_ids = JSON_ARRAY()"))
                .hasMessageContaining("Fixed fields of saved prescription cannot be modified");
        assertThat(db.queryForObject("SELECT COUNT(*) FROM catalog_item", Integer.class))
                .isEqualTo(catalogBefore + before.catalogRows().size());
    }

    /** Trường hợp (2): cơ sở dữ liệu dừng ở 006 trước V006_95; V001_95 rồi V006_95 chạy cùng lượt sau. */
    @Test
    void databaseWithOld006LinkTables_remapsLinks_thenV006_95CopiesTheNewIds() throws IOException {
        Flyway base = scratchFlyway(SUFFIX_C);
        Flyway upTo615 = Flyway.configure()
                .configuration(base.getConfiguration())
                .locations("filesystem:" + withoutV001_95("v001_95_c").toAbsolutePath())
                .target("6.15")
                .load();
        upTo615.migrate();
        JdbcTemplate db = new JdbcTemplate(upTo615.getConfiguration().getDataSource());
        seedOldData(db, Stage006.LINKS);
        Snapshot before = snapshot(db);
        long allergen = penicillin(db);

        Flyway.configure()
                .configuration(base.getConfiguration())
                .outOfOrder(true)
                .load()
                .migrate();

        assertMigrated(db, before);
        long mapped = mappedAllergen(db, allergen);
        assertThat(medicationIngredients(db))
                .as("V006_95 chép id mới của bảng con thuốc")
                .containsExactly(mapped);
        assertThat(prescriptionIngredients(db))
                .as("V006_95 chép id mới của bảng con đơn")
                .containsExactly(mapped);
    }

    /** Thư mục tạm có mọi migration trừ V001_95, gồm cả thư mục con demo (Flyway quét đệ quy). */
    private static Path withoutV001_95(String prefix) throws IOException {
        Path copy = Files.createTempDirectory(prefix);
        try (Stream<Path> files = Files.walk(MIGRATIONS)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".sql")).toList()) {
                if (!file.getFileName().toString().startsWith("V001_95__")) {
                    Path target = copy.resolve(MIGRATIONS.relativize(file).toString());
                    Files.createDirectories(target.getParent());
                    Files.copy(file, target);
                }
            }
        }
        return copy;
    }

    private static long penicillin(JdbcTemplate db) {
        return db.queryForObject("SELECT id FROM allergen WHERE name = 'Penicillin'", Long.class);
    }

    private static long mappedAllergen(JdbcTemplate db, long oldId) {
        return db.queryForObject(
                "SELECT id FROM catalog_item WHERE catalog_type = 'M01.ALLERGEN'"
                        + " AND CAST(attributes ->> '$.legacy.id' AS UNSIGNED) = ?",
                Long.class,
                oldId);
    }

    private static List<Long> medicationIngredients(JdbcTemplate db) {
        return db.queryForList(
                "SELECT j.v FROM catalog_item c, JSON_TABLE(c.attributes, '$.activeIngredientIds[*]'"
                        + " COLUMNS (v BIGINT PATH '$')) j WHERE c.catalog_type = 'M07.MEDICATION'",
                Long.class);
    }

    private static List<Long> prescriptionIngredients(JdbcTemplate db) {
        return db.queryForList(
                "SELECT j.v FROM prescription p, JSON_TABLE(p.active_ingredient_ids, '$[*]'"
                        + " COLUMNS (v BIGINT PATH '$')) j",
                Long.class);
    }

    private static String prescriptionLockTrigger(JdbcTemplate db) {
        return db.queryForObject(
                "SELECT ACTION_STATEMENT FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA = DATABASE()"
                        + " AND TRIGGER_NAME = 'trg_prescription_lock'",
                String.class);
    }

    /** Dữ liệu cũ của 001: thêm mục ngừng hiệu lực để thử trạng thái; id cũ nhỏ nên dễ đụng id mới của catalog_item. */
    private static void seedOldData(JdbcTemplate db, Stage006 stage) {
        db.update("INSERT INTO allergy_severity (name, display_order, status) VALUES ('Rất nặng', 4, 'INACTIVE')");
        db.update("INSERT INTO information_source (name, status) VALUES ('Nguồn cũ', 'INACTIVE')");
        db.update("INSERT INTO care_need (name) VALUES ('Nhu cầu thử')");
        long severe = id(db, "allergy_severity", "Nặng");
        long mild = id(db, "allergy_severity", "Nhẹ");
        long family = id(db, "information_source", "Người thân");
        long doctor = id(db, "information_source", "Bác sĩ khám");
        long needA = id(db, "care_need", "Hỗ trợ vận động");
        long needB = id(db, "care_need", "Phục hồi sau tai biến");
        long needC = id(db, "care_need", "Nhu cầu thử");

        for (int i = 0; i < 2; i++) {
            db.update(
                    "INSERT INTO resident (profile_code, full_name, date_of_birth, gender, status, status_since,"
                            + " current_care_level, created_at, created_by) VALUES (?, ?, '1940-05-01', 'FEMALE',"
                            + " 'IN_RESIDENCE', NOW(6), ?, NOW(6), 1)",
                    "M1-95-" + SUFFIX_A + i + System.nanoTime() % 100000,
                    "Người thử " + i,
                    i == 0 ? "BASIC" : "SPECIAL");
        }
        List<Long> residents = db.queryForList("SELECT id FROM resident ORDER BY id DESC LIMIT 2", Long.class);
        long r1 = residents.get(1);
        long r2 = residents.get(0);

        // Dị nguyên: một mục Ngừng hiệu lực (giữ trạng thái, phiên bản); id 006 lưu là id cũ
        db.update("INSERT INTO allergen (name, allergen_group, status, version) VALUES ('Đậu phộng', 'FOOD',"
                + " 'INACTIVE', 2)");
        db.update("INSERT INTO allergen (name, allergen_group) VALUES ('Penicillin', 'ACTIVE_INGREDIENT')");
        long allergen = db.queryForObject("SELECT id FROM allergen WHERE name = 'Penicillin'", Long.class);
        seed006(db, stage, r1, allergen);
        item(db, r1, "ALLERGY", allergen, null, severe, family);
        item(db, r1, "ALLERGY", null, "Hải sản", severe, family);
        item(db, r1, "CHRONIC_CONDITION", null, "Tăng huyết áp", null, doctor);
        item(db, r2, "ALLERGY", null, "Bụi", mild, family);

        db.update(
                "INSERT INTO assessment (resident_id, type, performed_by, performed_at, care_level, status)"
                        + " VALUES (?, 'INITIAL', 3, NOW(6), 'REGULAR', 'DRAFT')",
                r1);
        long assessment = db.queryForObject("SELECT MAX(id) FROM assessment", Long.class);
        for (long need : new long[] {needA, needB, needC}) {
            db.update("INSERT INTO assessment_care_need (assessment_id, care_need_id) VALUES (?, ?)", assessment, need);
        }
        db.update("INSERT INTO file (uuid, original_name, mime_type, size_bytes, sha256, uploaded_by, uploaded_at)"
                + " VALUES (UUID(), 'ky.pdf', 'application/pdf', 1, REPEAT('a', 64), 3, NOW(6))");
        long file = db.queryForObject("SELECT MAX(id) FROM file", Long.class);
        for (long resident : new long[] {r1, r2}) {
            db.update(
                    "INSERT INTO consent (resident_id, consenter_type, photo_consent, notification_consent,"
                            + " consented_at, evidence_file_id, status, created_at, created_by)"
                            + " VALUES (?, 'RESIDENT', FALSE, TRUE, NOW(6), ?, 'ACTIVE', NOW(6), 3)",
                    resident,
                    file);
        }
        long consent = db.queryForObject("SELECT MIN(id) FROM consent WHERE resident_id = ?", Long.class, r1);
        for (long member : new long[] {9002, 9001}) {
            db.update(
                    "INSERT INTO consent_family_member (consent_id, family_member_id) VALUES (?, ?)", consent, member);
        }
        db.update(
                "INSERT INTO care_level_history (resident_id, care_level, effective_from, basis_type, basis_id,"
                        + " recorded_at, recorded_by) VALUES (?, 'BASIC', '2026-01-01', 'ASSESSMENT', 1, NOW(6), 3)",
                r1);
        seedHistories(db, r1, r2);
        seedHealthRecords(db, r1, r2);
        // nhắc định kỳ: lần nhắc 2 lần, 1 lần, và chưa nhắc lần nào; cảnh báo quá hạn của hai mốc cùng một lượt vắng
        db.update(
                "INSERT INTO periodic_reminder (type, resident_id, milestone_id, started_at, last_reminded_at,"
                        + " reminder_count) VALUES"
                        + " ('MISSING_CONSENT', ?, 0, '2026-02-01 08:00:00.123456', '2026-02-03 08:00:00.654321', 2),"
                        + " ('REVIEW_SELF_DETERMINATION', ?, 77, '2026-02-02 09:00:00', '2026-02-02 09:00:00', 1),"
                        + " ('CREATE_CARE_LEVEL_CHANGE_REQUEST', ?, 5, '2026-02-04 10:00:00', NULL, 0)",
                r1,
                r2,
                r1);
        db.update(
                "INSERT INTO return_overdue_alert (absence_id, expected_return_at, resident_id, alerted_at) VALUES"
                        + " (31, '2026-03-01 10:30:00.250000', ?, '2026-03-01 12:00:00.500000'),"
                        + " (31, '2026-03-02 10:30:00', ?, '2026-03-02 12:00:00')",
                r1,
                r1);
        seedWishes(db, r1, r2, file);
    }

    /** Thuốc và đơn của 006 lưu id dị nguyên: bảng con (trước V006_95) hoặc cột JSON (sau V006_95). */
    private static void seed006(JdbcTemplate db, Stage006 stage, long resident, long allergen) {
        String prescription = "INSERT INTO prescription (resident_id, medication_id, dosage, dosage_unit, route,"
                + " prescription_type, min_interval_minutes, max_doses_per_24h, started_at, medication_source,"
                + " prescriber_type, created_at%s) VALUES (?, ?, 1, 'viên', 'Uống', 'PRN', 60, 4, NOW(6), 'FACILITY',"
                + " 'INTERNAL', NOW(6)%s)";
        if (stage == Stage006.JSON) {
            db.update(
                    "INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes,"
                            + " version) VALUES ('M07.MEDICATION', 'M07', 'M07-test', 'Thuốc thử | 1mg | VIÊN', 0,"
                            + " 'ACTIVE', JSON_OBJECT('name', 'Thuốc thử', 'activeIngredientIds', JSON_ARRAY(?)), 0)",
                    allergen);
            long medication = db.queryForObject(
                    "SELECT id FROM catalog_item WHERE catalog_type = 'M07.MEDICATION' AND code = 'M07-test'",
                    Long.class);
            db.update(
                    prescription.formatted(", active_ingredient_ids", ", JSON_ARRAY(?)"),
                    resident,
                    medication,
                    allergen);
        } else if (stage == Stage006.LINKS) {
            db.update("INSERT INTO medication (name, concentration, dosage_form, default_route, usage_unit)"
                    + " VALUES ('Thuốc thử', '1mg', 'VIÊN', 'Uống', 'viên')");
            long medication = db.queryForObject("SELECT id FROM medication WHERE name = 'Thuốc thử'", Long.class);
            db.update(
                    "INSERT INTO medication_active_ingredient (medication_id, allergen_id) VALUES (?, ?)",
                    medication,
                    allergen);
            db.update(prescription.formatted("", ""), resident, medication);
            long rx = db.queryForObject("SELECT MAX(id) FROM prescription", Long.class);
            db.update(
                    "INSERT INTO prescription_active_ingredient (prescription_id, allergen_id) VALUES (?, ?)",
                    rx,
                    allergen);
        }
    }

    /**
     * Lịch sử trạng thái (Tạo hồ sơ không lý do, lệnh do hệ thống, căn cứ, miễn điều kiện), mức chăm sóc thứ hai, và mọi
     * nơi trỏ id dòng lịch sử: tệp giấy tờ bệnh viện, bằng chứng qua đời, đính chính, chuỗi đính chính, yêu cầu đính chính
     * chờ duyệt, lần báo đánh giá lại nguồn {@code STATUS_HISTORY}.
     */
    private static void seedHistories(JdbcTemplate db, long r1, long r2) {
        String insert =
                "INSERT INTO resident_status_history (resident_id, from_status, to_status, command, occurred_at,"
                        + " performed_by, by_system, reason, basis_type, basis_id, condition_waiver)"
                        + " VALUES (?, ?, ?, ?, ?, 3, ?, ?, ?, ?, ?)";
        db.update(
                insert,
                r1,
                null,
                "ADMISSION_IN_PROGRESS",
                "CREATE_PROFILE",
                "2026-01-01 08:00:00.111111",
                false,
                null,
                null,
                null,
                null);
        db.update(
                insert,
                r1,
                "ADMISSION_IN_PROGRESS",
                "IN_RESIDENCE",
                "COMPLETE_ADMISSION",
                "2026-01-02 08:00:00",
                false,
                "Đủ hồ sơ",
                null,
                null,
                null);
        db.update(
                insert,
                r1,
                "IN_RESIDENCE",
                "HOSPITALIZED",
                "TRANSFER_OUT",
                "2026-01-03 08:00:00.5",
                false,
                "Cấp cứu",
                "INCIDENT",
                77,
                null);
        long transfer = db.queryForObject("SELECT MAX(id) FROM resident_status_history", Long.class);
        db.update(
                insert,
                r1,
                "HOSPITALIZED",
                "IN_RESIDENCE",
                "RECORD_RETURN",
                "2026-01-04 08:00:00",
                false,
                "Ra viện",
                null,
                null,
                null);
        long back = db.queryForObject("SELECT MAX(id) FROM resident_status_history", Long.class);
        db.update(
                insert,
                r2,
                "IN_RESIDENCE",
                "OFF_SITE_ACTIVITY",
                "ROLL_CALL_DEPARTURE",
                "2026-01-05 08:00:00",
                true,
                null,
                null,
                null,
                null);
        db.update(
                insert,
                r2,
                "OFF_SITE_ACTIVITY",
                "RESIDENCY_ENDED",
                "END_RESIDENCY",
                "2026-01-06 08:00:00",
                false,
                "Về nhà",
                "APPROVAL_REQUEST",
                501,
                "{\"approvalRequestId\": 501, \"conditionCodes\": [\"UNPAID_FEES\"]}");
        long ended = db.queryForObject("SELECT MAX(id) FROM resident_status_history", Long.class);
        db.update(
                insert,
                r2,
                "RESIDENCY_ENDED",
                "DECEASED",
                "RECORD_DEATH",
                "2026-01-07 08:00:00",
                false,
                "Qua đời",
                "EVIDENCE_FILE",
                1,
                null);
        long death = db.queryForObject("SELECT MAX(id) FROM resident_status_history", Long.class);
        db.update(
                "INSERT INTO care_level_history (resident_id, care_level, effective_from, basis_type, basis_id,"
                        + " recorded_at, recorded_by) VALUES (?, 'SPECIAL', '2026-02-01 00:00:00.000001',"
                        + " 'RESIDENCY_CHANGE_REQUEST', 12, '2026-01-31 09:00:00.25', 4)",
                r2);

        Object[][] owners = {
            {"M01.HOSPITAL_DOCUMENT", back}, {"M01.HOSPITAL_DOCUMENT", back}, {"M01.DEATH_EVIDENCE", death}
        };
        for (Object[] owner : owners) {
            db.update("INSERT INTO file (uuid, original_name, mime_type, size_bytes, sha256, uploaded_by, uploaded_at)"
                    + " VALUES (UUID(), 'giay.pdf', 'application/pdf', 1, REPEAT('c', 64), 3, NOW(6))");
            long file = db.queryForObject("SELECT MAX(id) FROM file", Long.class);
            db.update(
                    "UPDATE file SET owner_type = ?, owner_id = ?, attached_at = NOW(6) WHERE id = ?",
                    owner[0],
                    owner[1],
                    file);
        }
        db.update(
                "INSERT INTO correction (record_type, original_record_id, type, corrected_content, reason, actor_id,"
                        + " creation_method, recorded_at, sequence_no) VALUES ('M01.RESIDENT_STATUS_HISTORY', ?,"
                        + " 'AMEND_CONTENT', JSON_OBJECT('reason', 'Cấp cứu do ngã'), 'Ghi thiếu', 3, 'A', NOW(6), 1)",
                transfer);
        db.update(
                "INSERT INTO correction_chain (record_type, original_record_id, correction_count)"
                        + " VALUES ('M01.RESIDENT_STATUS_HISTORY', ?, 1)",
                transfer);
        db.update(
                "INSERT INTO approval_request (type, status, content, requester_id, created_at, reason,"
                        + " requested_effective_date) VALUES ('M15.CORRECTION', 'PENDING', JSON_OBJECT('recordType',"
                        + " 'M01.RESIDENT_STATUS_HISTORY', 'originalRecordId', ?, 'type', 'AMEND_CONTENT',"
                        + " 'correctedContent', JSON_OBJECT('reason', 'Về quê')), 3, NOW(6), 'Sửa lý do', CURDATE())",
                ended);
        db.update(
                "INSERT INTO reassessment_notice (resident_id, basis, source_type, source_id, notified_at, deadline_from)"
                        + " VALUES (?, 'RETURN_FROM_HOSPITAL', 'STATUS_HISTORY', ?, '2026-01-04 08:00:00', NULL)",
                r1,
                back);
        db.update(
                "INSERT INTO reassessment_notice (resident_id, basis, source_type, source_id, notified_at, deadline_from,"
                        + " overdue_alerted_at) VALUES (?, 'PERIODIC', 'ASSESSMENT', 41, '2026-01-10 08:00:00.123456',"
                        + " '2026-01-10 08:00:00.123456', '2026-02-20 09:00:00'), (?, 'AFTER_INCIDENT', 'INCIDENT', 9,"
                        + " '2026-01-11 08:00:00', '2026-01-11 08:00:00', NULL)",
                r1,
                r2);
    }

    /**
     * Hồ sơ sức khỏe ban đầu: r1 đã xác nhận (có đính chính, chuỗi, yêu cầu đính chính chờ duyệt), r2 còn nháp (trường
     * để trống, version 3).
     */
    private static void seedHealthRecords(JdbcTemplate db, long r1, long r2) {
        db.update(
                "INSERT INTO initial_health_record (resident_id, blood_type, mobility, self_care_ability, cognition,"
                        + " initial_needs_note, nutrition_needs, current_medications, rehabilitation_needs, performed_by,"
                        + " performed_at, confirmed_by, confirmed_at, version) VALUES (?, 'O+', 'Đi gậy', 'Tự ăn',"
                        + " 'Tỉnh', 'Ghi chú', 'Ít muối', 'Metformin', 'Tập đi', 5, '2026-01-02 07:00:00.654321', 3,"
                        + " '2026-01-02 09:15:00', 2), (?, NULL, 'Ngồi xe lăn', NULL, NULL, NULL, NULL, NULL, NULL, 5,"
                        + " '2026-01-05 07:00:00', NULL, NULL, 3)",
                r1,
                r2);
        long confirmed =
                db.queryForObject("SELECT id FROM initial_health_record WHERE resident_id = ?", Long.class, r1);
        db.update(
                "INSERT INTO correction (record_type, original_record_id, type, corrected_content, reason, actor_id,"
                        + " creation_method, recorded_at, sequence_no) VALUES ('M01.INITIAL_HEALTH_RECORD', ?,"
                        + " 'AMEND_CONTENT', JSON_OBJECT('bloodType', 'A+'), 'Ghi nhầm nhóm máu', 3, 'A', NOW(6), 1)",
                confirmed);
        db.update(
                "INSERT INTO correction_chain (record_type, original_record_id, correction_count)"
                        + " VALUES ('M01.INITIAL_HEALTH_RECORD', ?, 1)",
                confirmed);
        db.update(
                "INSERT INTO approval_request (type, status, content, requester_id, created_at, reason,"
                        + " requested_effective_date) VALUES ('M15.CORRECTION', 'PENDING', JSON_OBJECT('recordType',"
                        + " 'M01.INITIAL_HEALTH_RECORD', 'originalRecordId', ?, 'type', 'AMEND_CONTENT',"
                        + " 'correctedContent', JSON_OBJECT('mobility', 'Đi được')), 3, NOW(6), 'Sửa', CURDATE())",
                confirmed);
    }

    private static final String OLD_IHR_ROW = "CONCAT_WS('|', o.resident_id, IFNULL(o.blood_type, '-'),"
            + " IFNULL(o.mobility, '-'), IFNULL(o.self_care_ability, '-'), IFNULL(o.cognition, '-'),"
            + " IFNULL(o.initial_needs_note, '-'), IFNULL(o.nutrition_needs, '-'), IFNULL(o.current_medications, '-'),"
            + " IFNULL(o.rehabilitation_needs, '-'), o.performed_by, o.performed_at, IFNULL(o.confirmed_by, '-'),"
            + " IFNULL(o.confirmed_at, '-'), IF(o.confirmed_at IS NULL, 'DRAFT', 'CONFIRMED'), o.version)";

    private static String newField(String field) {
        return "IF(JSON_TYPE(s.content -> '$." + field + "') = 'NULL', '-', s.content ->> '$." + field + "')";
    }

    private static final String NEW_IHR_ROW = "CONCAT_WS('|', s.resident_id, " + newField("bloodType") + ", "
            + newField("mobility") + ", " + newField("selfCareAbility") + ", " + newField("cognition") + ", "
            + newField("initialNeedsNote") + ", " + newField("nutritionNeeds") + ", " + newField("currentMedications")
            + ", " + newField("rehabilitationNeeds") + ", s.content ->> '$.performedBy', s.created_at, "
            + newField("confirmedBy") + ", IFNULL(s.locked_at, '-'), s.status, s.version)";

    /** Hồ sơ sức khỏe ban đầu và nơi trỏ tới nó (đính chính, chuỗi, yêu cầu chờ duyệt), ghi bằng nội dung hồ sơ. */
    private static List<String> healthRecords(JdbcTemplate db, boolean migrated) {
        return migrated
                ? db.queryForList(
                        "SELECT CONCAT(" + NEW_IHR_ROW + ", '|', s.unique_key) FROM simple_record s"
                                + " WHERE s.kind = 'M01.INITIAL_HEALTH_RECORD'",
                        String.class)
                : db.queryForList(
                        "SELECT CONCAT(" + OLD_IHR_ROW + ", '|M01.IHR:', o.resident_id) FROM initial_health_record o",
                        String.class);
    }

    private static List<String> healthRecordRefs(JdbcTemplate db, boolean migrated) {
        String row = migrated ? NEW_IHR_ROW : OLD_IHR_ROW;
        String join = migrated
                ? "simple_record s ON s.id = %s AND s.kind = 'M01.INITIAL_HEALTH_RECORD'"
                : "initial_health_record o ON o.id = %s";
        List<String> refs = new ArrayList<>();
        refs.addAll(db.queryForList(
                "SELECT CONCAT('C|', c.id, '|', " + row + ") FROM correction c JOIN "
                        + join.formatted("c.original_record_id") + " WHERE c.record_type = 'M01.INITIAL_HEALTH_RECORD'",
                String.class));
        refs.addAll(db.queryForList(
                "SELECT CONCAT('CH|', c.correction_count, '|', " + row + ") FROM correction_chain c JOIN "
                        + join.formatted("c.original_record_id") + " WHERE c.record_type = 'M01.INITIAL_HEALTH_RECORD'",
                String.class));
        refs.addAll(db.queryForList(
                "SELECT CONCAT('A|', a.id, '|', " + row + ") FROM approval_request a JOIN "
                        + join.formatted("CAST(a.content ->> '$.originalRecordId' AS UNSIGNED)")
                        + " WHERE a.type = 'M15.CORRECTION' AND a.content ->> '$.recordType' = 'M01.INITIAL_HEALTH_RECORD'",
                String.class));
        return refs;
    }

    /** Lần báo đánh giá lại dạng chữ; nguồn là lịch sử trạng thái thì không ghi id (đổi khi chuyển, so ở historyRefs). */
    private static List<String> notices(JdbcTemplate db, boolean migrated) {
        return migrated
                ? db.queryForList(
                        "SELECT CONCAT_WS('|', resident_id, content ->> '$.basis', entity_type,"
                                + " IF(entity_type = 'M01.RESIDENT_STATUS_HISTORY', 'H', entity_id), created_at,"
                                + " content ->> '$.notifiedAt', content ->> '$.deadlineFrom',"
                                + " content ->> '$.overdueAlertedAt', status, IFNULL(locked_at, '-'),"
                                + " IF(entity_type = 'M01.RESIDENT_STATUS_HISTORY', CONCAT(SUBSTRING_INDEX(unique_key, ':',"
                                + " 4), ':H'), unique_key))"
                                + " FROM simple_record WHERE kind = 'M01.REASSESSMENT_NOTICE'",
                        String.class)
                : db.queryForList(
                        "SELECT CONCAT_WS('|', resident_id, basis, CASE source_type WHEN 'ASSESSMENT' THEN"
                                + " 'M01.ASSESSMENT' WHEN 'INCIDENT' THEN 'M05.INCIDENT' WHEN 'STATUS_HISTORY' THEN"
                                + " 'M01.RESIDENT_STATUS_HISTORY' ELSE 'M15.CORRECTION' END,"
                                + " IF(source_type = 'STATUS_HISTORY', 'H', source_id), notified_at,"
                                + " DATE_FORMAT(notified_at, '%Y-%m-%dT%H:%i:%s.%fZ'),"
                                + " IFNULL(DATE_FORMAT(deadline_from, '%Y-%m-%dT%H:%i:%s.%fZ'), 'null'),"
                                + " IFNULL(DATE_FORMAT(overdue_alerted_at, '%Y-%m-%dT%H:%i:%s.%fZ'), 'null'), 'NOTIFIED',"
                                + " '-', CONCAT('M01.RAN:', resident_id, ':', basis, ':', source_type, ':',"
                                + " IF(source_type = 'STATUS_HISTORY', 'H', source_id))) FROM reassessment_notice",
                        String.class);
    }

    /** Dòng lịch sử trạng thái dạng chữ (không có id dòng). */
    private static List<String> statusRows(JdbcTemplate db, boolean migrated) {
        return migrated
                ? db.queryForList(
                        "SELECT " + NEW_STATUS_ROW + " FROM " + NEW_STATUS + " WHERE TRUE" + NEW_STATUS_ON,
                        String.class)
                : db.queryForList("SELECT " + OLD_STATUS_ROW + " FROM " + OLD_STATUS, String.class);
    }

    private static List<String> careLevelRows(JdbcTemplate db, boolean migrated) {
        return migrated
                ? db.queryForList(
                        "SELECT CONCAT_WS('|', entity_id, value, starts_at, basis_type, basis_id, actor_id,"
                                + " details ->> '$.recordedAt') FROM status_history WHERE entity_type = 'M01.RESIDENT'"
                                + " AND attribute = 'CARE_LEVEL'",
                        String.class)
                : db.queryForList(
                        "SELECT CONCAT_WS('|', resident_id, care_level, effective_from, basis_type, basis_id, recorded_by,"
                                + " DATE_FORMAT(recorded_at, '%Y-%m-%dT%H:%i:%s.%fZ')) FROM care_level_history",
                        String.class);
    }

    /** Nơi trỏ tới dòng lịch sử trạng thái, ghi bằng nội dung dòng được trỏ (id dòng đổi khi chuyển). */
    private static List<String> historyRefs(JdbcTemplate db, boolean migrated) {
        String row = migrated ? NEW_STATUS_ROW : OLD_STATUS_ROW;
        String join = migrated ? NEW_STATUS + " ON n.id = %s" + NEW_STATUS_ON : OLD_STATUS + " ON o.id = %s";
        List<String> refs = new ArrayList<>();
        refs.addAll(db.queryForList(
                "SELECT CONCAT('F|', f.id, '|', f.owner_type, '|', " + row + ") FROM file f JOIN "
                        + join.formatted("f.owner_id")
                        + " WHERE f.owner_type IN ('M01.HOSPITAL_DOCUMENT', 'M01.DEATH_EVIDENCE')",
                String.class));
        refs.addAll(db.queryForList(
                "SELECT CONCAT('C|', c.id, '|', c.sequence_no, '|', " + row + ") FROM correction c JOIN "
                        + join.formatted("c.original_record_id")
                        + " WHERE c.record_type = 'M01.RESIDENT_STATUS_HISTORY'",
                String.class));
        refs.addAll(db.queryForList(
                "SELECT CONCAT('CH|', c.correction_count, '|', " + row + ") FROM correction_chain c JOIN "
                        + join.formatted("c.original_record_id")
                        + " WHERE c.record_type = 'M01.RESIDENT_STATUS_HISTORY'",
                String.class));
        refs.addAll(db.queryForList(
                "SELECT CONCAT('A|', a.id, '|', " + row + ") FROM approval_request a JOIN "
                        + join.formatted("CAST(a.content ->> '$.originalRecordId' AS UNSIGNED)")
                        + " WHERE a.type = 'M15.CORRECTION' AND a.content ->> '$.recordType' = 'M01.RESIDENT_STATUS_HISTORY'",
                String.class));
        refs.addAll(
                migrated
                        ? db.queryForList(
                                "SELECT CONCAT('N|', s.content ->> '$.basis', '|', " + row
                                        + ") FROM simple_record s JOIN "
                                        + join.formatted("s.entity_id")
                                        + " WHERE s.kind = 'M01.REASSESSMENT_NOTICE'"
                                        + " AND s.entity_type = 'M01.RESIDENT_STATUS_HISTORY'",
                                String.class)
                        : db.queryForList(
                                "SELECT CONCAT('N|', r.basis, '|', " + row + ") FROM reassessment_notice r JOIN "
                                        + join.formatted("r.source_id") + " WHERE r.source_type = 'STATUS_HISTORY'",
                                String.class));
        return refs;
    }

    /** Hai phiếu cuối đời (cũ Được thay thế, mới Hiệu lực), ba phiếu điều trị có người cần báo, ý kiến, tệp đã gắn. */
    private static void seedWishes(JdbcTemplate db, long r1, long r2, long file) {
        String eol = "INSERT INTO end_of_life_wish (resident_id, choice, first_contact, respondent_type,"
                + " assessing_doctor_id, can_self_determine, assessed_at, support_method, review_date, signer_type,"
                + " signed_at, signed_copy_file_id, source, status, recorded_by, recorded_at) VALUES (?, ?, 'Con trai',"
                + " 'RESIDENT', 3, TRUE, '2026-01-02 08:00:00', 'Giải thích', '2026-07-01', 'RESIDENT',"
                + " '2026-01-02 09:00:00', ?, 'ADMISSION', ?, 3, ?)";
        db.update(eol, r1, "RETURN_HOME", file, "SUPERSEDED", "2026-01-02 09:30:00.123456");
        long firstEol = db.queryForObject("SELECT MAX(id) FROM end_of_life_wish", Long.class);
        db.update(eol, r1, "STAY_PALLIATIVE", file, "ACTIVE", "2026-02-02 09:30:00");
        long secondEol = db.queryForObject("SELECT MAX(id) FROM end_of_life_wish", Long.class);
        db.update("UPDATE end_of_life_wish SET superseded_by_id = ? WHERE id = ?", secondEol, firstEol);
        db.update(
                "INSERT INTO end_of_life_wish_opinion (wish_id, source_type, source_family_member_id, content,"
                        + " recorded_by, recorded_at) VALUES (?, 'FAMILY_MEMBER', 9001, 'Gia đình muốn về nhà', 3,"
                        + " '2026-01-02 09:30:00.123456'), (?, 'OBSERVING_STAFF', NULL, 'Cụ hay nhắc nhà', 3,"
                        + " '2026-01-02 09:30:00.123456')",
                firstEol,
                firstEol);

        String treatment = "INSERT INTO treatment_wish (resident_id, hospital1_name, hospital1_address,"
                + " ask_family_first, escort, respondent_type, status, recorded_by, recorded_at, signed_copy_file_id)"
                + " VALUES (?, ?, 'Địa chỉ 1', ?, 'FAMILY', 'RESIDENT', ?, 3, ?, ?)";
        long[] treatments = new long[3];
        long[] files = new long[3];
        String[] hospitals = {"Bệnh viện X", "Bệnh viện Y", "Bệnh viện Z"};
        long[] residents = {r1, r1, r2};
        String[] statuses = {"EXPIRED", "ACTIVE", "ACTIVE"};
        for (int i = 0; i < 3; i++) {
            db.update("INSERT INTO file (uuid, original_name, mime_type, size_bytes, sha256, uploaded_by, uploaded_at)"
                    + " VALUES (UUID(), 'dieutri.pdf', 'application/pdf', 1, REPEAT('b', 64), 3, NOW(6))");
            files[i] = db.queryForObject("SELECT MAX(id) FROM file", Long.class);
            db.update(
                    treatment,
                    residents[i],
                    hospitals[i],
                    i == 1,
                    statuses[i],
                    "2026-03-0" + (i + 1) + " 10:00:00.5",
                    files[i]);
            treatments[i] = db.queryForObject("SELECT MAX(id) FROM treatment_wish", Long.class);
            db.update(
                    "UPDATE file SET owner_type = 'M01.TREATMENT_WISH', owner_id = ?, attached_at = NOW(6)"
                            + " WHERE id = ?",
                    treatments[i],
                    files[i]);
        }
        db.update("UPDATE treatment_wish SET superseded_by_id = ? WHERE id = ?", treatments[1], treatments[0]);
        db.update(
                "INSERT INTO treatment_wish_notify (treatment_wish_id, family_member_id) VALUES (?, 9002), (?, 9001)",
                treatments[1],
                treatments[1]);
        db.update(
                "INSERT INTO treatment_wish_opinion (treatment_wish_id, source_type, source_family_member_id, content,"
                        + " recorded_by, recorded_at) VALUES (?, 'OBSERVING_STAFF', NULL, 'Cụ ngại bệnh viện lớn', 3,"
                        + " '2026-03-01 10:00:00.5')",
                treatments[0]);
    }

    private static void item(
            JdbcTemplate db, long resident, String type, Long allergen, String text, Long severity, long source) {
        db.update(
                "INSERT INTO health_item (resident_id, type, allergen_id, free_text, severity_id, source_id,"
                        + " recorded_by, recorded_at) VALUES (?, ?, ?, ?, ?, ?, 3, NOW(6))",
                resident,
                type,
                allergen,
                text,
                severity,
                source);
    }

    private static long id(JdbcTemplate db, String table, String name) {
        return db.queryForObject("SELECT id FROM " + table + " WHERE name = ?", Long.class, name);
    }

    private static Snapshot snapshot(JdbcTemplate db) {
        Map<Long, String> severity = new java.util.LinkedHashMap<>();
        Map<Long, String> source = new java.util.LinkedHashMap<>();
        db.query(
                "SELECT h.id, s.name AS severity, i.name AS source FROM health_item h"
                        + " LEFT JOIN allergy_severity s ON s.id = h.severity_id"
                        + " JOIN information_source i ON i.id = h.source_id",
                rs -> {
                    severity.put(rs.getLong("id"), rs.getString("severity"));
                    source.put(rs.getLong("id"), rs.getString("source"));
                });
        Map<Long, Set<String>> needs = new java.util.LinkedHashMap<>();
        db.query(
                "SELECT a.assessment_id, n.name FROM assessment_care_need a JOIN care_need n ON n.id = a.care_need_id",
                rs -> {
                    needs.computeIfAbsent(rs.getLong(1), k -> new java.util.TreeSet<>())
                            .add(rs.getString(2));
                });
        List<String> rows = new ArrayList<>();
        rows.addAll(db.queryForList(
                "SELECT CONCAT('CARE_LEVEL|', code, '|', name, '|ACTIVE|', display_order)" + " FROM care_level",
                String.class));
        rows.addAll(db.queryForList(
                "SELECT CONCAT('ALLERGY_SEVERITY|', name, '|', status, '|', display_order)" + " FROM allergy_severity",
                String.class));
        rows.addAll(db.queryForList(
                "SELECT CONCAT('INFORMATION_SOURCE|', name, '|', status) FROM information_source", String.class));
        rows.addAll(db.queryForList("SELECT CONCAT('CARE_NEED|', name, '|', status) FROM care_need", String.class));
        rows.addAll(db.queryForList("SELECT CONCAT('ALLERGEN|', name, '|', status) FROM allergen", String.class));
        return new Snapshot(
                severity,
                source,
                needs,
                rows,
                db.queryForObject("SELECT COUNT(*) FROM health_item", Integer.class),
                db.queryForObject("SELECT COUNT(*) FROM assessment_care_need", Integer.class),
                consentMembers(
                        db,
                        "SELECT c.id, m.family_member_id FROM consent c"
                                + " LEFT JOIN consent_family_member m ON m.consent_id = c.id"),
                db.queryForList(
                        "SELECT CONCAT(type, '|', resident_id, '|', milestone_id, '|', started_at, '|',"
                                + " IFNULL(last_reminded_at, started_at), '|', reminder_count) FROM periodic_reminder",
                        String.class),
                db.queryForList(
                        "SELECT CONCAT(absence_id, '|', DATE_FORMAT(expected_return_at, '%Y-%m-%dT%H:%i:%s.%f'),"
                                + " '|', resident_id, '|', alerted_at) FROM return_overdue_alert",
                        String.class),
                oldWishes(db),
                db.queryForObject("SELECT COUNT(*) FROM file WHERE owner_type = 'M01.TREATMENT_WISH'", Integer.class),
                statusRows(db, false),
                careLevelRows(db, false),
                historyRefs(db, false),
                db.queryForObject("SELECT COALESCE(MAX(id), 0) FROM resident_status_history", Long.class),
                notices(db, false),
                db.queryForObject("SELECT COALESCE(MAX(id), 0) FROM reassessment_notice", Long.class),
                healthRecords(db, false),
                healthRecordRefs(db, false),
                db.queryForObject("SELECT COALESCE(MAX(id), 0) FROM initial_health_record", Long.class),
                allergenNames(db, "allergen a ON a.id = h.allergen_id"));
    }

    /** Phiếu cuối đời (giữ id), phiếu điều trị (không có id), ý kiến khác của phiếu cuối đời: dạng chữ để so sánh. */
    private static List<String> oldWishes(JdbcTemplate db) {
        List<String> rows = new ArrayList<>();
        rows.addAll(db.queryForList(
                "SELECT CONCAT('E|', id, '|', resident_id, '|', choice, '|', status, '|', IFNULL(superseded_by_id, '-'),"
                        + " '|', review_date, '|', recorded_at) FROM end_of_life_wish",
                String.class));
        rows.addAll(db.queryForList(
                "SELECT CONCAT('T|', t.resident_id, '|', t.hospital1_name, '|', t.ask_family_first, '|', t.escort, '|',"
                        + " t.status, '|', IF(t.superseded_by_id IS NULL, '-', 'S'), '|', t.recorded_at, '|',"
                        + " (SELECT IFNULL(GROUP_CONCAT(n.family_member_id ORDER BY n.family_member_id), '')"
                        + " FROM treatment_wish_notify n WHERE n.treatment_wish_id = t.id), '|',"
                        + " (SELECT IFNULL(GROUP_CONCAT(o.content ORDER BY o.id SEPARATOR ';'), '')"
                        + " FROM treatment_wish_opinion o WHERE o.treatment_wish_id = t.id)) FROM treatment_wish t",
                String.class));
        rows.addAll(db.queryForList(
                "SELECT CONCAT('EO|', wish_id, '|', source_type, '|', IFNULL(source_family_member_id, '-'), '|', content,"
                        + " '|', recorded_at) FROM end_of_life_wish_opinion",
                String.class));
        return rows;
    }

    private static List<String> newWishes(JdbcTemplate db) {
        List<String> rows = new ArrayList<>();
        rows.addAll(db.queryForList(
                "SELECT CONCAT('E|', id, '|', resident_id, '|', choice, '|', status, '|', IFNULL(superseded_by_id, '-'),"
                        + " '|', review_date, '|', recorded_at) FROM wish WHERE kind = 'END_OF_LIFE'",
                String.class));
        rows.addAll(db.queryForList(
                "SELECT CONCAT('T|', t.resident_id, '|', t.hospital1_name, '|', t.ask_family_first, '|', t.escort, '|',"
                        + " t.status, '|', IF(t.superseded_by_id IS NULL, '-', 'S'), '|', t.recorded_at, '|',"
                        + " (SELECT IFNULL(GROUP_CONCAT(j.id ORDER BY j.id), '') FROM JSON_TABLE(t.notify_family_member_ids,"
                        + " '$[*]' COLUMNS (id BIGINT PATH '$')) j), '|',"
                        + " (SELECT IFNULL(GROUP_CONCAT(e.content ->> '$.content' ORDER BY e.id SEPARATOR ';'), '')"
                        + " FROM record_event e WHERE e.kind = 'M01.WISH_OPINION' AND e.entity_type = 'M01.TREATMENT_WISH'"
                        + " AND e.entity_id = t.id)) FROM wish t WHERE t.kind = 'TREATMENT'",
                String.class));
        rows.addAll(db.queryForList(
                "SELECT CONCAT('EO|', entity_id, '|', content ->> '$.sourceType', '|',"
                        + " IF(JSON_TYPE(JSON_EXTRACT(content, '$.sourceFamilyMemberId')) = 'NULL', '-',"
                        + " content ->> '$.sourceFamilyMemberId'), '|', content ->> '$.content', '|', occurred_at)"
                        + " FROM record_event WHERE kind = 'M01.WISH_OPINION' AND entity_type = 'M01.END_OF_LIFE_WISH'",
                String.class));
        return rows;
    }

    /** Tên dị nguyên của từng mục sức khỏe chọn từ danh mục. */
    private static Map<Long, String> allergenNames(JdbcTemplate db, String join) {
        Map<Long, String> names = new java.util.TreeMap<>();
        db.query("SELECT h.id, a.name FROM health_item h JOIN " + join, rs -> {
            names.put(rs.getLong(1), rs.getString(2));
        });
        return names;
    }

    /** Người thân của từng bản đồng ý, sắp tăng; bản không có người thân là danh sách rỗng. */
    private static Map<Long, List<Long>> consentMembers(JdbcTemplate db, String sql) {
        Map<Long, List<Long>> result = new java.util.TreeMap<>();
        db.query(sql, rs -> {
            List<Long> members = result.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>());
            long member = rs.getLong(2);
            if (!rs.wasNull()) {
                members.add(member);
            }
        });
        result.values().forEach(java.util.Collections::sort);
        return result;
    }

    private static void assertMigrated(JdbcTemplate db, Snapshot before) {
        for (String table : OLD_TABLES) {
            assertThat(db.queryForObject(
                            "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()"
                                    + " AND TABLE_NAME = ?",
                            Integer.class,
                            table))
                    .as("bảng cũ %s đã xóa", table)
                    .isZero();
        }
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA = DATABASE()"
                                + " AND ROUTINE_NAME LIKE 'm01\\_%'",
                        Integer.class))
                .isZero();

        // Lịch sử trạng thái, mức chăm sóc vào status_history: đủ dòng, nguyên nội dung, id mới lớn hơn mọi id cũ; nơi
        // trỏ tới dòng lịch sử (tệp, đính chính, chuỗi, yêu cầu chờ duyệt, lần báo đánh giá lại) trỏ đúng dòng mới
        assertThat(statusRows(db, true))
                .as("lịch sử trạng thái")
                .containsExactlyInAnyOrderElementsOf(before.statusRows());
        assertThat(careLevelRows(db, true))
                .as("lịch sử mức chăm sóc")
                .containsExactlyInAnyOrderElementsOf(before.careLevelRows());
        assertThat(historyRefs(db, true))
                .as("tham chiếu tới dòng lịch sử")
                .containsExactlyInAnyOrderElementsOf(before.historyRefs());
        assertThat(before.historyRefs()).hasSizeGreaterThanOrEqualTo(7);
        assertThat(db.queryForObject(
                        "SELECT MIN(id) FROM status_history WHERE details ->> '$.legacy.table' = 'resident_status_history'",
                        Long.class))
                .as("id mới lớn hơn mọi id cũ")
                .isGreaterThan(before.maxStatusHistoryId());
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM status_history WHERE entity_type = 'M01.RESIDENT' AND attribute = 'CARE_LEVEL'"
                                + " AND NOT (unique_key <=> CONCAT('M01.CLH:', entity_id, ':',"
                                + " DATE_FORMAT(starts_at, '%Y-%m-%dT%H:%i:%s.%f')))",
                        Integer.class))
                .as("khóa M01.CLH thay UNIQUE (resident_id, effective_from)")
                .isZero();

        // Lần báo đánh giá lại vào simple_record M01.REASSESSMENT_NOTICE: nguồn là đối tượng cha, lúc báo ở created_at,
        // khóa M01.RAN, dòng không khóa, id mới lớn hơn mọi id cũ
        assertThat(notices(db, true)).as("lần báo đánh giá lại").containsExactlyInAnyOrderElementsOf(before.notices());
        assertThat(before.notices()).hasSizeGreaterThanOrEqualTo(3);
        assertThat(db.queryForObject(
                        "SELECT MIN(id) FROM simple_record WHERE kind = 'M01.REASSESSMENT_NOTICE'"
                                + " AND content ->> '$.legacy.table' = 'reassessment_notice'",
                        Long.class))
                .isGreaterThan(before.maxNoticeId());

        // Hồ sơ sức khỏe ban đầu vào simple_record M01.INITIAL_HEALTH_RECORD: nội dung, trạng thái, khóa lúc xác nhận,
        // version, khóa M01.IHR; đính chính, chuỗi, yêu cầu chờ duyệt trỏ id mới (lớn hơn mọi id cũ)
        assertThat(healthRecords(db, true))
                .as("hồ sơ sức khỏe ban đầu")
                .containsExactlyInAnyOrderElementsOf(before.healthRecords());
        assertThat(healthRecordRefs(db, true))
                .as("tham chiếu tới hồ sơ sức khỏe ban đầu")
                .containsExactlyInAnyOrderElementsOf(before.healthRecordRefs());
        assertThat(before.healthRecordRefs()).hasSizeGreaterThanOrEqualTo(3);
        assertThat(db.queryForObject(
                        "SELECT MIN(id) FROM simple_record WHERE kind = 'M01.INITIAL_HEALTH_RECORD'"
                                + " AND content ->> '$.legacy.table' = 'initial_health_record'",
                        Long.class))
                .isGreaterThan(before.maxHealthRecordId());
        long confirmedRecord = db.queryForObject(
                "SELECT MIN(id) FROM simple_record WHERE kind = 'M01.INITIAL_HEALTH_RECORD' AND locked_at IS NOT NULL",
                Long.class);
        assertThatThrownBy(() -> db.update(
                        "UPDATE simple_record SET content = JSON_SET(content, '$.bloodType', 'B') WHERE id = ?",
                        confirmedRecord))
                .hasMessageContaining("simple_record row is locked");

        // Dị nguyên vào catalog_item M01.ALLERGEN: nhóm ở attributes, id cũ ở legacy, phiên bản giữ; health_item trỏ
        // đúng
        // mục cùng tên
        assertThat(allergenNames(db, "catalog_item a ON a.id = h.allergen_id AND a.catalog_type = 'M01.ALLERGEN'"))
                .as("dị nguyên của mục sức khỏe")
                .isEqualTo(before.allergenNameByItem())
                .isNotEmpty();
        assertThat(db.queryForList(
                        "SELECT CONCAT(name, '|', status, '|', version, '|', attributes ->> '$.allergenGroup') FROM catalog_item"
                                + " WHERE catalog_type = 'M01.ALLERGEN' AND attributes ->> '$.legacy.table' = 'allergen'",
                        String.class))
                .contains("Đậu phộng|INACTIVE|2|FOOD", "Penicillin|ACTIVE|0|ACTIVE_INGREDIENT");

        // Nhắc định kỳ: loại, người, mốc, lúc bắt đầu, lần nhắc cuối và số lần tính lại từ các dòng reminder_marker
        assertThat(db.queryForList(
                        "SELECT CONCAT(SUBSTRING(kind, 23), '|', entity_id, '|', ms, '|', started, '|', last, '|', cnt)"
                                + " FROM (SELECT kind, entity_id,"
                                + " CAST(SUBSTRING_INDEX(REPLACE(marker, 'START:', ''), ':', 1) AS UNSIGNED) AS ms,"
                                + " MIN(IF(marker LIKE 'START:%', marked_at, NULL)) AS started, MAX(marked_at) AS last,"
                                + " SUM(marker NOT LIKE 'START:%') AS cnt FROM reminder_marker"
                                + " WHERE kind LIKE 'M01.PERIODIC_REMINDER.%' AND entity_type = 'M01.RESIDENT'"
                                + " GROUP BY kind, entity_id, ms) t",
                        String.class))
                .as("nhắc định kỳ")
                .containsExactlyInAnyOrderElementsOf(before.reminders());
        assertThat(db.queryForList(
                        "SELECT CONCAT(entity_id, '|', marker, '|', details ->> '$.residentId', '|', marked_at)"
                                + " FROM reminder_marker WHERE kind = 'M01.RETURN_OVERDUE_ALERT'"
                                + " AND entity_type = 'M02.ABSENCE'",
                        String.class))
                .as("cảnh báo Tạm vắng quá hạn")
                .containsExactlyInAnyOrderElementsOf(before.alerts());

        // Hai bảng nguyện vọng: phiếu cuối đời giữ id, điều trị id mới (legacy_id), người cần báo, ý kiến, tệp
        assertThat(newWishes(db)).as("nguyện vọng").containsExactlyInAnyOrderElementsOf(before.wishes());
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM wish WHERE (kind = 'TREATMENT') <> (legacy_id IS NOT NULL)",
                        Integer.class))
                .as("chỉ phiếu điều trị ghi legacy_id")
                .isZero();
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM file f JOIN wish w ON w.id = f.owner_id AND w.kind = 'TREATMENT'"
                                + " AND w.signed_copy_file_id = f.id WHERE f.owner_type = 'M01.TREATMENT_WISH'",
                        Integer.class))
                .as("tệp phiếu điều trị trỏ đúng id mới")
                .isEqualTo(before.treatmentFiles());
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()"
                                + " AND TABLE_NAME = 'wish' AND TABLE_COMMENT = 'DataGroup=BUSINESS'",
                        Integer.class))
                .as("wish khai nhóm dữ liệu")
                .isEqualTo(1);

        // Số dòng, mã, tên, trạng thái, thứ tự của từng danh mục
        List<String> after = db.queryForList(
                "SELECT CONCAT(SUBSTRING(catalog_type, 5), '|', IFNULL(code, ''), '|', name, '|', status, '|',"
                        + " IF(catalog_type IN ('M01.CARE_LEVEL', 'M01.ALLERGY_SEVERITY'), sort_order, 'ID'))"
                        + " FROM catalog_item WHERE module = 'M01' ORDER BY id",
                String.class);
        List<String> expected = before.catalogRows().stream()
                .map(CatalogConsolidationMigrationIT::toCatalogRow)
                .toList();
        assertThat(after).containsExactlyInAnyOrderElementsOf(expected);

        // Mọi mục chép sang đều ghi id cũ và id mới được cấp riêng (không giả định bằng nhau)
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM catalog_item WHERE module = 'M01'"
                                + " AND catalog_type <> 'M01.CARE_LEVEL'"
                                + " AND CAST(attributes ->> '$.legacy.id' AS UNSIGNED) IS NULL",
                        Integer.class))
                .isZero();

        // health_item: số dòng không đổi, mỗi dòng trỏ đúng mục có cùng tên như trước
        assertThat(db.queryForObject("SELECT COUNT(*) FROM health_item", Integer.class))
                .isEqualTo(before.healthItems());
        Map<Long, String> severity = new java.util.LinkedHashMap<>();
        Map<Long, String> source = new java.util.LinkedHashMap<>();
        db.query(
                "SELECT h.id, s.name AS severity, i.name AS source FROM health_item h"
                        + " LEFT JOIN catalog_item s ON s.id = h.severity_id AND s.catalog_type = 'M01.ALLERGY_SEVERITY'"
                        + " JOIN catalog_item i ON i.id = h.source_id AND i.catalog_type = 'M01.INFORMATION_SOURCE'",
                rs -> {
                    severity.put(rs.getLong("id"), rs.getString("severity"));
                    source.put(rs.getLong("id"), rs.getString("source"));
                });
        assertThat(severity).isEqualTo(before.severityNameByItem());
        assertThat(source).isEqualTo(before.sourceNameByItem());

        // Nhu cầu chăm sóc của lần đánh giá vào cột JSON care_need_ids: đủ số, đúng tập nhu cầu theo tên
        assertThat(db.queryForObject(
                        "SELECT COALESCE(SUM(JSON_LENGTH(care_need_ids)), 0) FROM assessment", Integer.class))
                .isEqualTo(before.assessmentCareNeeds());
        Map<Long, Set<String>> needs = new java.util.LinkedHashMap<>();
        db.query(
                "SELECT a.id, n.name FROM assessment a JOIN JSON_TABLE(a.care_need_ids, '$[*]'"
                        + " COLUMNS (need BIGINT PATH '$')) j JOIN catalog_item n"
                        + " ON n.id = j.need AND n.catalog_type = 'M01.CARE_NEED'",
                rs -> {
                    needs.computeIfAbsent(rs.getLong(1), k -> new java.util.TreeSet<>())
                            .add(rs.getString(2));
                });
        assertThat(needs).isEqualTo(before.careNeedNamesByAssessment());

        // Mã mức chăm sóc giữ nguyên ở resident, lịch sử, đánh giá và có trong danh mục
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM (SELECT current_care_level AS c FROM resident"
                                + " UNION ALL SELECT value FROM status_history WHERE attribute = 'CARE_LEVEL'"
                                + " AND entity_type = 'M01.RESIDENT'"
                                + " UNION ALL SELECT care_level FROM assessment) x"
                                + " WHERE c IS NOT NULL AND c NOT IN (SELECT code FROM catalog_item"
                                + " WHERE catalog_type = 'M01.CARE_LEVEL')",
                        Integer.class))
                .isZero();

        // Không còn khóa ngoại nào trỏ vào bảng cũ; trigger bất biến của health_item vẫn chặn đổi id
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE()"
                                + " AND REFERENCED_TABLE_NAME IN ('care_level', 'allergy_severity',"
                                + " 'information_source', 'care_need', 'allergen')",
                        Integer.class))
                .isZero();
        // Bản đồng ý: người thân chuyển sang cột JSON family_member_ids, đủ từng cặp, bảng con đã xóa
        assertThat(consentMembers(
                        db,
                        "SELECT c.id, j.member FROM consent c LEFT JOIN JSON_TABLE(c.family_member_ids,"
                                + " '$[*]' COLUMNS (member BIGINT PATH '$')) j ON TRUE"))
                .isEqualTo(before.consentMembers());
        assertThat(before.consentMembers().values())
                .anySatisfy(m -> assertThat(m).hasSize(2));

        long anyItem = severity.keySet().iterator().next();
        assertThatThrownBy(() -> db.update("UPDATE health_item SET source_id = source_id + 1 WHERE id = ?", anyItem))
                .hasMessageContaining("health_item content is immutable");
    }

    private static String toCatalogRow(String legacy) {
        String[] p = legacy.split("\\|");
        return switch (p[0]) {
            case "CARE_LEVEL" -> "CARE_LEVEL|" + p[1] + "|" + p[2] + "|ACTIVE|" + p[4];
            case "ALLERGY_SEVERITY" -> "ALLERGY_SEVERITY||" + p[1] + "|" + p[2] + "|" + p[3];
            default -> p[0] + "||" + p[1] + "|" + p[2] + "|ID";
        };
    }
}
