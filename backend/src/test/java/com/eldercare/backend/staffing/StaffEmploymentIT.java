package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.dto.AccountStatus;
import com.eldercare.backend.common.*;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.*;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import com.eldercare.backend.staffing.internal.staff.service.StaffService;
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
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) SELECT 'M09.SHIFT_TEMPLATE','M09',src.name,'ACTIVE',JSON_OBJECT('createdAt',DATE_FORMAT(UTC_TIMESTAMP(6),'%Y-%m-%dT%H:%i:%s.%fZ'),'startTime',TIME_FORMAT(src.start_time,'%H:%i:%s'),'endTime',TIME_FORMAT(src.end_time,'%H:%i:%s'),'scopeType',src.scope_type,'requiresHandover',IF(src.requires_handover,CAST('true' AS JSON),CAST('false' AS JSON)),'originalName',src.name) FROM (SELECT '[Mẫu kiểm thử nghỉ việc] #"
                        + com.eldercare.backend.support.TestUsers.nextId()
                        + "' AS name,'07:00' AS start_time,'15:00' AS end_time,'FLOOR' AS scope_type,TRUE AS requires_handover) src");
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
                for (String statement : List.of(
                        "DELETE FROM status_history WHERE entity_type='M09.DEPARTMENT' AND attribute='LEAD_STAFF' AND entity_id=?",
                        "DELETE FROM staff_department WHERE department_id=(SELECT id FROM catalog_item WHERE catalog_type='M09.DEPARTMENT' AND COALESCE(m09_legacy_id,id)=?)",
                        "DELETE FROM catalog_item WHERE catalog_type='M09.DEPARTMENT' AND COALESCE(m09_legacy_id,id)=?"))
                    try (var p = c.prepareStatement(statement)) {
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
        // 002 US2 #4, FR-009 (quickstart 002 K7, B1): nhật ký khóa tài khoản ghi căn cứ BR-M09-04
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_UNQUOTE(JSON_EXTRACT(basis, '$.ruleCode')) FROM audit_log WHERE action = 'ACCOUNT_LOCKED'"
                                + " AND entity_id = ? ORDER BY id DESC LIMIT 1",
                        String.class,
                        Long.toString(accounts.byStaff(employee).orElseThrow().accountId())))
                .isEqualTo("BR-M09-04");
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
