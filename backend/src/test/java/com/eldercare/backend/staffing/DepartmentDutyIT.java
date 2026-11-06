package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.staffing.internal.duty.service.DepartmentDutyService;
import com.eldercare.backend.support.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** T021, Q-292/Q-293/Q-299: cùng transaction lệnh gọi; người thay hợp lệ, giữ nguyên lịch sử khi rollback. */
@Import(DepartmentDutyIT.Users.class)
class DepartmentDutyIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    DepartmentDutyService service;

    @Autowired
    SwitchableUser current;

    @Autowired
    PlatformTransactionManager transactions;

    private static final AtomicInteger sequence = new AtomicInteger();
    private long department, old;
    private long fixtureDepartment;

    @BeforeEach
    void setup() {
        current.set(new TestUsers(jdbc, time).staff(Role.NURSING_HOME_MANAGER).user());
        old = staff(Role.NURSING_STAFF);
        department = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) SELECT 'M09.DEPARTMENT','M09',src.name,'ACTIVE',JSON_OBJECT('createdAt',DATE_FORMAT(UTC_TIMESTAMP(6),'%Y-%m-%dT%H:%i:%s.%fZ'),'leadStaffId',src.lead_staff_id) FROM (SELECT ? AS name,? AS lead_staff_id) src",
                "[Bộ phận kiểm thử " + sequence.incrementAndGet() + "]",
                old);
        fixtureDepartment = department;
        join(old, department);
        jdbc.update(
                "INSERT INTO status_history(entity_type,entity_id,attribute,module,kind,value_id,starts_at,actor_id,reason,command,by_system) SELECT 'M09.DEPARTMENT',src.department_id,'LEAD_STAFF','M09','PERIOD',src.staff_id,src.starts_at,src.actor_id,src.reason,'LEAD_CHANGED',FALSE FROM (SELECT ? AS department_id,? AS staff_id,UTC_TIMESTAMP(6)-INTERVAL 1 DAY AS starts_at,? AS actor_id,'[Khởi tạo test]' AS reason) src",
                department,
                old,
                current.accountId());
    }

    @AfterEach
    void removeOwnDepartmentFixture() throws java.sql.SQLException {
        // Chỉ dọn bộ phận do test này tạo, qua đúng tài khoản lưu giữ; không chạm danh mục khởi tạo.
        try (var connection = retentionConnection()) {
            connection.setAutoCommit(false);
            for (String sql : java.util.List.of(
                    "DELETE FROM status_history WHERE entity_type='M09.DEPARTMENT' AND attribute='LEAD_STAFF' AND entity_id=?",
                    "DELETE FROM staff_department WHERE department_id=?",
                    "DELETE FROM catalog_item WHERE catalog_type='M09.DEPARTMENT' AND id=?")) {
                try (var statement = connection.prepareStatement(sql)) {
                    statement.setLong(1, fixtureDepartment);
                    statement.executeUpdate();
                }
            }
            connection.commit();
        }
    }

    private long staff(Role role) {
        return staff(role, "WORKING");
    }

    private long staff(Role role, String status) {
        var user = new TestUsers(jdbc, time).staff(role);
        jdbc.update(
                "INSERT INTO staff_profile(id,full_name,position,account_id,status) VALUES (?, '[Nhân viên thay phụ trách]', ?, ?,?)",
                user.staffId(),
                role.name(),
                user.accountId(),
                status);
        return user.staffId();
    }

    private void join(long staff, long department) {
        jdbc.update(
                "INSERT INTO staff_department(staff_id,department_id,starts_at,actor_id,reason) SELECT src.staff_id,(SELECT c.id FROM catalog_item c WHERE c.catalog_type='M09.DEPARTMENT' AND COALESCE(c.m09_legacy_id,c.id)=src.department_id),src.starts_at,src.actor_id,src.reason FROM (SELECT ? AS staff_id,? AS department_id,UTC_TIMESTAMP(6)-INTERVAL 1 DAY AS starts_at,? AS actor_id,'[Kiểm thử]' AS reason) src",
                staff,
                department,
                current.accountId());
    }

    private void replace(long replacement) {
        new TransactionTemplate(transactions)
                .executeWithoutResult(
                        s -> service.replaceLead(old, replacement, new Reason("Thay khi khóa tài khoản")));
    }

    @Test
    void Q_292_replacementRequiresCallerTransaction() {
        assertThatThrownBy(() -> service.replaceLead(old, old, new Reason("Thay")))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void Q_292_replacementClosesOldHistoryAndOpensNewHistoryAtomically() {
        long next = staff(Role.NURSING_STAFF);
        join(next, department);
        replace(next);
        assertThat(jdbc.queryForObject(
                        "SELECT lead_staff_id FROM (SELECT COALESCE(CAST(NULLIF(JSON_UNQUOTE(c.attributes->'$.legacy.id'),'null') AS UNSIGNED),c.id) AS id,c.name AS name,c.status AS status,c.version AS version,CAST(REPLACE(REPLACE(NULLIF(JSON_UNQUOTE(c.attributes->'$.createdAt'),'null'),'T',' '),'Z','') AS DATETIME(6)) AS created_at,CAST(NULLIF(JSON_UNQUOTE(c.attributes->'$.leadStaffId'),'null') AS UNSIGNED) AS lead_staff_id FROM catalog_item c WHERE c.catalog_type='M09.DEPARTMENT') department WHERE id=?",
                        Long.class,
                        department))
                .isEqualTo(next);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM (SELECT COALESCE(CAST(NULLIF(JSON_UNQUOTE(h.details->'$.legacy.id'),'null') AS UNSIGNED),h.id) AS id,h.entity_id AS department_id,h.value_id AS staff_id,h.starts_at AS starts_at,h.ends_at AS ends_at,h.actor_id AS actor_id,h.reason AS reason FROM status_history h WHERE h.entity_type='M09.DEPARTMENT' AND h.attribute='LEAD_STAFF' AND h.kind='PERIOD') department_lead_history WHERE department_id=? AND ends_at IS NULL AND staff_id=?",
                        Integer.class,
                        department,
                        next))
                .isOne();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM (SELECT COALESCE(CAST(NULLIF(JSON_UNQUOTE(h.details->'$.legacy.id'),'null') AS UNSIGNED),h.id) AS id,h.entity_id AS department_id,h.value_id AS staff_id,h.starts_at AS starts_at,h.ends_at AS ends_at,h.actor_id AS actor_id,h.reason AS reason FROM status_history h WHERE h.entity_type='M09.DEPARTMENT' AND h.attribute='LEAD_STAFF' AND h.kind='PERIOD') department_lead_history WHERE department_id=? AND ends_at IS NOT NULL AND staff_id=?",
                        Integer.class,
                        department,
                        old))
                .isOne();
    }

    @Test
    void Q_292_replacementInAnotherDepartmentIsRejectedWithoutChangingCurrentLead() {
        long next = staff(Role.NURSING_STAFF);
        assertThatThrownBy(() -> replace(next)).isInstanceOf(BusinessException.class);
        assertThat(jdbc.queryForObject(
                        "SELECT lead_staff_id FROM (SELECT COALESCE(CAST(NULLIF(JSON_UNQUOTE(c.attributes->'$.legacy.id'),'null') AS UNSIGNED),c.id) AS id,c.name AS name,c.status AS status,c.version AS version,CAST(REPLACE(REPLACE(NULLIF(JSON_UNQUOTE(c.attributes->'$.createdAt'),'null'),'T',' '),'Z','') AS DATETIME(6)) AS created_at,CAST(NULLIF(JSON_UNQUOTE(c.attributes->'$.leadStaffId'),'null') AS UNSIGNED) AS lead_staff_id FROM catalog_item c WHERE c.catalog_type='M09.DEPARTMENT') department WHERE id=?",
                        Long.class,
                        department))
                .isEqualTo(old);
    }

    @Test
    void Q_293_Q_299_managerAndAccountantCannotBecomeDepartmentLead() {
        for (var role : java.util.List.of(Role.NURSING_HOME_MANAGER, Role.ACCOUNTANT)) {
            long next = staff(role);
            join(next, department);
            assertThatThrownBy(() -> replace(next)).isInstanceOf(BusinessException.class);
        }
    }

    @Test
    void Q_292_lockedReplacementIsRejected() {
        long next = staff(Role.NURSING_STAFF);
        join(next, department);
        jdbc.update("UPDATE account SET status='LOCKED',lock_cause='MANUAL' WHERE staff_id=?", next);
        assertThatThrownBy(() -> replace(next)).isInstanceOf(BusinessException.class);
    }

    @Test
    void Q_293_administrationDepartmentRequiresOfficeStaff() {
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            jdbc.update(
                    "UPDATE catalog_item c SET attributes=JSON_SET(attributes,'$.leadStaffId',NULL) WHERE c.catalog_type='M09.DEPARTMENT' AND (COALESCE(CAST(NULLIF(JSON_UNQUOTE(c.attributes->'$.legacy.id'),'null') AS UNSIGNED),c.id)=?)",
                    department);
            department = jdbc.queryForObject(
                    "SELECT id FROM (SELECT COALESCE(CAST(NULLIF(JSON_UNQUOTE(c.attributes->'$.legacy.id'),'null') AS UNSIGNED),c.id) AS id,c.name AS name,c.status AS status,c.version AS version,CAST(REPLACE(REPLACE(NULLIF(JSON_UNQUOTE(c.attributes->'$.createdAt'),'null'),'T',' '),'Z','') AS DATETIME(6)) AS created_at,CAST(NULLIF(JSON_UNQUOTE(c.attributes->'$.leadStaffId'),'null') AS UNSIGNED) AS lead_staff_id FROM catalog_item c WHERE c.catalog_type='M09.DEPARTMENT') department WHERE name=?",
                    Long.class,
                    "Hành chính – Kế toán");
            jdbc.update(
                    "UPDATE catalog_item c SET attributes=JSON_SET(attributes,'$.leadStaffId',?) WHERE c.catalog_type='M09.DEPARTMENT' AND (COALESCE(CAST(NULLIF(JSON_UNQUOTE(c.attributes->'$.legacy.id'),'null') AS UNSIGNED),c.id)=?)",
                    old,
                    department);
            long nurse = staff(Role.NURSING_STAFF);
            join(nurse, department);
            assertThatThrownBy(() -> replace(nurse)).isInstanceOf(BusinessException.class);
            // Lệnh bị từ chối dùng transaction riêng để không đánh rollback-only transaction kiểm thử.
            tx.setRollbackOnly();
        });
    }

    @Test
    void Q_292_resignedReplacementIsRejected() {
        long next = staff(Role.NURSING_STAFF, "RESIGNED");
        join(next, department);
        assertThatThrownBy(() -> replace(next)).isInstanceOf(BusinessException.class);
    }

    @Test
    void Q_292_callerFailureRollsBackBothDepartmentAndHistory() {
        long next = staff(Role.NURSING_STAFF);
        join(next, department);
        assertThatThrownBy(() -> new TransactionTemplate(transactions).executeWithoutResult(s -> {
                    service.replaceLead(old, next, new Reason("Thay"));
                    throw new IllegalStateException("Lệnh gọi thất bại");
                }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject(
                        "SELECT lead_staff_id FROM (SELECT COALESCE(CAST(NULLIF(JSON_UNQUOTE(c.attributes->'$.legacy.id'),'null') AS UNSIGNED),c.id) AS id,c.name AS name,c.status AS status,c.version AS version,CAST(REPLACE(REPLACE(NULLIF(JSON_UNQUOTE(c.attributes->'$.createdAt'),'null'),'T',' '),'Z','') AS DATETIME(6)) AS created_at,CAST(NULLIF(JSON_UNQUOTE(c.attributes->'$.leadStaffId'),'null') AS UNSIGNED) AS lead_staff_id FROM catalog_item c WHERE c.catalog_type='M09.DEPARTMENT') department WHERE id=?",
                        Long.class,
                        department))
                .isEqualTo(old);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM (SELECT COALESCE(CAST(NULLIF(JSON_UNQUOTE(h.details->'$.legacy.id'),'null') AS UNSIGNED),h.id) AS id,h.entity_id AS department_id,h.value_id AS staff_id,h.starts_at AS starts_at,h.ends_at AS ends_at,h.actor_id AS actor_id,h.reason AS reason FROM status_history h WHERE h.entity_type='M09.DEPARTMENT' AND h.attribute='LEAD_STAFF' AND h.kind='PERIOD') department_lead_history WHERE department_id=? AND ends_at IS NULL AND staff_id=?",
                        Integer.class,
                        department,
                        old))
                .isOne();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Users {
        @Bean
        @Primary
        SwitchableUser user() {
            return new SwitchableUser();
        }
    }
}
