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
        // V900_15 nối ca ngày, ca đêm của mọi tầng tới hết tháng sau (V900_02, V900_09, V900_11 có 7 ngày đầu)
        int days = jdbc.queryForObject(
                "SELECT DATEDIFF(LAST_DAY(d + INTERVAL 1 MONTH), d) + 1 FROM (SELECT DATE(UTC_TIMESTAMP() + INTERVAL 7 HOUR) d) x",
                Integer.class);
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
                .isEqualTo(days);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM staff_assignment a JOIN work_shift s ON s.id=a.shift_id"
                                + " WHERE s.template_id=9001 AND a.target_type='RESIDENT' AND a.target_id=9001",
                        Integer.class))
                .isEqualTo(2 * days);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM staff_assignment WHERE kind='FLOOR_MANAGER' AND staff_id=9003"
                                + " AND floor_area_id=9001 AND status='ACTIVE'",
                        Integer.class))
                .isEqualTo(1);
        // Người phụ trách ca đọc từ cột work_shift.lead_staff_id (ShiftStore), không chỉ dòng lịch sử LEAD_STAFF
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM work_shift WHERE template_id=9001 AND scope_id=9001 AND lead_staff_id=9005",
                        Integer.class))
                .isEqualTo(days);
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
        // tầng Tầng 2 ở lần nạp này. V900_22 phân công lại Trưởng tầng.
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM work_shift s JOIN shift_schedule h ON h.id=s.schedule_id"
                                + " AND h.status='PUBLISHED' WHERE s.scope_type='FLOOR' AND s.scope_id=9002"
                                + " AND s.status='PUBLISHED' AND s.lead_staff_id=9011 AND s.template_id=9001",
                        Integer.class))
                .isEqualTo(days);
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
        // V900_22 (Q-453): mỗi tài khoản Trưởng tầng được phân công đúng một tầng để lập công việc tháng.
        assertThat(jdbc.queryForList(
                        "SELECT CONCAT(s.staff_id,':',s.floor_area_id) FROM staff_assignment s"
                                + " JOIN account a ON a.staff_id=s.staff_id AND a.status='ACTIVE'"
                                + " JOIN permission_assignment r ON r.account_id=a.id"
                                + " AND r.kind='ROLE_ASSIGNMENT' AND r.role='AC-02'"
                                + " WHERE s.kind='FLOOR_MANAGER' AND s.status='ACTIVE'"
                                + " AND s.staff_id IN (9003,9013,9023,9024,9025) ORDER BY s.staff_id",
                        String.class))
                .containsExactly("9003:9001", "9013:9002", "9023:9003", "9024:9004", "9025:9005");
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
        // V900_12: A, B đi được BF-10 (kết thúc lưu trú): phiên bản nội dung mới nhất của hợp đồng có kỳ tính phí lưu
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
        // V900_13: tham số "viện nhập khi cài đặt" có giá trị, có lịch sử đổi; bữa ăn là khóa giờ bữa; lý do chuyển
        // giường
        assertThat(jdbc.queryForList(
                        "SELECT code FROM parameter WHERE code IN ('CFG-M11-10','CFG-M11-11','CFG-M03-01','CFG-M08-04',"
                                + "'CFG-M09-01','CFG-M09-12') AND JSON_TYPE(current_value) <> 'NULL'"
                                + " AND EXISTS (SELECT 1 FROM status_history h WHERE h.entity_type='M15.PARAMETER'"
                                + " AND h.attribute=parameter.code) ORDER BY code",
                        String.class))
                .hasSize(6);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM catalog_item m JOIN parameter p ON p.code='CFG-M08-04'"
                                + " WHERE m.catalog_type='M08.MEAL' AND JSON_CONTAINS_PATH(p.current_value, 'one',"
                                + " CONCAT('$.mealTimes.', m.code))",
                        Integer.class))
                .isEqualTo(4);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM catalog_item WHERE catalog_type='M03.BED_TRANSFER_REASON' AND status='ACTIVE'"
                                + " AND attributes->>'$.medicalSafety'='true'",
                        Integer.class))
                .isGreaterThanOrEqualTo(2);
        // V900_14: Khu B, năm tầng, 17 phòng; mỗi phòng đủ giường chuẩn của loại phòng và có mức chăm sóc được phép
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM floor WHERE status='ACTIVE'", Integer.class))
                .isEqualTo(5);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM room r JOIN room_type t ON t.id=r.room_type_id WHERE JSON_LENGTH(r.allowed_care_levels)>0"
                                + " AND (SELECT COUNT(*) FROM bed b WHERE b.room_id=r.id)=t.standard_beds",
                        Integer.class))
                .isEqualTo(17);
        // V900_15: Phụ trách bộ phận đủ bốn bộ phận; mọi tầng có ca đã công bố tới hết tháng sau, mỗi ca có người phụ
        // trách
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM catalog_item WHERE catalog_type='M09.DEPARTMENT' AND status='ACTIVE'"
                                + " AND attributes->>'$.leadStaffId' IS NOT NULL AND attributes->>'$.leadStaffId'<>'null'",
                        Integer.class))
                .isEqualTo(4);
        assertThat(jdbc.queryForList(
                        "SELECT COUNT(*) FROM work_shift WHERE scope_type='FLOOR' AND status='PUBLISHED'"
                                + " AND lead_staff_id IS NOT NULL GROUP BY scope_id ORDER BY scope_id",
                        Integer.class))
                .containsOnly(2 * days)
                .hasSize(5);
        // V900_16: mốc sinh hoạt, nhóm sở thích; mọi giường đã ghi tăng tài sản; có xe cho Lịch xe
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM catalog_item WHERE catalog_type='M04.DAILY_ROUTINE'", Integer.class))
                .isGreaterThanOrEqualTo(8);
        // Lịch chăm sóc hằng ngày (005 FR-055) đã ghép giờ bữa của 011 và không hiện thuốc: mốc không trùng bữa, không
        // nói thuốc (góp ý kiểm demo 2026-11-25)
        assertThat(jdbc.queryForList(
                        "SELECT name FROM catalog_item WHERE catalog_type='M04.DAILY_ROUTINE'"
                                + " AND (name LIKE 'Ăn%' OR name LIKE '%thuốc%')",
                        String.class))
                .isEmpty();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed b WHERE NOT EXISTS (SELECT 1 FROM asset a WHERE a.bed_id=b.id)",
                        Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM asset a JOIN catalog_item t ON t.id=a.type_id AND t.code='VEHICLE'"
                                + " WHERE a.license_plate IS NOT NULL",
                        Integer.class))
                .isEqualTo(2);
        // V900_17 và V900_19–21: hai mươi người Đang lưu trú có giường, hợp đồng Hiệu lực, sổ số dư, người đại diện.
        assertThat(jdbc.queryForList(
                        "SELECT r.id FROM resident r WHERE r.status='IN_RESIDENCE'"
                                + " AND EXISTS (SELECT 1 FROM bed_allocation a WHERE a.resident_id=r.id AND a.status='ACTIVE')"
                                + " AND EXISTS (SELECT 1 FROM contract c WHERE c.resident_id=r.id AND c.status='ACTIVE')"
                                + " AND EXISTS (SELECT 1 FROM resident_balance b WHERE b.resident_id=r.id)"
                                + " AND EXISTS (SELECT 1 FROM family_relationship f WHERE f.resident_id=r.id"
                                + " AND f.representative)",
                        Long.class))
                .containsExactlyInAnyOrder(
                        9001L, 9002L, 9003L, 9004L, 9005L, 9006L, 9007L, 9008L, 9009L, 9010L, 9021L, 9022L, 9101L,
                        9102L, 9103L, 9104L, 9105L, 9106L, 9107L, 9108L);
        // V900_17: mười người có phân công Điều dưỡng phụ trách trong mọi ca từ ngày nạp.
        assertThat(jdbc.queryForList(
                        "SELECT COUNT(*) FROM staff_assignment a JOIN work_shift s ON s.id=a.shift_id"
                                + " WHERE a.target_type='RESIDENT' AND a.assigned_nurse"
                                + " AND s.starts_at >= TIMESTAMP(DATE(UTC_TIMESTAMP() + INTERVAL 7 HOUR))"
                                + " GROUP BY a.target_id",
                        Integer.class))
                .containsOnly(2 * days)
                .hasSize(10);
        // V900_17: dòng dịch vụ của hợp đồng có quantity là số dương (ContractStore đọc bằng decimalValue,
        // ContractRules
        // bắt > 0); quantity JSON null làm backend dừng khi khởi động (CareDailyJob đọc hợp đồng, 2026-11-25)
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM contract_version v, JSON_TABLE(IF(v.kind='DRAFT', v.services,"
                                + " v.content->'$.services'), '$[*]' COLUMNS (q JSON PATH '$.quantity')) s"
                                + " WHERE v.contract_id BETWEEN 9101 AND 9199 AND v.kind IN ('DRAFT','CONTENT')"
                                + " AND (s.q IS NULL OR JSON_TYPE(s.q) NOT IN ('INTEGER','DECIMAL','DOUBLE')"
                                + " OR CAST(s.q AS DECIMAL(15,3)) <= 0)",
                        Integer.class))
                .isZero();
        // V900_18: tài khoản người thân demo.nguoithan của Lê Văn Cường (người đại diện của 9101, đủ quyền xem sức
        // khỏe,
        // chi phí) để kiểm cổng người thân (VietQR nộp tiền 017, Lịch chăm sóc hằng ngày 011); vai trò duy nhất Người
        // thân
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM account a JOIN permission_assignment r ON r.account_id=a.id"
                                + " AND r.kind='ROLE_ASSIGNMENT' AND r.role='AC-10'"
                                + " JOIN status_history h ON h.entity_type='M15.ACCOUNT' AND h.entity_id=a.id"
                                + " AND h.value='ACTIVE' WHERE a.username='demo.nguoithan' AND a.type='FAMILY_MEMBER'"
                                + " AND a.family_member_id=9101 AND a.status='ACTIVE' AND a.staff_id IS NULL"
                                + " AND (SELECT COUNT(*) FROM permission_assignment x WHERE x.account_id=a.id"
                                + " AND x.kind='ROLE_ASSIGNMENT')=1",
                        Integer.class))
                .isEqualTo(1);
    }
}
