package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 004 T071, T110 (data-model mục 0, 15; FR-010 của 000, BR-M15-03; constitution XII): bảng gộp chứa nhóm 2 và nhóm 3 của 004 khai
 * {@code DataGroup=MULTIPLE}, có cột {@code locked_at} và hai trigger {@code row_lock}, {@code no_delete}.
 */
class M02RecordGroupTriggerIT extends MySqlIntegrationTestBase {

    private static final List<String> MULTIPLE_GROUP_TABLES =
            List.of("admission_record", "contract_version", "absence", "residency_ending");

    @Test
    void XII_multipleGroupTables_declareTheGroup_andHaveRowLockTriggers() throws SQLException {
        Set<String> triggers = new HashSet<>();
        Map<String, String> comments = new HashMap<>();
        try (Connection c = adminConnection();
                Statement st = c.createStatement()) {
            try (ResultSet rs = st.executeQuery(
                    "SELECT LOWER(TRIGGER_NAME) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA = DATABASE()")) {
                while (rs.next()) {
                    triggers.add(rs.getString(1));
                }
            }
            try (ResultSet rs = st.executeQuery(
                    "SELECT LOWER(TABLE_NAME), TABLE_COMMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()")) {
                while (rs.next()) {
                    comments.put(rs.getString(1), rs.getString(2));
                }
            }
        }

        assertThat(MULTIPLE_GROUP_TABLES).allSatisfy(table -> {
            assertThat(comments.get(table)).contains("DataGroup=MULTIPLE");
            assertThat(triggers).contains("trg_" + table + "_row_lock", "trg_" + table + "_no_delete");
        });
    }
}
