package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.ResidentHistoryRows;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 001 T005 (data-model mục 2, 3; 9): bảo đảm ở cơ sở dữ liệu của hồ sơ người cao tuổi, lịch sử trạng thái và lịch sử mức
 * chăm sóc: DBR-01 (mã hồ sơ và CCCD duy nhất, một trạng thái hiện tại), lịch sử chỉ thêm (BR-M01-04, FR-046), lý do
 * bắt buộc, mức chăm sóc cố định và `UNIQUE (resident_id, effective_from)` (FR-053). Chạy bằng tài khoản ứng dụng
 * `eldercare_app` như lúc vận hành, nên cũng kiểm quyền: không có `DELETE`.
 */
class ResidentSchemaIT extends MySqlIntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    private long resident(String fullName, String documentType, String documentNumber, String status) {
        jdbc.update(
                "INSERT INTO resident (profile_code, full_name, date_of_birth, gender, id_document_type,"
                        + " id_document_number, status, status_since, created_at, created_by)"
                        + " VALUES (?, ?, '1940-05-01', 'FEMALE', ?, ?, ?, NOW(6), NOW(6), 1)",
                UUID.randomUUID().toString().substring(0, 20),
                fullName,
                documentType,
                documentNumber,
                status);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    /** Chạy lệnh bằng tài khoản quản trị (có quyền) để kiểm trigger; trả thông báo lỗi. */
    private static String adminFails(String sql) throws SQLException {
        try (Connection c = adminConnection();
                Statement s = c.createStatement()) {
            s.execute(sql);
            return "";
        } catch (SQLException e) {
            return e.getMessage();
        }
    }

    private static String number() {
        return String.valueOf(System.nanoTime());
    }

    private void history(long residentId, String command, boolean bySystem, String reason) {
        ResidentHistoryRows.insertStatus(
                jdbc,
                null,
                residentId,
                "IN_RESIDENCE",
                "HOSPITALIZED",
                command,
                java.sql.Timestamp.from(java.time.Instant.now()),
                1,
                bySystem,
                reason);
    }

    private static final String STATUS_ROWS =
            "status_history WHERE entity_type = 'M01.RESIDENT' AND attribute = 'STATUS'";
    private static final String CARE_LEVEL_ROWS =
            "status_history WHERE entity_type = 'M01.RESIDENT' AND attribute = 'CARE_LEVEL'";

    @Test
    void DBR_01_profileCode_isUnique() {
        jdbc.update("INSERT INTO resident (profile_code, full_name, date_of_birth, gender, status, status_since,"
                + " created_at, created_by) VALUES ('NCT-2026-900001', 'A', '1940-01-01', 'MALE',"
                + " 'ADMISSION_IN_PROGRESS', NOW(6), NOW(6), 1)");

        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO resident (profile_code, full_name, date_of_birth, gender, status, status_since,"
                                + " created_at, created_by) VALUES ('NCT-2026-900001', 'B', '1940-01-01', 'MALE',"
                                + " 'ADMISSION_IN_PROGRESS', NOW(6), NOW(6), 1)"))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void DBR_01_citizenId_uniqueWhileProfileNotInFinalStatus() {
        String id = number();
        long first = resident("Người A", "CITIZEN_ID", id, "IN_RESIDENCE");

        assertThatThrownBy(() -> resident("Người B", "CITIZEN_ID", id, "ADMISSION_IN_PROGRESS"))
                .isInstanceOf(DuplicateKeyException.class);

        jdbc.update("UPDATE resident SET status = 'RESIDENCY_ENDED' WHERE id = ?", first);
        long second = resident("Người B", "CITIZEN_ID", id, "ADMISSION_IN_PROGRESS");

        assertThat(second).isGreaterThan(first);
    }

    @Test
    void DBR_01_otherDocumentNumber_mayRepeat() {
        String id = number();
        resident("Người A", "OTHER", id, "IN_RESIDENCE");

        resident("Người B", "OTHER", id, "IN_RESIDENCE");
    }

    @Test
    void FR_001_documentTypeAndNumber_comeTogether() {
        assertThatThrownBy(() -> resident("Người A", "CITIZEN_ID", null, "IN_RESIDENCE"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("chk_resident_document");
        assertThatThrownBy(() -> resident("Người A", null, number(), "IN_RESIDENCE"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("chk_resident_document");
        resident("Người A", null, null, "IN_RESIDENCE");
    }

    @Test
    void FR_005_fullNameMatch_ignoresCaseAndExtraSpacesButNotDiacritics() {
        long a = resident("  Nguyễn   Văn  An ", null, null, "IN_RESIDENCE");
        long b = resident("NGUYỄN VĂN AN", null, null, "IN_RESIDENCE");
        long c = resident("Nguyen Van An", null, null, "IN_RESIDENCE");

        List<String> keys = jdbc.queryForList(
                "SELECT full_name_match FROM resident WHERE id IN (?, ?, ?) ORDER BY id", String.class, a, b, c);

        assertThat(keys.get(0)).isEqualTo("Nguyễn Văn An").isEqualToIgnoringCase(keys.get(1));
        long same = jdbc.queryForObject(
                "SELECT COUNT(*) FROM resident WHERE full_name_match = ? AND date_of_birth = '1940-05-01'",
                Long.class,
                "nguyễn văn an");
        assertThat(same).isGreaterThanOrEqualTo(2);
        long withoutDiacritics = jdbc.queryForObject(
                "SELECT COUNT(*) FROM resident WHERE id = ? AND full_name_match = ?", Long.class, c, "Nguyễn Văn An");
        assertThat(withoutDiacritics).isZero();
    }

    @Test
    void DBR_01_status_isRequiredAndLimitedToEightCodes() {
        assertThatThrownBy(() -> resident("Người A", null, null, "NOT_A_STATUS"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> resident("Người A", null, null, null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("status");
    }

    @Test
    void BR_M01_05_resident_isNeverDeletedByApplicationAccount() throws SQLException {
        long id = resident("Người A", null, null, "IN_RESIDENCE");

        assertThatThrownBy(() -> jdbc.update("DELETE FROM resident WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .rootCause()
                .hasMessageContaining("DELETE command denied");
        // Kể cả tài khoản có quyền DELETE (quản trị) cũng bị trigger chặn: chỉ eldercare_retention xóa được (FR-051)
        assertThat(adminFails("DELETE FROM resident WHERE id = " + id)).contains("removed only by the retention job");
    }

    @Test
    void BR_M01_04_statusHistory_isAppendOnly() throws SQLException {
        long id = resident("Người A", null, null, "IN_RESIDENCE");
        history(id, "TRANSFER_OUT", false, "Chuyển viện cấp cứu");

        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE status_history SET reason = 'sửa' WHERE entity_type = 'M01.RESIDENT'"
                                + " AND attribute = 'STATUS' AND entity_id = ?",
                        id))
                .isInstanceOf(DataAccessException.class)
                .rootCause()
                .hasMessageContaining("UPDATE command denied");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM " + STATUS_ROWS + " AND entity_id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .rootCause()
                .hasMessageContaining("DELETE command denied");
        assertThat(adminFails("UPDATE status_history SET reason = 'sua' WHERE entity_type = 'M01.RESIDENT'"
                        + " AND attribute = 'STATUS' AND entity_id = " + id))
                .contains("append-only");
        assertThat(adminFails("DELETE FROM " + STATUS_ROWS + " AND entity_id = " + id))
                .contains("removed only by the retention job");
    }

    // FR-046 (lý do bắt buộc trừ lệnh do hệ thống, Tạo hồ sơ): từ 001 T084 không còn CHECK ở bảng, kiểm ở
    // StatusHistoryApi theo luật của thuộc tính STATUS (StatusHistoryIT.FR_046_…).

    @Test
    void FR_053_careLevel_fixedThreeLevelsAndOneEntryPerInstant() throws SQLException {
        assertThat(jdbc.queryForList(
                        "SELECT code FROM catalog_item WHERE catalog_type = 'M01.CARE_LEVEL' ORDER BY sort_order",
                        String.class))
                .containsExactly("BASIC", "REGULAR", "SPECIAL");
        long id = resident("Người A", null, null, "IN_RESIDENCE");
        java.time.Instant from = java.time.Instant.parse("2026-10-01T00:00:00Z");
        ResidentHistoryRows.insertCareLevel(jdbc, id, "REGULAR", from, "ASSESSMENT", 7, 1);

        // M01.CLH:<residentId>:<từ> thay UNIQUE (resident_id, effective_from)
        assertThatThrownBy(() -> ResidentHistoryRows.insertCareLevel(jdbc, id, "SPECIAL", from, "ASSESSMENT", 7, 1))
                .isInstanceOf(DuplicateKeyException.class);
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE status_history SET value = 'BASIC' WHERE entity_type = 'M01.RESIDENT'"
                                + " AND attribute = 'CARE_LEVEL' AND entity_id = ?",
                        id))
                .isInstanceOf(DataAccessException.class)
                .rootCause()
                .hasMessageContaining("UPDATE command denied");
        assertThat(adminFails("UPDATE status_history SET value = 'BASIC' WHERE entity_type = 'M01.RESIDENT'"
                        + " AND attribute = 'CARE_LEVEL' AND entity_id = " + id))
                .contains("append-only");
        assertThat(adminFails("DELETE FROM " + CARE_LEVEL_ROWS + " AND entity_id = " + id))
                .contains("removed only by the retention job");
    }

    @Test
    void FR_052_careLevelCatalog_isExactlyTheThreeLevelsTheBackendAccepts() {
        // Từ 001 T084 (data-model 0.1) bỏ khóa ngoại current_care_level → care_level: mã được kiểm ở backend (enum
        // CareLevel), nên danh mục chung phải khớp đúng ba mức đó.
        assertThat(jdbc.queryForList(
                        "SELECT code FROM catalog_item WHERE catalog_type = 'M01.CARE_LEVEL' ORDER BY sort_order",
                        String.class))
                .containsExactly(java.util.Arrays.stream(com.eldercare.backend.resident.api.dto.CareLevel.values())
                        .map(Enum::name)
                        .toArray(String[]::new));
    }
}
