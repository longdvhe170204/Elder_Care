package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

/**
 * 013 T003 (data-model mục 0, 3, 4; FR-001, FR-001a, FR-003, FR-011, BR-M12-01, BR-M15-03): hai bảng giữ của M12 sau
 * R-15 và danh mục mặc định trên {@code catalog_item}.
 */
class T003_BelongingSchemaIT extends MySqlIntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    private long insert(String sql, Object... args) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    for (int i = 0; i < args.length; i++) {
                        ps.setObject(i + 1, args[i]);
                    }
                    return ps;
                },
                keys);
        return keys.getKey().longValue();
    }

    private long catalogId(String kind, String code) {
        return jdbc.queryForObject(
                "SELECT id FROM catalog_item WHERE catalog_type = ? AND code = ?", Long.class, kind, code);
    }

    private long insertBelonging(Long quantity, Long cash) {
        long residentId = TestUsers.nextId();
        return insert(
                """
                INSERT INTO belonging (resident_id, type_id, description, quantity, cash_amount, status,
                    holder_type, holder_id, condition_level_id, created_by, created_at)
                VALUES (?, ?, 'Điện thoại', ?, ?, 'HELD', 'STAFF', 1, ?, 1, NOW(6))
                """,
                residentId,
                catalogId("M12.BELONGING_TYPE", "PHONE"),
                quantity,
                cash,
                catalogId("M12.CONDITION_LEVEL", "GOOD"));
    }

    private long insertHandover(long belongingId) {
        long residentId =
                jdbc.queryForObject("SELECT resident_id FROM belonging WHERE id = ?", Long.class, belongingId);
        return insert("""
                INSERT INTO belonging_handover (belonging_id, resident_id, kind, target_status, occurred_at, quantity,
                    condition_level_id, valuable, photo_file_ids, recorded_by, recorded_at)
                VALUES (?, ?, 'REPORT_LOST', 'LOST', NOW(6), 1, ?, TRUE, JSON_ARRAY(), 1, NOW(6))
                """, belongingId, residentId, catalogId("M12.CONDITION_LEVEL", "GOOD"));
    }

    @Test
    void FR_001_defaultBelongingTypes_withCashAndConsentFlags() {
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM catalog_item WHERE catalog_type = 'M12.BELONGING_TYPE' AND code IN"
                                + " ('PHONE','GLASSES','CLOTHING','WHEELCHAIR','DOCUMENTS','PERSONAL_ITEMS','JEWELRY',"
                                + " 'CASH','OTHER_ASSETS')",
                        Integer.class))
                .isEqualTo(9);
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_EXTRACT(attributes, '$.hasCashAmount') = TRUE AND"
                                + " JSON_EXTRACT(attributes, '$.requiresConsentForUse') = TRUE FROM catalog_item"
                                + " WHERE catalog_type = 'M12.BELONGING_TYPE' AND code = 'CASH'",
                        Boolean.class))
                .isTrue();
    }

    @Test
    void FR_001a_fourDefaultLevels_exactlyOneUnusable() {
        assertThat(jdbc.queryForList(
                        "SELECT code FROM catalog_item WHERE catalog_type = 'M12.CONDITION_LEVEL' AND code IS NOT NULL"
                                + " ORDER BY sort_order",
                        String.class))
                .startsWith("GOOD", "SIGNS_OF_USE", "PARTLY_DAMAGED", "UNUSABLE");
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes)"
                                + " VALUES ('M12.CONDITION_LEVEL', 'M12', 'UNUSABLE', 'Hỏng hẳn', 9, 'ACTIVE',"
                                + " JSON_OBJECT('unusable', TRUE))"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_003_quantityOrCashAmount_exactlyOne() {
        assertThat(insertBelonging(1L, null)).isPositive();
        assertThat(insertBelonging(null, 2_000_000L)).isPositive();
        assertThatThrownBy(() -> insertBelonging(1L, 2_000_000L)).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> insertBelonging(null, null)).isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_001_typeReferencedByBelonging_cannotBeDeleted() throws SQLException {
        insertBelonging(1L, null);
        try (Connection c = adminConnection();
                Statement st = c.createStatement()) {
            assertThatThrownBy(() -> st.executeUpdate(
                            "DELETE FROM catalog_item WHERE catalog_type = 'M12.BELONGING_TYPE' AND code = 'PHONE'"))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void BR_M12_01_handover_isAppendOnly_exceptIncidentLinkedOnce() {
        long handoverId = insertHandover(insertBelonging(1L, null));

        assertThatThrownBy(() -> jdbc.update("UPDATE belonging_handover SET reason = 'sửa' WHERE id = ?", handoverId))
                .isInstanceOf(DataAccessException.class);
        assertThat(jdbc.update("UPDATE belonging_handover SET incident_id = 501 WHERE id = ?", handoverId))
                .isEqualTo(1);
        assertThatThrownBy(
                        () -> jdbc.update("UPDATE belonging_handover SET incident_id = 502 WHERE id = ?", handoverId))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM belonging_handover WHERE id = ?", handoverId))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void retentionAccount_mayDeleteHandoverThenBelonging() throws SQLException {
        long belongingId = insertBelonging(1L, null);
        long handoverId = insertHandover(belongingId);
        try (Connection c = retentionConnection();
                Statement st = c.createStatement()) {
            assertThat(st.executeUpdate("DELETE FROM belonging_handover WHERE id = " + handoverId))
                    .isEqualTo(1);
            assertThat(st.executeUpdate("DELETE FROM belonging WHERE id = " + belongingId))
                    .isEqualTo(1);
        }
    }
}
