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
            "return_overdue_alert");

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
            List<String> alerts) {}

    @AfterAll
    static void dropScratchSchemas() {
        dropScratchSchema(SUFFIX_A);
        dropScratchSchema(SUFFIX_B);
    }

    @Test
    void freshDatabase_oldRowsMoveToCatalogItemWithMappedIds() {
        Flyway upTo110 = Flyway.configure()
                .configuration(scratchFlyway(SUFFIX_A).getConfiguration())
                .target("1.10")
                .load();
        upTo110.migrate();
        JdbcTemplate db = new JdbcTemplate(upTo110.getConfiguration().getDataSource());
        seedOldData(db, false);
        Snapshot before = snapshot(db);

        Flyway.configure()
                .configuration(upTo110.getConfiguration())
                .target("latest")
                .load()
                .migrate();

        assertMigrated(db, before);
    }

    @Test
    void databaseWith006Data_isMigratedWithoutTouchingAllergensAnd006References() throws IOException {
        Flyway base = scratchFlyway(SUFFIX_B);
        Path withoutGroup1 = Files.createTempDirectory("v001_95_b");
        // Cả thư mục con demo: Flyway quét đệ quy, nên lượt chạy sau không còn migration nào khác chưa áp dụng
        try (Stream<Path> files = Files.walk(MIGRATIONS)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".sql")).toList()) {
                if (!file.getFileName().toString().startsWith("V001_95__")) {
                    Path target =
                            withoutGroup1.resolve(MIGRATIONS.relativize(file).toString());
                    Files.createDirectories(target.getParent());
                    Files.copy(file, target);
                }
            }
        }
        Flyway applied = Flyway.configure()
                .configuration(base.getConfiguration())
                .locations("filesystem:" + withoutGroup1.toAbsolutePath())
                .load();
        applied.migrate();
        JdbcTemplate db = new JdbcTemplate(applied.getConfiguration().getDataSource());
        seedOldData(db, true);
        Snapshot before = snapshot(db);
        long allergen = db.queryForObject("SELECT MIN(id) FROM allergen", Long.class);
        String medicationBefore = medication006(db);
        int catalogBefore = db.queryForObject("SELECT COUNT(*) FROM catalog_item", Integer.class);
        assertThat(medicationBefore).contains("[" + allergen + "]");

        // Sau mọi migration khác: chạy riêng V001_95 theo thứ tự muộn (out-of-order)
        Flyway.configure()
                .configuration(base.getConfiguration())
                .outOfOrder(true)
                .load()
                .migrate();

        assertMigrated(db, before);
        assertThat(medication006(db)).isEqualTo(medicationBefore);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM allergen WHERE id = ?", Integer.class, allergen))
                .isEqualTo(1);
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM health_item h JOIN allergen a ON a.id = h.allergen_id", Integer.class))
                .isEqualTo(1);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM catalog_item", Integer.class))
                .isEqualTo(catalogBefore + before.catalogRows().size());
    }

    private static String medication006(JdbcTemplate db) {
        return db.queryForObject(
                "SELECT CAST(attributes AS CHAR) FROM catalog_item WHERE catalog_type = 'M07.MEDICATION'",
                String.class);
    }

    /** Dữ liệu cũ của 001: thêm mục ngừng hiệu lực để thử trạng thái; id cũ nhỏ nên dễ đụng id mới của catalog_item. */
    private static void seedOldData(JdbcTemplate db, boolean with006) {
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

        Long allergen = null;
        if (with006) {
            db.update("INSERT INTO allergen (name, allergen_group) VALUES ('Penicillin', 'ACTIVE_INGREDIENT')");
            allergen = db.queryForObject("SELECT MAX(id) FROM allergen", Long.class);
            db.update(
                    "INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes,"
                            + " version) VALUES ('M07.MEDICATION', 'M07', 'M07-test', 'Thuốc thử | 1mg | VIÊN', 0,"
                            + " 'ACTIVE', JSON_OBJECT('name', 'Thuốc thử', 'activeIngredientIds', JSON_ARRAY(?)), 0)",
                    allergen);
        }
        item(db, r1, "ALLERGY", allergen, allergen == null ? "Hải sản" : null, severe, family);
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
                        String.class));
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
                                + " AND ROUTINE_NAME = 'm01_verify_group1'",
                        Integer.class))
                .isZero();

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
                                + " UNION ALL SELECT care_level FROM care_level_history"
                                + " UNION ALL SELECT care_level FROM assessment) x"
                                + " WHERE c IS NOT NULL AND c NOT IN (SELECT code FROM catalog_item"
                                + " WHERE catalog_type = 'M01.CARE_LEVEL')",
                        Integer.class))
                .isZero();

        // Không còn khóa ngoại nào trỏ vào bảng cũ; trigger bất biến của health_item vẫn chặn đổi id
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE()"
                                + " AND REFERENCED_TABLE_NAME IN ('care_level', 'allergy_severity',"
                                + " 'information_source', 'care_need')",
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
