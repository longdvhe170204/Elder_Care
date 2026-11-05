package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.staffing.internal.credential.persistence.CredentialStore;
import com.eldercare.backend.staffing.internal.duty.persistence.DepartmentDutyStore;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import com.eldercare.backend.staffing.internal.staff.persistence.StaffStore;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** T050: Store đọc/ghi id logic; FK thật dùng id vật lý, không sửa tham chiếu chuyên môn của A. */
@Transactional
class StaffingCatalogIdentityIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    CredentialStore credentials;

    @Autowired
    DepartmentDutyStore departments;

    @Autowired
    ShiftStore shifts;

    @Autowired
    StaffStore staff;

    private long department;
    private long type;
    private long scope;
    private long template;
    private long staffId;
    private long credentialId;
    private static final long LOGICAL = 7001;

    @BeforeEach
    void copiedRecordsWithSameLogicalId() {
        long used = jdbc.queryForObject(
                "SELECT GREATEST((SELECT COALESCE(MAX(id),0) FROM staff_profile),(SELECT COALESCE(MAX(id),0) FROM credential),(SELECT COALESCE(MAX(id),0) FROM catalog_item))",
                Long.class);
        while (TestUsers.nextId() <= used) {
            /* Fixture import trước đó có thể đã đưa AUTO_INCREMENT vượt bộ đếm test. */
        }
        department = TestUsers.nextId();
        type = TestUsers.nextId();
        scope = TestUsers.nextId();
        template = TestUsers.nextId();
        staffId = TestUsers.nextId();
        credentialId = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO catalog_item(id,catalog_type,module,name,status,attributes) VALUES (?,'M09.DEPARTMENT','M09',?,'ACTIVE',JSON_OBJECT('leadStaffId',NULL,'createdAt','2026-01-01T00:00:00Z','legacy',JSON_OBJECT('table','department','id',7001)))",
                department,
                "[Bộ phận " + department + "]");
        jdbc.update(
                "INSERT INTO catalog_item(id,catalog_type,module,code,name,status,attributes) VALUES (?,'M09.CREDENTIAL_TYPE','M09',?,?,'ACTIVE',JSON_OBJECT('credentialGroup','PROFESSIONAL_LICENSE','hasExpiry',true,'requiredForRoles',JSON_ARRAY('NURSING_STAFF'),'hasPracticeScopes',true,'legacyCode','LEGACY_LICENSE','createdAt','2026-01-01T00:00:00Z','legacy',JSON_OBJECT('table','credential_type','id',7001)))",
                type,
                "CT-" + type,
                "[Chứng chỉ " + type + "]");
        jdbc.update(
                "INSERT INTO catalog_item(id,catalog_type,module,code,name,status,attributes) VALUES (?,'M09.PRACTICE_SCOPE','M09',?,?,'ACTIVE',JSON_OBJECT('legacyCode','LEGACY_SCOPE','createdAt','2026-01-01T00:00:00Z','legacy',JSON_OBJECT('table','practice_scope','id',7001)))",
                scope,
                "PS-" + scope,
                "[Phạm vi " + scope + "]");
        jdbc.update(
                "INSERT INTO catalog_item(id,catalog_type,module,name,status,attributes) VALUES (?,'M09.SHIFT_TEMPLATE','M09',?,'ACTIVE',JSON_OBJECT('startTime','06:00:00','endTime','14:00:00','scopeType','FLOOR','requiresHandover',true,'originalName','[Tên mẫu cũ]','createdAt','2026-01-01T00:00:00Z','legacy',JSON_OBJECT('table','shift_template','id',7001)))",
                template,
                "[Mẫu " + template + "]");
        jdbc.update(
                "INSERT INTO staff_profile(id,full_name,position) VALUES (?,'[Nhân viên identity]','Điều dưỡng')",
                staffId);
        jdbc.update(
                "INSERT INTO staff_department(staff_id,department_id,starts_at,actor_id,reason) VALUES (?,?,'2026-01-01',?,'Lập hồ sơ')",
                staffId,
                department,
                staffId);
        jdbc.update(
                "INSERT INTO credential(id,staff_id,credential_type_id,issue_date,practice_scope_ids) VALUES (?,?,?,'2026-01-01',JSON_ARRAY(?))",
                credentialId,
                staffId,
                type,
                scope);
    }

    @Test
    void FR_005_keepsLicenseAndScopeLogicalIdsWhenPhysicalIdsDiffer() {
        var record = credentials.find(credentialId).orElseThrow();
        assertThat(record.credentialTypeId()).isEqualTo(LOGICAL);
        assertThat(record.typeCode()).isEqualTo("LEGACY_LICENSE");
        assertThat(record.practiceScopeIds()).containsExactly(LOGICAL);
        assertThat(record.practiceScopes()).containsExactly("LEGACY_SCOPE");
        assertThat(credentials.practiceScope(LOGICAL).orElseThrow().id()).isEqualTo(LOGICAL);
    }

    @Test
    void FR_001a_mapsDepartmentForeignKeyBackToOriginalLogicalId() {
        assertThat(staff.find(staffId).orElseThrow().departmentId()).isEqualTo(LOGICAL);
        assertThat(departments.lock(LOGICAL).id()).isEqualTo(LOGICAL);
        departments.replace(LOGICAL, staffId, Instant.parse("2026-02-01T00:00:00Z"), staffId, "Giao phụ trách");
        assertThat(departments.lead(LOGICAL)).contains(staffId);
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_UNQUOTE(attributes->'$.leadStaffId') FROM catalog_item WHERE id=?",
                        String.class,
                        department))
                .isEqualTo(Long.toString(staffId));
    }

    @Test
    void FR_011_usesOriginalTemplateIdAndName() {
        var found = shifts.template(LOGICAL, false);
        assertThat(found.id()).isEqualTo(LOGICAL);
        assertThat(found.name()).isEqualTo("[Tên mẫu cũ]");
        assertThat(found.handoverRequired()).isTrue();
    }

    @Test
    void R_15_newCatalogIdCannotShadowLegacyIdOrAnotherKind() {
        long added = credentials.addType(
                "[Loại mới " + type + "]",
                "TRAINING",
                false,
                Set.of(),
                false,
                true,
                Instant.parse("2026-02-01T00:00:00Z"));
        assertThat(added).isGreaterThan(LOGICAL);
        assertThat(credentials.type(LOGICAL).orElseThrow().code()).isEqualTo("LEGACY_LICENSE");
        assertThat(credentials.practiceScope(department)).isEmpty();
    }
}
