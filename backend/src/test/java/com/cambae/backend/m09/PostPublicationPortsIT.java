package com.cambae.backend.m09;

import static org.assertj.core.api.Assertions.*;

import com.cambae.backend.common.*;
import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.*;
import com.cambae.backend.m09.assignment.internal.domain.AssignmentInput;
import com.cambae.backend.m09.assignment.internal.service.port.AssignmentCommandPort;
import com.cambae.backend.m09.shift.internal.service.port.*;
import com.cambae.backend.m15.auditlog.api.Basis;
import com.cambae.backend.support.*;
import com.cambae.backend.support.fake.m03.*;
import com.cambae.backend.support.fake.m03.FakeLocations;
import com.cambae.backend.support.fake.m04.FakeM04Config;
import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

@Import({
    FakeLocations.Config.class,
    FakeM04Config.class,
    PostPublicationShiftIT.Time.class,
    PostPublicationPortsIT.Users.class
})
class PostPublicationPortsIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    SwitchableUser current;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    FakeLocations locations;

    @Autowired
    ShiftCommandPort shifts;

    @Autowired
    AssignmentCommandPort assignments;

    long shift, floor, worker, newStaff;
    final Instant from = Instant.parse("2026-10-25T01:00:00Z"), to = from.plusSeconds(28800);
    final Basis basis = new Basis(null, null, 12345L, null);

    @BeforeEach
    void setup() {
        clock.setTo(from.minusSeconds(3600));
        var users = new TestUsers(jdbc, time);
        current.set(users.staff(Role.NURSING_HOME_MANAGER).user());
        worker = person(users, Role.CAREGIVER);
        newStaff = person(users, Role.NURSING_STAFF);
        floor = TestUsers.nextId();
        locations.floor(floor, null);
        long template = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO shift_template(name,start_time,end_time,scope_type,requires_handover) VALUES ('[Mẫu port]','08:00','16:00','FLOOR',FALSE)");
        long schedule = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO shift_schedule(month,scope_type,scope_id,status) VALUES ('2026-10','FLOOR',?,'PUBLISHED')",
                floor);
        shift = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,requires_handover,status) VALUES (?,?,?,?,'FLOOR',?,FALSE,'PUBLISHED')",
                schedule,
                template,
                com.cambae.backend.m09.shift.internal.persistence.ShiftStore.sql(from),
                com.cambae.backend.m09.shift.internal.persistence.ShiftStore.sql(to),
                floor);
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", shift, worker);
    }

    private long person(TestUsers users, Role role) {
        long max = jdbc.queryForObject("SELECT COALESCE(MAX(id),0) FROM staff_profile", Long.class);
        while (TestUsers.nextId() <= max) {}
        var u = users.staff(role);
        jdbc.update(
                "INSERT INTO staff_profile(id,full_name,position,account_id) VALUES (?,'[Nhân viên port]','Chăm sóc',?)",
                u.staffId(),
                u.accountId());
        return u.staffId();
    }

    @AfterEach
    void reset() {
        clock.reset();
    }

    private AssignmentInput input(long person) {
        return new AssignmentInput(person, "FLOOR", floor, null, Role.CAREGIVER, "SUPPORT", false, from, to);
    }

    private ShiftEntryBasis entry() {
        return new ShiftEntryBasis(ShiftEntryBasis.Kind.SWAPPED, 12345, new Reason("[Yêu cầu đổi ca kiểm thử]"));
    }

    @Test
    void FR_017_portsRequireCallerTransaction() {
        assertThatThrownBy(() -> shifts.addToShift(shift, newStaff, entry(), false))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThatThrownBy(() -> assignments.cancel(0, basis)).isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void FR_017_swapMembershipAndAssignmentRollbackWithCallingCommand() {
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
                    shifts.leaveShift(shift, worker, new ShiftDepartureBasis(ShiftDepartureBasis.Kind.SWAPPED, 12345));
                    shifts.addToShift(shift, newStaff, entry(), false);
                    assignments.create(shift, input(newStaff), basis);
                    throw new IllegalStateException("[Hoàn tác lệnh gọi]");
                }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject(
                        "SELECT left_at FROM shift_staff WHERE shift_id=? AND staff_id=?", Object.class, shift, worker))
                .isNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM shift_staff WHERE shift_id=?", Integer.class, shift))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM staff_assignment WHERE shift_id=?", Integer.class, shift))
                .isZero();
    }

    @Test
    void FR_017_portsReuseValidationAndKeepSourceAndHistoricalMembership() {
        tx.executeWithoutResult(status -> {
            assertThat(assignments.validate(shift, input(newStaff))).isNotEmpty();
            shifts.addToShift(shift, newStaff, entry(), false);
            assertThat(assignments.validate(shift, input(newStaff))).isEmpty();
            long assignment = assignments.create(shift, input(newStaff), basis);
            assignments.cancel(assignment, basis);
            shifts.leaveShift(shift, worker, new ShiftDepartureBasis(ShiftDepartureBasis.Kind.APPROVED_LEAVE, 12345));
            shifts.changeLead(shift, newStaff, basis);
        });
        assertThat(jdbc.queryForObject(
                        "SELECT entry_basis FROM shift_staff WHERE shift_id=? AND staff_id=?",
                        String.class,
                        shift,
                        newStaff))
                .isEqualTo("SWAPPED");
        assertThat(jdbc.queryForObject(
                        "SELECT approved_leave_request_id FROM shift_staff WHERE shift_id=? AND staff_id=?",
                        Long.class,
                        shift,
                        worker))
                .isEqualTo(12345);
        assertThat(jdbc.queryForObject("SELECT source FROM staff_assignment WHERE shift_id=?", String.class, shift))
                .isEqualTo("SHIFT_SWAP_015");
        assertThat(jdbc.queryForObject("SELECT status FROM staff_assignment WHERE shift_id=?", String.class, shift))
                .isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject("SELECT lead_staff_id FROM work_shift WHERE id=?", Long.class, shift))
                .isEqualTo(newStaff);
    }

    @Test
    void FR_019_portMarksMissingLeadOnlyWhenNoEligibleMemberRemains() {
        tx.executeWithoutResult(status -> shifts.markMissingLead(shift, basis));
        assertThat(jdbc.queryForObject("SELECT missing_lead FROM work_shift WHERE id=?", Boolean.class, shift))
                .isTrue();
        assertThat(jdbc.queryForObject("SELECT lead_staff_id FROM work_shift WHERE id=?", Long.class, shift))
                .isNull();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Users {
        @Bean
        @Primary
        SwitchableUser phase8PortUser() {
            return new SwitchableUser();
        }
    }
}
