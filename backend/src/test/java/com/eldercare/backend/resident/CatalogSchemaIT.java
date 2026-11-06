package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
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
        assertThat(jdbc.queryForList(
                        "SELECT name FROM catalog_item WHERE catalog_type = 'M01.ALLERGY_SEVERITY' ORDER BY sort_order",
                        String.class))
                .startsWith("Nhẹ", "Trung bình", "Nặng");
        assertThat(jdbc.queryForList(
                        "SELECT name FROM catalog_item WHERE catalog_type = 'M01.INFORMATION_SOURCE' ORDER BY id",
                        String.class))
                .startsWith("Người cao tuổi", "Người thân", "Hồ sơ y tế kèm theo", "Bác sĩ khám");
        assertThat(jdbc.queryForList(
                        "SELECT name FROM catalog_item WHERE catalog_type = 'M01.CARE_NEED' ORDER BY id", String.class))
                .startsWith(
                        "Hỗ trợ vận động",
                        "Theo dõi sức khỏe thường xuyên",
                        "Phục hồi sau tai biến",
                        "Phục hồi chức năng khác");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM catalog_item WHERE catalog_type = 'M01.ALLERGEN' AND name = ''",
                        Long.class))
                .isZero();
    }

    @Test
    void BR_M15_06_twoActiveItemsMayNotShareName_inactiveNameCanBeReused() {
        String name = "Hải sản " + UUID.randomUUID();
        // Dị nguyên ở catalog_item loại M01.ALLERGEN từ 001 T084: active_name thay UNIQUE (active_name) của bảng cũ
        String food = "INSERT INTO catalog_item (catalog_type, module, name, sort_order, status, attributes)"
                + " VALUES ('M01.ALLERGEN', 'M01', ?, 0, 'ACTIVE', JSON_OBJECT('allergenGroup', 'FOOD'))";
        jdbc.update(food, name);

        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO catalog_item (catalog_type, module, name, sort_order, status, attributes)"
                                + " VALUES ('M01.ALLERGEN', 'M01', ?, 0, 'ACTIVE', JSON_OBJECT('allergenGroup', 'OTHER'))",
                        name))
                .isInstanceOf(DuplicateKeyException.class);

        jdbc.update(
                "UPDATE catalog_item SET status = 'INACTIVE' WHERE catalog_type = 'M01.ALLERGEN' AND name = ?", name);
        jdbc.update(food, name);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM catalog_item WHERE catalog_type = 'M01.ALLERGEN' AND name = ?",
                        Long.class,
                        name))
                .isEqualTo(2);
    }
}
