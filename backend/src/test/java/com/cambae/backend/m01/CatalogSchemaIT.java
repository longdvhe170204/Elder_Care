package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.support.MySqlIntegrationTestBase;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 001 T006 (BR-M01-11, BR-M15-06; Q-291): danh mục của M01 nạp sẵn theo nguồn và hai mục Hiệu lực không trùng tên ở cơ
 * sở dữ liệu; mục ngừng hiệu lực thì tên dùng lại được.
 */
class CatalogSchemaIT extends MySqlIntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void BR_M01_11_seeds_severityAndSourceAndCareNeedFromSource() {
        assertThat(jdbc.queryForList("SELECT name FROM allergy_severity ORDER BY display_order", String.class))
                .startsWith("Nhẹ", "Trung bình", "Nặng");
        assertThat(jdbc.queryForList("SELECT name FROM information_source ORDER BY id", String.class))
                .startsWith("Người cao tuổi", "Người thân", "Hồ sơ y tế kèm theo", "Bác sĩ khám");
        assertThat(jdbc.queryForList("SELECT name FROM care_need ORDER BY id", String.class))
                .startsWith(
                        "Hỗ trợ vận động",
                        "Theo dõi sức khỏe thường xuyên",
                        "Phục hồi sau tai biến",
                        "Phục hồi chức năng khác");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM allergen WHERE name = ''", Long.class))
                .isZero();
    }

    @Test
    void BR_M15_06_twoActiveItemsMayNotShareName_inactiveNameCanBeReused() {
        String name = "Hải sản " + UUID.randomUUID();
        jdbc.update("INSERT INTO allergen (name, allergen_group) VALUES (?, 'FOOD')", name);

        assertThatThrownBy(() -> jdbc.update("INSERT INTO allergen (name, allergen_group) VALUES (?, 'OTHER')", name))
                .isInstanceOf(DuplicateKeyException.class);

        jdbc.update("UPDATE allergen SET status = 'INACTIVE' WHERE name = ?", name);
        jdbc.update("INSERT INTO allergen (name, allergen_group) VALUES (?, 'FOOD')", name);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM allergen WHERE name = ?", Long.class, name))
                .isEqualTo(2);
    }
}
