package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.*;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.care.api.spi.CareShiftPort;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.*;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.medication.api.spi.NursingShiftPort;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.time.Instant;
import org.junit.jupiter.api.*;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** T043: kiểm kết nối với bộ tính phạm vi thật của A, không sửa test cô lập của chủ module khác. */
@Import(PostPublicationShiftIT.Time.class)
@Transactional
class StaffModuleIntegrationIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    ApplicationContext context;

    @Autowired
    ScopeApi scopes;

    @Autowired
    AssignmentApi assignments;

    @Autowired
    DutyApi duties;

    final Instant start = Instant.parse("2026-10-19T00:00:00Z"),
            change = start.plusSeconds(14400),
            end = start.plusSeconds(28800);
    long floor, shift;
    TestUsers.TestUser oldLead, newLead;

    @BeforeEach
    void fixture() {
        clock.setTo(start);
        oldLead = person();
        newLead = person();
        floor = TestUsers.nextId();
        long template = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) SELECT 'M09.SHIFT_TEMPLATE','M09',src.name,'ACTIVE',JSON_OBJECT('createdAt',DATE_FORMAT(UTC_TIMESTAMP(6),'%Y-%m-%dT%H:%i:%s.%fZ'),'startTime',TIME_FORMAT(src.start_time,'%H:%i:%s'),'endTime',TIME_FORMAT(src.end_time,'%H:%i:%s'),'scopeType',src.scope_type,'requiresHandover',IF(TRUE,CAST('true' AS JSON),CAST('false' AS JSON)),'originalName',src.name) FROM (SELECT '[Tích hợp phạm vi] #"
                        + com.eldercare.backend.support.TestUsers.nextId()
                        + "' AS name,'07:00' AS start_time,'15:00' AS end_time,'FLOOR' AS scope_type) src");
        long schedule = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO shift_schedule(month,scope_type,scope_id,status) VALUES ('2026-10','FLOOR',?,'PUBLISHED')",
                floor);
        shift = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,status,lead_staff_id,requires_handover) VALUES (?,?,?,?,'FLOOR',?,'PUBLISHED',?,TRUE)",
                schedule,
                template,
                ShiftStore.sql(start),
                ShiftStore.sql(end),
                floor,
                newLead.staffId());
        for (long person : new long[] {oldLead.staffId(), newLead.staffId()})
            jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", shift, person);
        jdbc.update(
                "INSERT INTO status_history(entity_type,entity_id,attribute,module,kind,value_id,starts_at,ends_at,command,by_system) SELECT 'M09.SHIFT',src.shift_id,'LEAD_STAFF','M09','PERIOD',src.staff_id,src.starts_at,src.ends_at,src.basis,FALSE FROM (SELECT ? AS shift_id,? AS staff_id,? AS starts_at,? AS ends_at,'SCHEDULED' AS basis) src",
                shift,
                oldLead.staffId(),
                ShiftStore.sql(start),
                ShiftStore.sql(change));
        jdbc.update(
                "INSERT INTO status_history(entity_type,entity_id,attribute,module,kind,value_id,starts_at,command,by_system) SELECT 'M09.SHIFT',src.shift_id,'LEAD_STAFF','M09','PERIOD',src.staff_id,src.starts_at,src.basis,FALSE FROM (SELECT ? AS shift_id,? AS staff_id,? AS starts_at,'CHANGED' AS basis) src",
                shift,
                newLead.staffId(),
                ShiftStore.sql(change));
    }

    private TestUsers.TestUser person() {
        long max = jdbc.queryForObject("SELECT COALESCE(MAX(id),0) FROM staff_profile", Long.class);
        while (TestUsers.nextId() <= max) {}
        var user = new TestUsers(jdbc, time).staff(Role.NURSING_STAFF);
        jdbc.update(
                "INSERT INTO staff_profile(id,full_name,position,account_id) VALUES (?,'[Kiểm phạm vi thật]','NURSING_STAFF',?)",
                user.staffId(),
                user.accountId());
        return user;
    }

    @AfterEach
    void reset() {
        clock.reset();
    }

    @Test
    void FR_049_allPublishedStaffSpisUseM09BeansWithoutFakeOverrides() {
        for (Class<?> api : new Class<?>[] {
            StaffProfileApi.class,
            LicenseApi.class,
            PracticeScopeCatalogApi.class,
            AssignmentApi.class,
            DutyApi.class,
            CareShiftPort.class,
            com.eldercare.backend.care.api.spi.ShiftStaffPort.class,
            NursingShiftPort.class,
            com.eldercare.backend.medication.api.spi.ShiftNursePort.class,
            AlertStaffPort.class
        })
            assertThat(AopUtils.getTargetClass(context.getBean(api)).getPackageName())
                    .startsWith("com.eldercare.backend.staffing.");
    }

    @Test
    void FR_049_careStaffPortUsesPublishedShiftMembersAndActualTimeBoundaries() {
        var port = context.getBean(com.eldercare.backend.care.api.spi.ShiftStaffPort.class);
        assertThat(port.shift(shift).orElseThrow().published()).isTrue();
        assertThat(port.publishedShifts(floor, start, end))
                .extracting(s -> s.id())
                .containsExactly(shift);
        assertThat(port.shiftContaining(floor, start).orElseThrow().id()).isEqualTo(shift);
        assertThat(port.shiftContaining(floor, end)).isEmpty();
        assertThat(port.shiftLead(shift)).contains(newLead.staffId());
        assertThat(port.members(shift))
                .extracting(m -> m.staffId())
                .containsExactlyInAnyOrder(oldLead.staffId(), newLead.staffId());
        assertThat(port.onShift(oldLead.staffId(), start.minusNanos(1000))).isFalse();
        assertThat(port.onShift(oldLead.staffId(), start)).isTrue();
        assertThat(port.onShift(oldLead.staffId(), end)).isFalse();
        assertThat(port.qualified(oldLead.staffId(), java.util.Set.of(), time.today()))
                .isTrue();
        assertThat(port.qualified(oldLead.staffId(), java.util.Set.of("MISSING_CERTIFICATE"), time.today()))
                .isFalse();
        assertThat(port.hasQualifiedStaff(shift, Role.NURSING_STAFF, java.util.Set.of()))
                .isTrue();
        assertThat(port.hasQualifiedStaff(shift, Role.DOCTOR, java.util.Set.of()))
                .isFalse();
        assertThat(port.handoverConfirmed(shift)).isFalse();
        jdbc.update(
                "UPDATE shift_staff SET absent_at=? WHERE shift_id=? AND staff_id=?",
                ShiftStore.sql(start),
                shift,
                oldLead.staffId());
        assertThat(port.members(shift)).extracting(m -> m.staffId()).containsExactly(newLead.staffId());
        assertThat(port.onShift(oldLead.staffId(), start)).isFalse();
        jdbc.update("UPDATE work_shift SET status='DRAFT' WHERE id=?", shift);
        assertThat(port.shift(shift).orElseThrow().published()).isFalse();
        assertThat(port.members(shift)).isEmpty();
        assertThat(port.publishedShifts(floor, start, end)).isEmpty();
        assertThat(port.hasQualifiedStaff(shift, Role.NURSING_STAFF, java.util.Set.of()))
                .isFalse();
    }

    @Test
    void FR_019_dutyAtTimeUsesLeadHistoryInsteadOfTheCurrentShiftPointer() {
        assertThat(duties.shiftLead(floor, change.minusNanos(1000))).contains(oldLead.staffId());
        assertThat(duties.shiftLead(floor, change)).contains(newLead.staffId());
        assertThat(scopes.shiftLead(floor, change.minusNanos(1000))).contains(oldLead.accountId());
        assertThat(scopes.shiftLead(floor, change)).contains(newLead.accountId());
    }

    @Test
    void FR_049_medicationShiftNursePortUsesTheSameRecordedShiftAndLead() {
        var medication = context.getBean(com.eldercare.backend.medication.api.spi.ShiftNursePort.class);
        var before = medication.shiftContaining(floor, change.minusNanos(1000)).orElseThrow();
        assertThat(before.shiftId()).isEqualTo(shift);
        assertThat(before.shiftLeadId()).isEqualTo(oldLead.staffId());
        assertThat(medication.shiftContaining(floor, change).orElseThrow().shiftLeadId())
                .isEqualTo(newLead.staffId());
        assertThat(medication.nursesOnShift(floor, start))
                .containsExactlyInAnyOrder(oldLead.staffId(), newLead.staffId());
        assertThat(medication.isStaffOnActiveShift(oldLead.staffId(), end)).isFalse();
        assertThat(medication.isHandoverConfirmed(shift)).isFalse();
    }

    @Test
    void FR_037a_oldLeadKeepsPastScopeAndLosesItAtTheRecordedReplacement() {
        var target = new Target("M09.SHIFT", Long.toString(shift), null, floor);
        assertThat(assignments.history(oldLead.staffId(), start, end))
                .singleElement()
                .satisfies(a -> {
                    assertThat(a.kind()).isEqualTo(AssignmentKind.SHIFT_LEAD);
                    assertThat(a.scopeEndsAt()).isEqualTo(change);
                });
        assertThat(scopes.inScope(oldLead.user(), target, change.minusNanos(1000)))
                .isTrue();
        assertThat(scopes.inScope(oldLead.user(), target, change)).isFalse();
        assertThat(scopes.inScope(newLead.user(), target, change)).isTrue();
    }

    @Test
    void FR_019_missingLeadTodayDoesNotErasePreviouslyRecordedDuty() {
        Instant absence = change.plusSeconds(3600);
        jdbc.update(
                "UPDATE shift_staff SET absent_at=? WHERE shift_id=? AND staff_id=?",
                ShiftStore.sql(absence),
                shift,
                newLead.staffId());
        jdbc.update(
                "UPDATE status_history h SET ends_at=? WHERE h.entity_type='M09.SHIFT' AND h.attribute='LEAD_STAFF' AND h.kind='PERIOD' AND (h.entity_id=? AND h.ends_at IS NULL)",
                ShiftStore.sql(absence),
                shift);
        jdbc.update("UPDATE work_shift SET lead_staff_id=NULL,missing_lead=TRUE WHERE id=?", shift);
        assertThat(duties.shiftLead(floor, change.minusNanos(1000))).contains(oldLead.staffId());
        assertThat(duties.shiftLead(floor, absence.minusNanos(1000))).contains(newLead.staffId());
        assertThat(duties.shiftLead(floor, absence)).isEmpty();
        var target = new Target("M09.SHIFT", Long.toString(shift), null, floor);
        assertThat(scopes.inScope(newLead.user(), target, absence.minusNanos(1000)))
                .isTrue();
        assertThat(scopes.inScope(newLead.user(), target, absence)).isFalse();
    }

    @Test
    void FR_015_ambiguousHistoricalLeadsFailClosedInsteadOfChoosingOne() {
        jdbc.update(
                "INSERT INTO status_history(entity_type,entity_id,attribute,module,kind,value_id,starts_at,ends_at,command,by_system) SELECT 'M09.SHIFT',src.shift_id,'LEAD_STAFF','M09','PERIOD',src.staff_id,src.starts_at,src.ends_at,src.basis,FALSE FROM (SELECT ? AS shift_id,? AS staff_id,? AS starts_at,? AS ends_at,'CHANGED' AS basis) src",
                shift,
                newLead.staffId(),
                ShiftStore.sql(start),
                ShiftStore.sql(change));
        assertThatThrownBy(() -> duties.shiftLead(floor, change.minusSeconds(1)))
                .isInstanceOf(InvalidPermissionDataException.class);
    }

    @Test
    void FR_025_normalShiftEndPreservesConfiguredMarginUnlikeEarlyReplacement() {
        var margin = context.getBean(ParameterApi.class)
                .read(ParameterCode.CFG_M15_07)
                .asDuration();
        var target = new Target("M09.SHIFT", Long.toString(shift), null, floor);
        assertThat(scopes.inScope(newLead.user(), target, end.plus(margin).minusNanos(1000)))
                .isTrue();
        assertThat(scopes.inScope(newLead.user(), target, end.plus(margin))).isFalse();
    }
}
