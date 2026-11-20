package com.eldercare.backend.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.time.Instant;
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
        // Người phụ trách ca đọc từ cột work_shift.lead_staff_id (ShiftStore), không chỉ dòng lịch sử LEAD_STAFF
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM work_shift WHERE template_id=9001 AND scope_id=9001 AND lead_staff_id=9005",
                        Integer.class))
                .isEqualTo(7);
        // Mốc thời gian trong JSON của bảng chung được đọc thành Instant (ISO-8601 UTC); dạng DATETIME của MySQL làm
        // backend dừng khi khởi động (job phút của 005 đọc mẫu ca, 2026-11-17).
        assertThat(Instant.parse(jdbc.queryForObject(
                        "SELECT attributes->>'$.createdAt' FROM catalog_item WHERE id=9001 AND catalog_type='M09.SHIFT_TEMPLATE'",
                        String.class)))
                .isNotNull();
        // V900_04: Bác sĩ demo kê đơn nội bộ được qua lớp pháp lý (006 FR-005): loại giấy phép của nghiệp vụ kê đơn đã
        // cấu hình (JSON null ở V002_95 không phải SQL NULL), giấy phép cơ sở có phạm vi đó.
        assertThat(jdbc.queryForObject(
                        "SELECT attributes->>'$.requiredLicenseType' FROM catalog_item"
                                + " WHERE catalog_type='M15.PROFESSIONAL_ACTIVITY' AND code='INTERNAL_DIAGNOSIS_PRESCRIBING'",
                        String.class))
                .isEqualTo("DOCTOR_LICENSE");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM catalog_item WHERE catalog_type='M06.FACILITY_LICENSE'"
                                + " AND JSON_CONTAINS(attributes->'$.operationScopes', '\"INTERNAL_DIAGNOSIS_PRESCRIBING\"')",
                        Integer.class))
                .isEqualTo(1);
        // V900_06: dịch vụ chăm sóc demo 9302 "Chăm sóc thường xuyên" mang mã mức chăm sóc REGULAR (001 CareLevel),
        // không
        // phải FREQUENT V900_05 ghi nhầm, để hợp đồng mức Thường xuyên chọn được dịch vụ chăm sóc tương ứng
        assertThat(jdbc.queryForObject(
                        "SELECT attributes->>'$.careLevelCode' FROM catalog_item WHERE id=9302 AND catalog_type='M02.SERVICE'",
                        String.class))
                .isEqualTo("REGULAR");
        // V900_07: phòng demo có danh sách mức chăm sóc được phép khởi tạo từ loại phòng (003 FR-004); V900_01 để trống
        // nên mọi giường trượt FR-018(a) và không phân bổ được giường cho người mới (BF-01 bước 8)
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM room r JOIN room_type t ON t.id=r.room_type_id WHERE r.id IN"
                                + " (9001,9002,9003,9004) AND JSON_LENGTH(r.allowed_care_levels)>0"
                                + " AND JSON_CONTAINS(r.allowed_care_levels, t.allowed_care_levels)"
                                + " AND JSON_CONTAINS(t.allowed_care_levels, r.allowed_care_levels)",
                        Integer.class))
                .isEqualTo(4);
    }
}
