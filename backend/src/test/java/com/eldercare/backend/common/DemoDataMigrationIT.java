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
        // V900_08: Bác sĩ thứ hai (demo.bacsi2, hồ sơ 9010, giấy phép DOCTOR_LICENSE) để thử quy tắc hai người của
        // phiếu
        // đối chiếu thuốc trước Hoàn tất tiếp nhận (BF-01 bước 12, Q-55; Điều dưỡng chỉ thấy người trong phân công)
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM account a JOIN permission_assignment r ON r.account_id=a.id AND r.kind='ROLE_ASSIGNMENT' AND r.role='AC-03'"
                                + " JOIN staff_profile s ON s.id=a.staff_id AND s.account_id=a.id"
                                + " JOIN credential c ON c.staff_id=s.id JOIN catalog_item t ON t.id=c.credential_type_id"
                                + " AND t.code='DOCTOR_LICENSE' WHERE a.username='demo.bacsi2' AND a.status='ACTIVE'"
                                + " AND a.staff_id=9010",
                        Integer.class))
                .isEqualTo(1);
        // V900_09: ca ngày Tầng 2 (9002) đã công bố 7 ngày với Điều dưỡng thứ hai (demo.dieuduong2, 9011, phụ trách ca,
        // giấy phép NURSING_LICENSE) và Nhân viên chăm sóc thứ hai (demo.chamsoc2, 9012); demo.truongtang là Trưởng
        // tầng
        // cả Tầng 2. Người ở Tầng 2 phân công được Điều dưỡng để lập kế hoạch chăm sóc (BF-01 bước 16)
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM work_shift s JOIN shift_schedule h ON h.id=s.schedule_id"
                                + " AND h.status='PUBLISHED' WHERE s.scope_type='FLOOR' AND s.scope_id=9002"
                                + " AND s.status='PUBLISHED' AND s.lead_staff_id=9011",
                        Integer.class))
                .isEqualTo(7);
        assertThat(jdbc.queryForList(
                        "SELECT DISTINCT m.staff_id FROM shift_staff m JOIN work_shift s ON s.id=m.shift_id"
                                + " WHERE s.scope_type='FLOOR' AND s.scope_id=9002 ORDER BY m.staff_id",
                        Long.class))
                .containsExactly(9011L, 9012L);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM account a JOIN permission_assignment r ON r.account_id=a.id"
                                + " AND r.kind='ROLE_ASSIGNMENT' JOIN staff_profile s ON s.id=a.staff_id"
                                + " AND s.account_id=a.id WHERE a.status='ACTIVE' AND ((a.username='demo.dieuduong2'"
                                + " AND a.staff_id=9011 AND r.role='AC-04') OR (a.username='demo.chamsoc2'"
                                + " AND a.staff_id=9012 AND r.role='AC-05'))",
                        Integer.class))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM credential c JOIN catalog_item t ON t.id=c.credential_type_id"
                                + " AND t.code='NURSING_LICENSE' WHERE c.staff_id=9011",
                        Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM staff_assignment WHERE kind='FLOOR_MANAGER' AND staff_id=9003"
                                + " AND floor_area_id=9002 AND status='ACTIVE'",
                        Integer.class))
                .isEqualTo(1);
        // V900_10: nghiệp vụ Phát thuốc có loại giấy phép NURSING_LICENSE để Trưởng tầng đặt được Điều dưỡng phụ trách
        // (008 AssignmentService, BF-01 bước 16; F17); Điều dưỡng demo 9005, 9011 có giấy phép đó
        assertThat(jdbc.queryForObject(
                        "SELECT attributes->>'$.requiredLicenseType' FROM catalog_item"
                                + " WHERE catalog_type='M15.PROFESSIONAL_ACTIVITY' AND code='MEDICATION_ADMINISTRATION'",
                        String.class))
                .isEqualTo("NURSING_LICENSE");
        assertThat(jdbc.queryForList(
                        "SELECT c.staff_id FROM credential c JOIN catalog_item t ON t.id=c.credential_type_id"
                                + " AND t.code='NURSING_LICENSE' WHERE c.staff_id IN (9005, 9011) ORDER BY c.staff_id",
                        Long.class))
                .containsExactly(9005L, 9011L);
        // V900_11: A, B đi được BF-10 (kết thúc lưu trú): phiên bản nội dung mới nhất của hợp đồng có kỳ tính phí lưu
        // trú
        // (010 chia phí theo ngày, trước báo 500) và không cần cọc; có sổ số dư với mã nộp tiền đúng ký tự kiểm tra
        // (017)
        assertThat(jdbc.queryForList(
                        "SELECT v.content->>'$.residencyFeeBillingPeriod' FROM contract_version v"
                                + " WHERE v.kind='CONTENT' AND v.contract_id IN (9001,9002) AND v.sequence_no ="
                                + " (SELECT MAX(x.sequence_no) FROM contract_version x"
                                + " WHERE x.kind='CONTENT' AND x.contract_id=v.contract_id)",
                        String.class))
                .containsOnly("MONTH")
                .hasSize(2);
        assertThat(jdbc.queryForList(
                        "SELECT payment_code FROM resident_balance WHERE resident_id IN (9001,9002) ORDER BY resident_id",
                        String.class))
                .containsExactly("NCTDEM9012", "NCTDEM9023");
    }
}
