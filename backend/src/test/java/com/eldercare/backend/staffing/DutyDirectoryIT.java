package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionBasis;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.Duty;
import com.eldercare.backend.administration.account.api.spi.DutyApi;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Q-262, 002 FR-019e, 014 FR-066: quyền của Người phụ trách ca theo sự có mặt của Trưởng tầng trong đúng ca. */
@Import(PostPublicationShiftIT.Time.class)
@Transactional
class DutyDirectoryIT extends MySqlIntegrationTestBase {
    private static final PermissionKey QUALITY_CHECK = new PermissionKey(Function.QUALITY_CHECK, Operation.CREATE);

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    DutyApi duties;

    @Autowired
    PermissionApi permissions;

    private final Instant start = Instant.parse("2031-01-13T00:00:00Z");
    private final Instant change = start.plusSeconds(7200);
    private long floor, shift, manager;
    private TestUsers.TestUser lead;

    @BeforeEach
    void fixture() {
        clock.setTo(change);
        lead = person(Role.NURSING_STAFF);
        manager = person(Role.FLOOR_MANAGER).staffId();
        floor = TestUsers.nextId();
        shift = shiftAt(floor);
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", shift, lead.staffId());
        jdbc.update(
                "INSERT INTO status_history(entity_type,entity_id,attribute,module,kind,value_id,starts_at,command,by_system) SELECT 'M09.SHIFT',src.shift_id,'LEAD_STAFF','M09','PERIOD',src.staff_id,src.starts_at,src.basis,FALSE FROM (SELECT ? AS shift_id,? AS staff_id,? AS starts_at,'SCHEDULED' AS basis) src",
                shift,
                lead.staffId(),
                ShiftStore.sql(start));
        jdbc.update(
                "INSERT INTO staff_assignment(kind,staff_id,floor_area_id,starts_at,assigned_by,reason,status) SELECT 'FLOOR_MANAGER',src.staff_id,src.floor_area_id,src.starts_at,src.assigned_by,src.reason,'ACTIVE' FROM (SELECT ? AS staff_id,? AS floor_area_id,? AS starts_at,? AS assigned_by,? AS reason) src",
                manager,
                floor,
                ShiftStore.sql(start.minusSeconds(86400)),
                lead.accountId(),
                "[Giao Trưởng tầng kiểm thử Q-262]");
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void FR_019e_Q_262_managerNotInPublishedShift_allowsShiftLeadQualityCheck() {
        assertException(true);
        assertThat(permissions
                        .hasPermission(lead.user(), QUALITY_CHECK, target())
                        .basis())
                .contains(PermissionBasis.duty(Duty.SHIFT_LEAD));
    }

    @Test
    void FR_019e_Q_262_managerAbsenceStartsAtRecordedInstant_withoutGrantingEarly() {
        addManager(shift);
        jdbc.update(
                "UPDATE shift_staff SET absent_at=? WHERE shift_id=? AND staff_id=?",
                ShiftStore.sql(change),
                shift,
                manager);
        clock.setTo(change.minusNanos(1000));
        assertException(false);
        clock.setTo(change);
        assertException(true);
    }

    @Test
    void FR_019e_Q_262_approvedLeaveAllowsShiftLeadQualityCheck() {
        addManager(shift);
        jdbc.update(
                "UPDATE shift_staff SET approved_leave_request_id=? WHERE shift_id=? AND staff_id=?",
                TestUsers.nextId(),
                shift,
                manager);
        assertException(true);
    }

    @Test
    void FR_019e_Q_262_managerPresentInTheSameShift_keepsExceptionClosed() {
        addManager(shift);
        assertException(false);
    }

    @Test
    void FR_019e_Q_262_managerWorkingAnotherFloor_doesNotBlockThisShiftLead() {
        addManager(shiftAt(TestUsers.nextId()));
        assertException(true);
    }

    @Test
    void FR_019e_Q_262_draftShiftDoesNotQualifyForTheException() {
        jdbc.update("UPDATE work_shift SET status='DRAFT' WHERE id=?", shift);
        assertException(false);
    }

    @Test
    void FR_019e_Q_262_unknownShiftDoesNotQualifyForTheException() {
        assertThat(duties.shiftHasNoFloorManager(TestUsers.nextId())).isFalse();
    }

    private void assertException(boolean expected) {
        assertThat(duties.shiftHasNoFloorManager(shift)).isEqualTo(expected);
        assertThat(permissions
                        .hasPermission(lead.user(), QUALITY_CHECK, target())
                        .allowed())
                .isEqualTo(expected);
    }

    private Target target() {
        return new Target("M03.FLOOR", Long.toString(floor), null, floor);
    }

    private void addManager(long shiftId) {
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", shiftId, manager);
    }

    private TestUsers.TestUser person(Role role) {
        var user = new TestUsers(jdbc, time).staff(role);
        jdbc.update(
                "INSERT INTO staff_profile(id,full_name,position,account_id) VALUES (?,?,?,?)",
                user.staffId(),
                "[Nhân viên kiểm thử Q-262]",
                role.name(),
                user.accountId());
        return user;
    }

    private long shiftAt(long floorId) {
        long template = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) SELECT 'M09.SHIFT_TEMPLATE','M09',src.name,'ACTIVE',JSON_OBJECT('createdAt',DATE_FORMAT(UTC_TIMESTAMP(6),'%Y-%m-%dT%H:%i:%s.%fZ'),'startTime',TIME_FORMAT(src.start_time,'%H:%i:%s'),'endTime',TIME_FORMAT(src.end_time,'%H:%i:%s'),'scopeType',src.scope_type,'requiresHandover',IF(TRUE,CAST('true' AS JSON),CAST('false' AS JSON)),'originalName',src.name) FROM (SELECT '[Ca kiểm thử Q-262] #"
                        + com.eldercare.backend.support.TestUsers.nextId()
                        + "' AS name,'07:00' AS start_time,'15:00' AS end_time,'FLOOR' AS scope_type) src");
        long schedule = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO shift_schedule(month,scope_type,scope_id,status) VALUES ('2031-01','FLOOR',?,'PUBLISHED')",
                floorId);
        return AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,status,lead_staff_id,requires_handover) VALUES (?,?,?,?,'FLOOR',?,'PUBLISHED',?,TRUE)",
                schedule,
                template,
                ShiftStore.sql(start),
                ShiftStore.sql(start.plusSeconds(28800)),
                floorId,
                lead.staffId());
    }
}
