package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.support.MySqlIntegrationTestBase;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 004 T071 (data-model mục 15; FR-010 của 000, BR-M15-03): mỗi bảng nhóm 3 (bản ghi chỉ thêm) của 004 có trigger chặn sửa và
 * chặn xóa. 004 ghi bằng JDBC nên {@code RecordGroupTriggerIT} của 000 (đi theo entity) không phủ các bảng này; danh sách
 * bảng lấy từ các mục "nhóm 3" của data-model.
 */
class M02RecordGroupTriggerIT extends MySqlIntegrationTestBase {

    private static final List<String> RECORD_TABLES = List.of(
            "score_history",
            "bed_contact",
            "contract_content_version",
            "expected_return_history",
            "bed_hold_decision",
            "day_care_absence_notice",
            "resident_handover",
            "ending_condition_exception");

    @Test
    void FR_010_everyGroup3Table_hasUpdateAndDeleteTriggers() throws SQLException {
        Set<String> guarded = new HashSet<>();
        try (Connection c = adminConnection();
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery("SELECT LOWER(EVENT_OBJECT_TABLE), EVENT_MANIPULATION"
                        + " FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA = DATABASE()")) {
            while (rs.next()) {
                guarded.add(rs.getString(1) + ":" + rs.getString(2));
            }
        }

        assertThat(RECORD_TABLES)
                .allSatisfy(table -> assertThat(guarded).contains(table + ":UPDATE", table + ":DELETE"));
    }
}
