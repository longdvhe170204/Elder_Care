package com.eldercare.backend.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Dữ liệu demo (quickstart 002 mục 1; {@code db/demo/V900_01}, {@code V900_02}, chủ dự án chọn 2026-11-17): migration của
 * profile demo chạy được trên schema mới cùng mọi migration chính; A (9001), B (9002) Đang lưu trú có phân bổ giường Đang
 * hiệu lực ở tầng 1, tầng 2; ca ngày tầng 1 có Người phụ trách ca, phân công A; Trưởng tầng giao tầng 1.
 */
class DemoDataMigrationIT extends MySqlIntegrationTestBase {

    private static final String SUFFIX = "demo";

    @AfterAll
    static void drop() {
        dropScratchSchema(SUFFIX);
    }

    @Test
    void demoMigrations_applyOnTopOfAllMigrations_andSeedQuickstartData() {
        Flyway base = scratchFlyway(SUFFIX);
        Flyway demo = Flyway.configure()
                .configuration(base.getConfiguration())
                .locations("classpath:db/migration", "classpath:db/demo")
                .load();
        demo.migrate();

        var config = base.getConfiguration();
        var jdbc =
                new JdbcTemplate(new DriverManagerDataSource(config.getUrl(), config.getUser(), config.getPassword()));
        assertThat(jdbc.queryForList("SELECT status FROM resident WHERE id IN (9001,9002)", String.class))
                .containsOnly("IN_RESIDENCE")
                .hasSize(2);
        assertThat(jdbc.queryForList(
                        "SELECT r.floor_id FROM bed_allocation a JOIN bed b ON b.id=a.bed_id JOIN room r ON"
                                + " r.id=b.room_id WHERE a.status='ACTIVE' AND a.resident_id IN (9001,9002)"
                                + " ORDER BY a.resident_id",
                        Long.class))
                .containsExactly(9001L, 9002L);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM work_shift WHERE template_id=9001 AND scope_id=9001", Integer.class))
                .isEqualTo(7);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM staff_assignment a JOIN work_shift s ON s.id=a.shift_id"
                                + " WHERE s.template_id=9001 AND a.target_type='RESIDENT' AND a.target_id=9001",
                        Integer.class))
                .isEqualTo(14);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM staff_assignment WHERE kind='FLOOR_MANAGER' AND staff_id=9003"
                                + " AND floor_area_id=9001 AND status='ACTIVE'",
                        Integer.class))
                .isEqualTo(1);
    }
}
