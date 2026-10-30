package com.cambae.backend.m09;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m09.duty.internal.service.DepartmentDutyService;
import com.cambae.backend.support.*;
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
                "INSERT INTO department(name,lead_staff_id) VALUES (?,?)",
                "[Bộ phận kiểm thử " + sequence.incrementAndGet() + "]",
                old);
        fixtureDepartment = department;
        join(old, department);
        jdbc.update(
                "INSERT INTO department_lead_history(department_id,staff_id,starts_at,actor_id,reason) VALUES (?,?,UTC_TIMESTAMP(6)-INTERVAL 1 DAY,?,'[Khởi tạo test]')",
                department,
                old,
                current.accountId());
    }

    @AfterEach
    void removeOwnDepartmentFixture() throws java.sql.SQLException {
        // Chỉ dọn bộ phận do test này tạo, qua đúng tài khoản lưu giữ; không chạm danh mục khởi tạo.
        try (var connection = retentionConnection()) {
            connection.setAutoCommit(false);
            for (String table : java.util.List.of("department_lead_history", "staff_department", "department")) {
                String column = table.equals("department") ? "id" : "department_id";
                try (var statement = connection.prepareStatement("DELETE FROM " + table + " WHERE " + column + "=?")) {
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
                "INSERT INTO staff_department(staff_id,department_id,starts_at,actor_id,reason) VALUES (?,?,UTC_TIMESTAMP(6)-INTERVAL 1 DAY,?,'[Kiểm thử]')",
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
        assertThat(jdbc.queryForObject("SELECT lead_staff_id FROM department WHERE id=?", Long.class, department))
                .isEqualTo(next);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM department_lead_history WHERE department_id=? AND ends_at IS NULL AND staff_id=?",
                        Integer.class,
                        department,
                        next))
                .isOne();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM department_lead_history WHERE department_id=? AND ends_at IS NOT NULL AND staff_id=?",
                        Integer.class,
                        department,
                        old))
                .isOne();
    }

    @Test
    void Q_292_replacementInAnotherDepartmentIsRejectedWithoutChangingCurrentLead() {
        long next = staff(Role.NURSING_STAFF);
        assertThatThrownBy(() -> replace(next)).isInstanceOf(BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT lead_staff_id FROM department WHERE id=?", Long.class, department))
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
            jdbc.update("UPDATE department SET lead_staff_id=NULL WHERE id=?", department);
            department =
                    jdbc.queryForObject("SELECT id FROM department WHERE name=?", Long.class, "Hành chính – Kế toán");
            jdbc.update("UPDATE department SET lead_staff_id=? WHERE id=?", old, department);
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
        assertThat(jdbc.queryForObject("SELECT lead_staff_id FROM department WHERE id=?", Long.class, department))
                .isEqualTo(old);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM department_lead_history WHERE department_id=? AND ends_at IS NULL AND staff_id=?",
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
