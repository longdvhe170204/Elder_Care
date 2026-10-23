package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.support.MySqlIntegrationTestBase;
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
        jdbc.update(
                "INSERT INTO resident_status_history (resident_id, from_status, to_status, command, occurred_at,"
                        + " performed_by, by_system, reason) VALUES (?, 'IN_RESIDENCE', 'HOSPITALIZED', ?, NOW(6), 1,"
                        + " ?, ?)",
                residentId,
                command,
                bySystem,
                reason);
    }

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

        assertThatThrownBy(() ->
                        jdbc.update("UPDATE resident_status_history SET reason = 'sửa' WHERE resident_id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .rootCause()
                .hasMessageContaining("UPDATE command denied");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM resident_status_history WHERE resident_id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .rootCause()
                .hasMessageContaining("DELETE command denied");
        assertThat(adminFails("UPDATE resident_status_history SET reason = 'sua' WHERE resident_id = " + id))
                .contains("append-only");
        assertThat(adminFails("DELETE FROM resident_status_history WHERE resident_id = " + id))
                .contains("removed only by the retention job");
    }

    @Test
    void FR_046_statusHistory_reasonRequiredUnlessSystemOrProfileCreation() {
        long id = resident("Người A", null, null, "IN_RESIDENCE");

        assertThatThrownBy(() -> history(id, "TRANSFER_OUT", false, null))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("chk_resident_status_history_reason");
        history(id, "ROLL_CALL_DEPARTURE", true, null);
        history(id, "CREATE_PROFILE", false, null);
    }

    @Test
    void FR_053_careLevel_fixedThreeLevelsAndOneEntryPerInstant() throws SQLException {
        assertThat(jdbc.queryForList("SELECT code FROM care_level ORDER BY display_order", String.class))
                .containsExactly("BASIC", "REGULAR", "SPECIAL");
        long id = resident("Người A", null, null, "IN_RESIDENCE");
        String insert = "INSERT INTO care_level_history (resident_id, care_level, effective_from, basis_type, basis_id,"
                + " recorded_at, recorded_by) VALUES (?, ?, '2026-10-01 00:00:00', 'ASSESSMENT', 7, NOW(6), 1)";
        jdbc.update(insert, id, "REGULAR");

        assertThatThrownBy(() -> jdbc.update(insert, id, "SPECIAL")).isInstanceOf(DuplicateKeyException.class);
        assertThatThrownBy(() ->
                        jdbc.update("UPDATE care_level_history SET care_level = 'BASIC' WHERE resident_id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .rootCause()
                .hasMessageContaining("UPDATE command denied");
        assertThat(adminFails("UPDATE care_level_history SET care_level = 'BASIC' WHERE resident_id = " + id))
                .contains("append-only");
    }

    @Test
    void FR_052_currentCareLevel_mustBeOneOfTheThreeLevels() {
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO resident (profile_code, full_name, date_of_birth, gender, status, status_since,"
                                + " current_care_level, created_at, created_by) VALUES ('X2', 'A', '1940-01-01',"
                                + " 'MALE', 'IN_RESIDENCE', NOW(6), 'PLATINUM', NOW(6), 1)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
