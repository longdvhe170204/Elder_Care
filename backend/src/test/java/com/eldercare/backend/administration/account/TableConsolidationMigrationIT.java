package com.eldercare.backend.administration.account;

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
 * 002 T098 (research R-15 mục 5, data-model 002 mục 0.1): {@code V002_95} chép đủ dòng của 12 bảng cũ sang hai bảng gộp
 * và bảng chung rồi bỏ bảng cũ. Chạy trên schema tạm: migration tới {@code 2.90} (Flyway quét cả {@code db/migration/demo}
 * nên có dữ liệu mẫu {@code demo/V002_90}, như R-15 mục 5), ghi thêm dữ liệu cũ của mọi bảng, chạy {@code 2.95}.
 */
class TableConsolidationMigrationIT extends MySqlIntegrationTestBase {

    private static final String SUFFIX = "m002_95";
    private static final List<String> LEGACY_TABLES = List.of(
            "role",
            "role_assignment",
            "permission_restriction",
            "additional_grant",
            "field_visibility_rule",
            "professional_activity",
            "professional_activity_practice_scope",
            "license_expiry_warning",
            "login_qr_code",
            "bound_device",
            "account_status_history",
            "pending_review_record",
            "permission_data_error");

    private static JdbcTemplate db;
    private static long manager;
    private static long nurse;
    private static long resident;
    private static Map<String, Long> before;
    private static long oldAssignmentId;
    private static long oldBindingId;
    private static long medicationActivityId;

    @BeforeAll
    static void migrateLegacyData() {
        Flyway upTo294 = Flyway.configure()
                .configuration(scratchFlyway(SUFFIX).getConfiguration())
                .target("2.90")
                .load();
        upTo294.migrate();
        db = new JdbcTemplate(upTo294.getConfiguration().getDataSource());

        manager = account("m.manager", "STAFF", 1L, null);
        nurse = account("m.nurse", "STAFF", 2L, null);
        resident = account("m.resident", "RESIDENT", null, 3L);
        db.update(
                "INSERT INTO account_status_history (account_id, status, effective_from, actor_id)"
                        + " VALUES (?, 'ACTIVE', '2026-01-01 00:00:00.000000', NULL)",
                nurse);
        db.update(
                "INSERT INTO account_status_history (account_id, status, lock_cause, effective_from, actor_id,"
                        + " audit_log_id) VALUES (?, 'LOCKED', 'MANUAL', '2026-02-01 08:30:00.123456', ?, 99)",
                nurse,
                manager);
        db.update(
                "INSERT INTO role_assignment (account_id, role, effective_from, effective_to, assigned_by, reason)"
                        + " VALUES (?, 'AC-04', '2026-01-01', '2026-01-15', NULL, 'Khởi tạo')",
                nurse);
        db.update(
                "INSERT INTO role_assignment (account_id, role, effective_from, assigned_by, reason)"
                        + " VALUES (?, 'AC-01', '2026-01-01', NULL, 'Khởi tạo')",
                manager);
        oldAssignmentId = db.queryForObject("SELECT MAX(id) FROM role_assignment", Long.class);
        db.update(
                "INSERT INTO permission_restriction (applies_to, role, function_code, operation, kind, metric_group,"
                        + " channel, effective_from, actor_id, reason) VALUES ('ROLE', 'AC-02', 'DASHBOARD_REPORT',"
                        + " 'VIEW', 'REMOVE_OPERATION', 'CARE', 'DASHBOARD', '2026-01-02', ?, 'Thu hẹp')",
                manager);
        db.update(
                "INSERT INTO permission_restriction (applies_to, account_id, function_code, operation, kind,"
                        + " effective_from, effective_to, actor_id, reason) VALUES ('ACCOUNT', ?, 'INCIDENT', 'CREATE',"
                        + " 'LOWER_SCOPE', '2026-01-02', '2026-01-03', ?, 'Thu hẹp tài khoản')",
                nurse,
                manager);
        db.update(
                "INSERT INTO additional_grant (account_id, function_code, operation, effective_from, actor_id, reason)"
                        + " VALUES (?, 'CARE_PLAN', 'APPROVE', '2026-01-05', ?, 'Gán thêm')",
                nurse,
                manager);
        db.update(
                "INSERT INTO login_qr_code (account_id, code_hash, created_by, created_at, expires_at, used_at,"
                        + " used_device_id) VALUES (?, UNHEX(SHA2('qr-used', 256)), ?, '2026-01-01', '2026-01-02',"
                        + " '2026-01-01 01:00', 'dev-1')",
                resident,
                manager);
        db.update(
                "INSERT INTO login_qr_code (account_id, code_hash, created_by, created_at, expires_at, revoked_at)"
                        + " VALUES (?, UNHEX(SHA2('qr-revoked', 256)), ?, '2026-01-03', '2026-01-04', '2026-01-03 05:00')",
                resident,
                manager);
        long usedQr = db.queryForObject("SELECT MIN(id) FROM login_qr_code", Long.class);
        db.update(
                "INSERT INTO bound_device (account_id, device_id, device_token_hash, bound_at, unbound_at, unbound_by,"
                        + " unbind_reason, unbind_note) VALUES (?, 'dev-0', UNHEX(SHA2('tok-0', 256)), '2025-12-01',"
                        + " '2026-01-01 01:00', ?, 'MANUAL', 'Mất máy')",
                resident,
                manager);
        db.update(
                "INSERT INTO bound_device (account_id, device_id, device_token_hash, bound_at, login_qr_code_id)"
                        + " VALUES (?, 'dev-1', UNHEX(SHA2('tok-1', 256)), '2026-01-01 01:00', ?)",
                resident,
                usedQr);
        oldBindingId = db.queryForObject("SELECT MAX(id) FROM bound_device", Long.class);
        medicationActivityId = db.queryForObject(
                "SELECT id FROM professional_activity WHERE code = 'MEDICATION_ADMINISTRATION'", Long.class);
        db.update(
                "INSERT INTO professional_activity_practice_scope (activity_id, practice_scope_id) VALUES (?, 77), (?, 78)",
                medicationActivityId,
                medicationActivityId);
        db.update("INSERT INTO license_expiry_warning (kind, license_id, expiry_date, warned_at)"
                + " VALUES ('FACILITY', 5, '2026-06-30', '2026-05-01')");
        db.update(
                "INSERT INTO pending_review_record (record_type, content, idempotency_key, recorder_id, device_id,"
                        + " device_time, synced_at, last_connected_at, pending_reason, floor_area_id)"
                        + " VALUES ('M04.CARE_TASK_RESULT', '{\"taskId\": 4}', '11111111-1111-1111-1111-111111111111', ?,"
                        + " 'dev-9', '2026-01-10 07:00', '2026-01-10 09:00', '2026-01-10 06:00', 'LATER_THAN_SYNC', 3)",
                nurse);
        db.update(
                "INSERT INTO pending_review_record (record_type, content, idempotency_key, recorder_id, device_id,"
                        + " device_time, synced_at, last_connected_at, pending_reason, status, decided_by, decided_at,"
                        + " rejection_reason, version) VALUES ('M04.CARE_TASK_RESULT', '{\"taskId\": 5}',"
                        + " '22222222-2222-2222-2222-222222222222', ?, 'dev-9', '2026-01-10 07:00', '2026-01-10 09:00',"
                        + " '2026-01-10 06:00', 'EXCEEDS_CFG_M15_08', 'REJECTED', ?, '2026-01-11 10:00', 'Sai giờ', 1)",
                nurse,
                manager);
        db.update("INSERT INTO permission_data_error (source, record_id, error_kind, first_at, last_at, occurrences)"
                + " VALUES ('M09.SHIFT', 8, 'MULTIPLE_SHIFT_LEADS', '2026-01-01', '2026-01-02', 3)");
        db.update("INSERT INTO permission_data_error (source, record_id, error_kind, first_at, last_at, fixed_at)"
                + " VALUES ('M03.SCOPE', NULL, 'MISSING_RECORD', '2026-01-01', '2026-01-01', '2026-01-04')");

        before = Map.of(
                "role", count("role"),
                "field_visibility_rule", count("field_visibility_rule"),
                "professional_activity", count("professional_activity"),
                "role_assignment", count("role_assignment"));

        Flyway.configure()
                .configuration(upTo294.getConfiguration())
                .target("2.95")
                .load()
                .migrate();
    }

    @AfterAll
    static void dropSchema() {
        dropScratchSchema(SUFFIX);
    }

    @Test
    void legacyTablesAreDropped() {
        assertThat(db.queryForList(
                        "SELECT LOWER(TABLE_NAME) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()",
                        String.class))
                .doesNotContainAnyElementsOf(LEGACY_TABLES)
                .contains("permission_assignment", "device_credential", "account", "permission_matrix");
    }

    @Test
    void permissionAssignment_keepsRoleAssignmentIds_andCopiesRestrictionsAndGrants() {
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM permission_assignment WHERE kind = 'ROLE_ASSIGNMENT'", Long.class))
                .isEqualTo(before.get("role_assignment"));
        assertThat(db.queryForMap(
                        "SELECT kind, applies_to, account_id, role, actor_id, effective_to FROM permission_assignment"
                                + " WHERE id = ?",
                        oldAssignmentId))
                .containsEntry("kind", "ROLE_ASSIGNMENT")
                .containsEntry("applies_to", "ACCOUNT")
                .containsEntry("account_id", manager)
                .containsEntry("role", "AC-01")
                .containsEntry("effective_to", null);
        assertThat(db.queryForMap(
                        "SELECT applies_to, role, account_id, function_code, operation, restriction_kind, metric_group,"
                                + " channel, actor_id FROM permission_assignment WHERE kind = 'RESTRICTION'"
                                + " AND applies_to = 'ROLE'"))
                .containsEntry("role", "AC-02")
                .containsEntry("account_id", null)
                .containsEntry("function_code", "DASHBOARD_REPORT")
                .containsEntry("restriction_kind", "REMOVE_OPERATION")
                .containsEntry("metric_group", "CARE")
                .containsEntry("channel", "DASHBOARD")
                .containsEntry("actor_id", manager);
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM permission_assignment WHERE kind = 'RESTRICTION' AND applies_to = 'ACCOUNT'"
                                + " AND account_id = ? AND restriction_kind = 'LOWER_SCOPE' AND effective_to IS NOT NULL",
                        Long.class,
                        nurse))
                .isEqualTo(1);
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM permission_assignment WHERE kind = 'ADDITIONAL_GRANT' AND account_id = ?"
                                + " AND function_code = 'CARE_PLAN' AND operation = 'APPROVE'",
                        Long.class,
                        nurse))
                .isEqualTo(1);
    }

    @Test
    void deviceCredential_keepsBoundDeviceIds_andRelinksQrCode() {
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM device_credential WHERE kind = 'LOGIN_QR_CODE' AND account_id = ?",
                        Long.class,
                        resident))
                .isEqualTo(2);
        Map<String, Object> bound = db.queryForMap(
                "SELECT b.kind, b.device_id, b.ended_at, q.kind AS qr_kind, q.device_id AS qr_device,"
                        + " q.secret_hash = UNHEX(SHA2('qr-used', 256)) AS same_hash, b.active_account_id"
                        + " FROM device_credential b JOIN device_credential q ON q.id = b.login_qr_code_id"
                        + " WHERE b.id = ?",
                oldBindingId);
        assertThat(bound)
                .containsEntry("kind", "BOUND_DEVICE")
                .containsEntry("device_id", "dev-1")
                .containsEntry("ended_at", null)
                .containsEntry("qr_kind", "LOGIN_QR_CODE")
                .containsEntry("qr_device", "dev-1")
                .containsEntry("same_hash", 1L)
                .containsEntry("active_account_id", resident);
        assertThat(db.queryForMap(
                        "SELECT end_reason, end_note, ended_by FROM device_credential WHERE kind = 'BOUND_DEVICE'"
                                + " AND device_id = 'dev-0'"))
                .containsEntry("end_reason", "MANUAL")
                .containsEntry("end_note", "Mất máy")
                .containsEntry("ended_by", manager);
    }

    @Test
    void catalogs_moveToCatalogItem() {
        assertThat(db.queryForObject("SELECT COUNT(*) FROM catalog_item WHERE catalog_type = 'M15.ROLE'", Long.class))
                .isEqualTo(before.get("role"))
                .isEqualTo(10);
        assertThat(db.queryForMap("SELECT name, status, JSON_UNQUOTE(attributes->'$.accountType') AS account_type,"
                        + " JSON_UNQUOTE(attributes->'$.executionScope') AS execution_scope,"
                        + " attributes->'$.permissionVersion' = 0 AS version_zero"
                        + " FROM catalog_item WHERE catalog_type = 'M15.ROLE' AND code = 'AC-16'"))
                .containsEntry("name", "Người cao tuổi")
                .containsEntry("status", "ACTIVE")
                .containsEntry("account_type", "RESIDENT")
                .containsEntry("execution_scope", "SELF")
                .containsEntry("version_zero", 1L);
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM catalog_item WHERE catalog_type = 'M15.FIELD_VISIBILITY'", Long.class))
                .isEqualTo(before.get("field_visibility_rule"));
        assertThat(db.queryForObject(
                        "SELECT JSON_UNQUOTE(attributes->'$.visibility') FROM catalog_item"
                                + " WHERE catalog_type = 'M15.FIELD_VISIBILITY' AND code = 'AC-07:HEALTH'",
                        String.class))
                .isEqualTo("NONE");
        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM catalog_item WHERE catalog_type = 'M15.PROFESSIONAL_ACTIVITY'",
                        Long.class))
                .isEqualTo(before.get("professional_activity"));
        assertThat(db.queryForMap(
                        "SELECT name, JSON_UNQUOTE(attributes->'$.functionCode') AS function_code,"
                                + " attributes->'$.isDefault' = TRUE AS is_default,"
                                + " attributes->'$.requiresFacilityScope' = FALSE AS no_facility,"
                                + " JSON_CONTAINS(attributes->'$.practiceScopeIds', '[77, 78]') AS scopes,"
                                + " attributes->'$.legacy.id' = ? AS legacy"
                                + " FROM catalog_item WHERE catalog_type = 'M15.PROFESSIONAL_ACTIVITY'"
                                + " AND code = 'MEDICATION_ADMINISTRATION'",
                        medicationActivityId))
                .containsEntry("name", "Phát thuốc và xác nhận liều")
                .containsEntry("function_code", "MEDICATION_ADMINISTRATION")
                .containsEntry("is_default", 1L)
                .containsEntry("no_facility", 1L)
                .containsEntry("scopes", 1L)
                .containsEntry("legacy", 1L);
    }

    @Test
    void accountStatusHistory_movesToStatusHistory() {
        List<Map<String, Object>> rows = db.queryForList(
                "SELECT kind, value, actor_id, by_system, unique_key, JSON_TYPE(details->'$.lockCause') AS cause_type,"
                        + " details->'$.auditLogId' AS audit_log_id FROM status_history"
                        + " WHERE entity_type = 'M15.ACCOUNT' AND entity_id = ? AND attribute = 'STATUS'"
                        + " ORDER BY starts_at",
                nurse);
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0))
                .containsEntry("kind", "POINT")
                .containsEntry("value", "ACTIVE")
                .containsEntry("by_system", true)
                .containsEntry("cause_type", "NULL");
        assertThat(rows.get(1))
                .containsEntry("value", "LOCKED")
                .containsEntry("actor_id", manager)
                .containsEntry("by_system", false)
                .containsEntry("cause_type", "STRING")
                .containsEntry("unique_key", "M15.ASH:" + nurse + ":2026-02-01T08:30:00.123456");
        assertThat(rows.get(1).get("audit_log_id").toString()).isEqualTo("99");
    }

    @Test
    void pendingReviewAndDataErrors_moveToSimpleRecord() {
        List<Map<String, Object>> pending =
                db.queryForList("SELECT status, unique_key, locked_at IS NOT NULL AS locked, created_by,"
                        + " JSON_UNQUOTE(content->'$.recordType') AS record_type, content->'$.content.taskId' AS task,"
                        + " content->'$.floorAreaId' AS floor, JSON_UNQUOTE(content->'$.rejectionReason') AS reason"
                        + " FROM simple_record WHERE kind = 'M15.PENDING_REVIEW_RECORD' ORDER BY id");
        assertThat(pending).hasSize(2);
        assertThat(pending.get(0))
                .containsEntry("status", "PENDING_REVIEW")
                .containsEntry("unique_key", "M15.PRR:" + nurse + ":11111111-1111-1111-1111-111111111111")
                .containsEntry("locked", 0L)
                .containsEntry("created_by", nurse)
                .containsEntry("record_type", "M04.CARE_TASK_RESULT");
        assertThat(pending.get(0).get("task").toString()).isEqualTo("4");
        assertThat(pending.get(0).get("floor").toString()).isEqualTo("3");
        assertThat(pending.get(1))
                .containsEntry("status", "REJECTED")
                .containsEntry("locked", 1L)
                .containsEntry("reason", "Sai giờ");

        List<Map<String, Object>> errors =
                db.queryForList("SELECT status, unique_key, content->'$.occurrences' AS occurrences FROM simple_record"
                        + " WHERE kind = 'M15.PERMISSION_DATA_ERROR' ORDER BY id");
        assertThat(errors).hasSize(2);
        assertThat(errors.get(0))
                .containsEntry("status", "OPEN")
                .containsEntry("unique_key", "M15.PDE:M09.SHIFT|8|MULTIPLE_SHIFT_LEADS");
        assertThat(errors.get(0).get("occurrences").toString()).isEqualTo("3");
        assertThat(errors.get(1)).containsEntry("status", "FIXED").containsEntry("unique_key", null);

        assertThat(db.queryForObject(
                        "SELECT COUNT(*) FROM reminder_marker WHERE kind = 'M15.LICENSE_EXPIRY_WARNING'"
                                + " AND entity_type = 'FACILITY' AND entity_id = 5 AND marker = '2026-06-30'",
                        Long.class))
                .isEqualTo(1);
    }

    private static long account(String username, String type, Long staffId, Long residentId) {
        db.update(
                "INSERT INTO account (username, type, staff_id, resident_id, status, password_hash,"
                        + " must_change_password, inactivity_reference_at, created_at)"
                        + " VALUES (?, ?, ?, ?, 'ACTIVE', 'x', FALSE, '2026-01-01', '2026-01-01')",
                username,
                type,
                staffId,
                residentId);
        return db.queryForObject("SELECT id FROM account WHERE username = ?", Long.class, username);
    }

    private static long count(String table) {
        return db.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }
}
