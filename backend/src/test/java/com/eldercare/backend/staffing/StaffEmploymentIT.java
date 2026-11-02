package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.dto.AccountStatus;
import com.eldercare.backend.common.*;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.time.*;
import com.eldercare.backend.staffing.shift.internal.persistence.ShiftStore;
import com.eldercare.backend.staffing.staff.internal.service.StaffService;
import com.eldercare.backend.support.*;
import com.eldercare.backend.support.fake.care.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

/** T031: nghỉ việc/nhận lại đúng mốc, không khôi phục tài khoản/ca, bộ phận cần người thay. */
@Import({FakeM04Config.class, StaffEmploymentIT.Users.class})
class StaffEmploymentIT extends MySqlIntegrationTestBase {
    private static final java.util.concurrent.atomic.AtomicInteger sequence =
            new java.util.concurrent.atomic.AtomicInteger();

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PhaseTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    SwitchableUser current;

    @Autowired
    StaffService staff;

    @Autowired
    AccountApi accounts;

    @Autowired
    com.eldercare.backend.staffing.scheduling.M09MinuteJob minute;

    @Autowired
    FakeCareRequirements tasks;

    @Autowired
    com.eldercare.backend.administration.account.api.spi.DutyApi duties;

    private long employee, account, shift, assignment, floor;
    private Instant now;
    private Long fixtureDepartment;

    @BeforeEach
    void setup() {
        fixtureDepartment = null;
        now = Instant.parse("2037-06-01T01:00:00Z").plus(Duration.ofDays(sequence.incrementAndGet()));
        clock.setTo(now);
        time.set(now);
        current.set(new TestUsers(jdbc, new BusinessTime(Clock.systemUTC()))
                .staff(Role.NURSING_HOME_MANAGER)
                .user());
        var person =
                new TestUsers(jdbc, new BusinessTime(Clock.systemUTC())).staff(Role.NURSING_STAFF, Role.FLOOR_MANAGER);
        employee = person.staffId();
        account = person.accountId();
        floor = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO staff_profile(id,full_name,position,account_id) VALUES (?,'[Nhân viên kiểm thử nghỉ việc]','Điều dưỡng',?)",
                employee,
                account);
        long template = insert(
                "INSERT INTO shift_template(name,start_time,end_time,scope_type,requires_handover) VALUES ('[Mẫu kiểm thử nghỉ việc]','07:00','15:00','FLOOR',TRUE)");
        long schedule = insert(
                "INSERT INTO shift_schedule(month,scope_type,scope_id,status) VALUES (?,'FLOOR',?,'PUBLISHED')",
                YearMonth.from(now.atZone(BusinessTime.ZONE)).toString(),
                floor);
        shift = insert(
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,requires_handover,status,lead_staff_id) VALUES (?,?,?,?,'FLOOR',?,TRUE,'IN_PROGRESS',?)",
                schedule,
                template,
                ShiftStore.sql(now.minusSeconds(3600)),
                ShiftStore.sql(now.plusSeconds(3600)),
                floor,
                employee);
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", shift, employee);
        assignment = insert(
                "INSERT INTO staff_assignment(shift_id,staff_id,target_type,target_id,role,assignment_kind,starts_at,ends_at,status) VALUES (?,?,'FLOOR',?,'NURSING_STAFF','PRIMARY',?,?,'ACTIVE')",
                shift,
                employee,
                floor,
                ShiftStore.sql(now.minusSeconds(3600)),
                ShiftStore.sql(now.plusSeconds(3600)));
    }

    @AfterEach
    void reset() throws Exception {
        clock.reset();
        tasks.onResign(() -> {});
        if (fixtureDepartment != null)
            try (var c = retentionConnection()) {
                c.setAutoCommit(false);
                for (String table : List.of("department_lead_history", "staff_department", "department"))
                    try (var p = c.prepareStatement("DELETE FROM " + table + " WHERE "
                            + (table.equals("department") ? "id" : "department_id") + "=?")) {
                        p.setLong(1, fixtureDepartment);
                        p.executeUpdate();
                    }
                c.commit();
            }
    }

    private long insert(String sql, Object... args) {
        return AssignmentFixtures.insert(jdbc, sql, args);
    }

    private void terminate(Instant at, Long replacement) {
        ReflectionTestUtils.invokeMethod(
                staff,
                "terminateEmployment",
                employee,
                at,
                replacement,
                new Reason("Chấm dứt hợp đồng theo quyết định"));
    }

    private void rehire() {
        ReflectionTestUtils.invokeMethod(staff, "rehire", employee, new Reason("Nhận lại theo quyết định"));
    }

    @Test
    void BR_M09_04_immediateResignationLocksAccountWithdrawsShiftAndEndsAssignment() {
        terminate(now, null);
        assertThat(staff.get(employee).status()).isEqualTo("RESIGNED");
        assertThat(accounts.byStaff(employee).orElseThrow().status()).isEqualTo(AccountStatus.LOCKED);
        assertThat(jdbc.queryForObject(
                                "SELECT withdrawn_on_resignation_at FROM shift_staff WHERE shift_id=? AND staff_id=?",
                                java.sql.Timestamp.class,
                                shift,
                                employee)
                        .toInstant())
                .isEqualTo(now);
        assertThat(jdbc.queryForObject("SELECT status FROM staff_assignment WHERE id=?", String.class, assignment))
                .isEqualTo("ENDED");
        assertThat(jdbc.queryForObject("SELECT missing_lead FROM work_shift WHERE id=?", Boolean.class, shift))
                .isTrue();
    }

    @Test
    void FR_003_futureResignationAppliesOnceAtEffectiveTime() {
        var effective = now.plusSeconds(60);
        terminate(effective, null);
        assertThat(staff.get(employee).status()).isEqualTo("WORKING");
        assertThat(accounts.byStaff(employee).orElseThrow().status()).isEqualTo(AccountStatus.ACTIVE);
        clock.setTo(effective);
        time.set(effective);
        minute.runNow();
        minute.runNow();
        assertThat(staff.get(employee).status()).isEqualTo("RESIGNED");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type='M09.STAFF' AND entity_id=? AND action='STAFF_EMPLOYMENT_ENDED'",
                        Integer.class,
                        Long.toString(employee)))
                .isEqualTo(1);
    }

    @Test
    void FR_003_rehireDoesNotReopenAccountOrRestoreAssignments() {
        terminate(now, null);
        rehire();
        assertThat(staff.get(employee).status()).isEqualTo("WORKING");
        assertThat(accounts.byStaff(employee).orElseThrow().status()).isEqualTo(AccountStatus.LOCKED);
        assertThat(jdbc.queryForObject("SELECT status FROM staff_assignment WHERE id=?", String.class, assignment))
                .isEqualTo("ENDED");
    }

    @Test
    void FR_003_effectiveTimeCannotBeInPast() {
        assertThatThrownBy(() -> terminate(now.minusSeconds(1), null)).isInstanceOf(BusinessException.class);
        assertThat(staff.get(employee).status()).isEqualTo("WORKING");
    }

    @Test
    void Q_292_departmentLeadMustBeReplacedInSameCommand() {
        long dept = insert(
                "INSERT INTO department(name,lead_staff_id) VALUES (?,?)",
                "[Bộ phận nghỉ việc " + employee + "]",
                employee);
        fixtureDepartment = dept;
        jdbc.update(
                "INSERT INTO staff_department(staff_id,department_id,starts_at,actor_id,reason) VALUES (?,?,?,?,'[Khởi tạo]')",
                employee,
                dept,
                ShiftStore.sql(now.minusSeconds(1)),
                current.accountId());
        assertThatThrownBy(() -> terminate(now, null)).isInstanceOf(BusinessException.class);
        assertThat(staff.get(employee).status()).isEqualTo("WORKING");
        assertThat(accounts.byStaff(employee).orElseThrow().status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void BR_M09_04_careFailureRollsBackAccountShiftAssignmentAndStaff() {
        tasks.onResign(
                () -> {
                    assertThat(org.springframework.transaction.support.TransactionSynchronizationManager
                                    .isActualTransactionActive())
                            .isTrue();
                    throw new IllegalStateException("Nguồn công việc từ chối nghỉ việc");
                });
        assertThatThrownBy(() -> terminate(now, null)).isInstanceOf(IllegalStateException.class);
        assertThat(staff.get(employee).status()).isEqualTo("WORKING");
        assertThat(accounts.byStaff(employee).orElseThrow().status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(jdbc.queryForObject("SELECT status FROM staff_assignment WHERE id=?", String.class, assignment))
                .isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT missing_lead FROM work_shift WHERE id=?", Boolean.class, shift))
                .isFalse();
    }

    @Test
    void FR_004_futureFloorManagerAssignmentNeverGrantsScopeAfterResignation() {
        jdbc.update(
                "INSERT INTO floor_manager_assignment(staff_id,floor_area_id,starts_at,assigned_by) VALUES (?,?,?,?)",
                employee,
                floor,
                ShiftStore.sql(now.plusSeconds(3600)),
                current.accountId());
        terminate(now, null);
        assertThat(duties.assignedFloorManager(floor, now.plusSeconds(7200))).isEmpty();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action='FLOOR_MANAGER_ENDED_BY_EMPLOYMENT' AND entity_type='M09.FLOOR_MANAGER_ASSIGNMENT'",
                        Integer.class))
                .isPositive();
    }

    static class PhaseTime extends BusinessTime {
        private volatile Instant at = Instant.parse("2030-01-01T00:00:00Z");

        PhaseTime() {
            super(Clock.systemUTC());
        }

        void set(Instant at) {
            this.at = at;
        }

        @Override
        public Instant now() {
            return at;
        }

        @Override
        public LocalDate today() {
            return at.atZone(ZONE).toLocalDate();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Users {
        @Bean
        @Primary
        SwitchableUser current() {
            return new SwitchableUser();
        }

        @Bean
        @Primary
        PhaseTime phaseTime() {
            return new PhaseTime();
        }
    }
}
