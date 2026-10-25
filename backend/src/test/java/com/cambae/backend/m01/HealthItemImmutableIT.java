package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.support.MySqlIntegrationTestBase;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * 001 T024 (DBR-04, SC-003; data-model mục 5): nội dung mục sức khỏe bất biến ở cơ sở dữ liệu. Trigger chặn {@code UPDATE}
 * nội dung và {@code EXCLUDED → ACTIVE}, chặn {@code DELETE}; ràng buộc {@code CHECK} giữ dị ứng có mức độ, mục có nội
 * dung, loại trừ có đủ ba cột; không có endpoint sửa, xóa. Chạy bằng {@code eldercare_app} như lúc vận hành.
 */
class HealthItemImmutableIT extends MySqlIntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    RequestMappingHandlerMapping mappings;

    private long resident() {
        jdbc.update(
                "INSERT INTO resident (profile_code, full_name, date_of_birth, gender, status, status_since,"
                        + " created_at, created_by) VALUES (?, 'Người thử', '1940-05-01', 'FEMALE', 'IN_RESIDENCE',"
                        + " NOW(6), NOW(6), 1)",
                UUID.randomUUID().toString().substring(0, 20));
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long severity() {
        return jdbc.queryForObject("SELECT id FROM allergy_severity WHERE name = 'Nhẹ'", Long.class);
    }

    private long source() {
        return jdbc.queryForObject("SELECT id FROM information_source WHERE name = 'Người thân'", Long.class);
    }

    private long allergen() {
        jdbc.update("INSERT INTO allergen (name, allergen_group) VALUES (?, 'FOOD')", "Dị nguyên " + UUID.randomUUID());
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    /** Bản ghi mặc định: bệnh nền nhập văn bản. */
    private long item(long residentId, String type, Long allergenId, String text, Long severityId) {
        jdbc.update(
                "INSERT INTO health_item (resident_id, type, allergen_id, free_text, severity_id, source_id,"
                        + " recorded_by, recorded_at) VALUES (?, ?, ?, ?, ?, ?, 3, NOW(6))",
                residentId,
                type,
                allergenId,
                text,
                severityId,
                source());
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private static String adminFails(String sql) throws SQLException {
        try (Connection c = adminConnection();
                Statement s = c.createStatement()) {
            s.execute(sql);
            return "";
        } catch (SQLException e) {
            return e.getMessage();
        }
    }

    @Test
    void DBR_04_updateOfContent_isBlockedByTrigger() {
        long id = item(resident(), "CHRONIC_CONDITION", null, "Tăng huyết áp", null);

        for (String column : new String[] {
            "free_text = 'Đã sửa'",
            "type = 'MEDICAL_HISTORY'",
            "source_id = 1",
            "recorded_by = 99",
            "recorded_at = NOW(6)",
            "severity_id = " + severity(),
            "resident_id = 1"
        }) {
            assertThatThrownBy(() -> jdbc.update("UPDATE health_item SET " + column + " WHERE id = ?", id))
                    .as(column)
                    .isInstanceOf(DataAccessException.class)
                    .hasMessageContaining("immutable");
        }
        assertThat(jdbc.queryForObject("SELECT free_text FROM health_item WHERE id = ?", String.class, id))
                .isEqualTo("Tăng huyết áp");
    }

    @Test
    void DBR_04_exclusionAndCheckFlags_areTheOnlyAllowedChanges() {
        long id = item(resident(), "ALLERGY", allergen(), null, severity());

        jdbc.update(
                "UPDATE health_item SET pending_medication_check = TRUE, pending_nutrition_check = TRUE WHERE id = ?",
                id);
        jdbc.update("UPDATE health_item SET pending_medication_check = FALSE WHERE id = ?", id);
        jdbc.update(
                "UPDATE health_item SET status = 'EXCLUDED', excluded_at = NOW(6), excluded_by = 3,"
                        + " exclusion_reason = 'Nhầm lẫn khi ghi nhận' WHERE id = ?",
                id);

        assertThat(jdbc.queryForObject("SELECT status FROM health_item WHERE id = ?", String.class, id))
                .isEqualTo("EXCLUDED");
    }

    @Test
    void DBR_04_excludedItem_cannotBeReinstatedOrRewritten() {
        long id = item(resident(), "MEDICAL_HISTORY", null, "Gãy xương đùi", null);
        jdbc.update(
                "UPDATE health_item SET status = 'EXCLUDED', excluded_at = NOW(6), excluded_by = 3,"
                        + " exclusion_reason = 'Ghi nhầm người' WHERE id = ?",
                id);

        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE health_item SET status = 'ACTIVE', excluded_at = NULL, excluded_by = NULL,"
                                + " exclusion_reason = NULL WHERE id = ?",
                        id))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("cannot be reinstated");
        assertThatThrownBy(() -> jdbc.update("UPDATE health_item SET exclusion_reason = 'Lý do khác' WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("cannot be reinstated");
    }

    @Test
    void DBR_04_delete_isBlockedForEveryoneButRetentionJob() throws SQLException {
        long id = item(resident(), "CHRONIC_CONDITION", null, "Đái tháo đường", null);

        assertThat(adminFails("DELETE FROM health_item WHERE id = " + id)).contains("retention job");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM health_item WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM health_item WHERE id = ?", Integer.class, id))
                .isEqualTo(1);
    }

    @Test
    void BR_M01_11_allergyWithoutSeverity_isRejectedByCheck() {
        long resident = resident();

        assertThatThrownBy(() -> item(resident, "ALLERGY", allergen(), null, null))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Check constraint");
        assertThatThrownBy(() -> item(resident, "ALLERGY", null, "Phấn hoa", null))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Check constraint");
    }

    @Test
    void FR_018_itemNeedsContent_andAllergenOnlyForAllergy() {
        long resident = resident();

        assertThatThrownBy(() -> item(resident, "CHRONIC_CONDITION", null, null, null))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Check constraint");
        assertThatThrownBy(() -> item(resident, "CHRONIC_CONDITION", allergen(), null, null))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Check constraint");
    }

    @Test
    void FR_018a_otherAllergy_isFlaggedNotAutoChecked() {
        long resident = resident();
        long other = item(resident, "ALLERGY", null, "Phấn hoa", severity());
        long fromCatalog = item(resident, "ALLERGY", allergen(), null, severity());
        long condition = item(resident, "CHRONIC_CONDITION", null, "Hen suyễn", null);

        assertThat(notAutoChecked(other)).isTrue();
        assertThat(notAutoChecked(fromCatalog)).isFalse();
        assertThat(notAutoChecked(condition)).isFalse();
    }

    @Test
    void DBR_04_exclusionNeedsAllThreeColumns() {
        long id = item(resident(), "CHRONIC_CONDITION", null, "Viêm khớp", null);

        assertThatThrownBy(() -> jdbc.update("UPDATE health_item SET status = 'EXCLUDED' WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Check constraint");
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE health_item SET status = 'EXCLUDED', excluded_at = NOW(6), excluded_by = 3 WHERE id = ?",
                        id))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Check constraint");
    }

    @Test
    void DBR_04_noEndpointEditsOrDeletesHealthItems() {
        for (RequestMappingInfo info : mappings.getHandlerMethods().keySet()) {
            boolean healthItemPath = info.getPathPatternsCondition().getPatterns().stream()
                    .anyMatch(p -> p.getPatternString().contains("/health-items"));
            if (healthItemPath) {
                assertThat(info.getMethodsCondition().getMethods())
                        .as(info.toString())
                        .doesNotContain(RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE);
            }
        }
    }

    private boolean notAutoChecked(long id) {
        return jdbc.queryForObject("SELECT not_auto_checked FROM health_item WHERE id = ?", Boolean.class, id);
    }
}
