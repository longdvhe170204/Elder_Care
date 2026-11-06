package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** T004: chứng minh bất biến tại MySQL thật, dùng quyền ứng dụng. */
@Transactional
class StaffSchemaIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    private long staff() {
        jdbc.update("INSERT INTO staff_profile(full_name, position) VALUES ('[Nhân viên kiểm thử]', 'CAREGIVER')");
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Test
    void FR_001_staffCannotBeDeletedOrChangeStatusWithoutCommand() {
        long id = staff();
        assertThatThrownBy(() -> jdbc.update("DELETE FROM staff_profile WHERE id=?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE staff_profile SET status='RESIGNED' WHERE id=?", id))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_005_requiredDefaultCatalogsExist() {
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM (SELECT COALESCE(CAST(NULLIF(JSON_UNQUOTE(c.attributes->'$.legacy.id'),'null') AS UNSIGNED),c.id) AS id,c.name AS name,c.status AS status,c.version AS version,CAST(REPLACE(REPLACE(NULLIF(JSON_UNQUOTE(c.attributes->'$.createdAt'),'null'),'T',' '),'Z','') AS DATETIME(6)) AS created_at,CAST(NULLIF(JSON_UNQUOTE(c.attributes->'$.leadStaffId'),'null') AS UNSIGNED) AS lead_staff_id FROM catalog_item c WHERE c.catalog_type='M09.DEPARTMENT') department WHERE id IN (1,2,3,4)",
                        Integer.class))
                .isEqualTo(4);
        assertThat(jdbc.queryForList(
                        "SELECT code FROM (SELECT COALESCE(CAST(NULLIF(JSON_UNQUOTE(c.attributes->'$.legacy.id'),'null') AS UNSIGNED),c.id) AS id,c.name AS name,c.status AS status,c.version AS version,CAST(REPLACE(REPLACE(NULLIF(JSON_UNQUOTE(c.attributes->'$.createdAt'),'null'),'T',' '),'Z','') AS DATETIME(6)) AS created_at,COALESCE(NULLIF(JSON_UNQUOTE(c.attributes->'$.legacyCode'),'null'),c.code) AS code,NULLIF(JSON_UNQUOTE(c.attributes->'$.credentialGroup'),'null') AS credential_group,(c.attributes->'$.hasExpiry'=CAST('true' AS JSON)) AS has_expiry,c.attributes->'$.requiredForRoles' AS required_for_roles,(c.attributes->'$.hasPracticeScopes'=CAST('true' AS JSON)) AS has_practice_scopes FROM catalog_item c WHERE c.catalog_type='M09.CREDENTIAL_TYPE') credential_type",
                        String.class))
                .contains("DOCTOR_LICENSE", "NURSING_LICENSE", "FIRST_AID_TRAINING");
    }

    @Test
    void DBR_36_staffCannotHaveTwoOpenDepartments() {
        long id = staff();
        jdbc.update(
                "INSERT INTO staff_department(staff_id,department_id,starts_at,actor_id,reason) SELECT src.staff_id,(SELECT c.id FROM catalog_item c WHERE c.catalog_type='M09.DEPARTMENT' AND COALESCE(c.m09_legacy_id,c.id)=src.department_id),src.starts_at,src.actor_id,src.reason FROM (SELECT ? AS staff_id,1 AS department_id,'2026-10-23' AS starts_at,1 AS actor_id,'Kiểm thử' AS reason) src",
                id);
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO staff_department(staff_id,department_id,starts_at,actor_id,reason) SELECT src.staff_id,(SELECT c.id FROM catalog_item c WHERE c.catalog_type='M09.DEPARTMENT' AND COALESCE(c.m09_legacy_id,c.id)=src.department_id),src.starts_at,src.actor_id,src.reason FROM (SELECT ? AS staff_id,2 AS department_id,'2026-10-23' AS starts_at,1 AS actor_id,'Kiểm thử' AS reason) src",
                        id))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_001a_departmentLeadHistoryCanOnlyBeClosedOnce() {
        long id = staff();
        jdbc.update(
                "INSERT INTO status_history(entity_type,entity_id,attribute,module,kind,value_id,starts_at,actor_id,reason,command,by_system) SELECT 'M09.DEPARTMENT',src.department_id,'LEAD_STAFF','M09','PERIOD',src.staff_id,src.starts_at,src.actor_id,src.reason,'LEAD_CHANGED',FALSE FROM (SELECT 1 AS department_id,? AS staff_id,'2026-10-23' AS starts_at,1 AS actor_id,'Kiểm thử' AS reason) src",
                id);
        long history = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update(
                "UPDATE status_history h SET ends_at='2026-10-24' WHERE h.entity_type='M09.DEPARTMENT' AND h.attribute='LEAD_STAFF' AND h.kind='PERIOD' AND (COALESCE(CAST(NULLIF(JSON_UNQUOTE(h.details->'$.legacy.id'),'null') AS UNSIGNED),h.id)=?)",
                history);
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE status_history h SET ends_at='2026-10-25' WHERE h.entity_type='M09.DEPARTMENT' AND h.attribute='LEAD_STAFF' AND h.kind='PERIOD' AND (COALESCE(CAST(NULLIF(JSON_UNQUOTE(h.details->'$.legacy.id'),'null') AS UNSIGNED),h.id)=?)",
                        history))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE status_history h SET reason='Ghi đè' WHERE h.entity_type='M09.DEPARTMENT' AND h.attribute='LEAD_STAFF' AND h.kind='PERIOD' AND (COALESCE(CAST(NULLIF(JSON_UNQUOTE(h.details->'$.legacy.id'),'null') AS UNSIGNED),h.id)=?)",
                        history))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_006_credentialsCannotBeDeletedOrSilentlyEdited() {
        long id = staff();
        jdbc.update(
                "INSERT INTO credential(staff_id,credential_type_id,issue_date,expiry_date) SELECT src.staff_id,(SELECT c.id FROM catalog_item c WHERE c.catalog_type='M09.CREDENTIAL_TYPE' AND COALESCE(c.m09_legacy_id,c.id)=src.credential_type_id),src.issue_date,src.expiry_date FROM (SELECT ? AS staff_id,1 AS credential_type_id,'2026-01-01' AS issue_date,'2026-12-31' AS expiry_date) src",
                id);
        long credential = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM credential WHERE id=?", credential))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE credential SET issue_date='2025-01-01' WHERE id=?", credential))
                .isInstanceOf(DataAccessException.class);
        jdbc.update("UPDATE credential SET expiry_warning_at='2026-10-23' WHERE id=?", credential);
    }
}
