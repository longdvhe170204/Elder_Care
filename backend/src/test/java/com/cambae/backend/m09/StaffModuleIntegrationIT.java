package com.cambae.backend.m09;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.*;
import com.cambae.backend.m04.api.spi.CareShiftPort;
import com.cambae.backend.m05.api.spi.AlertStaffPort;
import com.cambae.backend.m07.api.spi.NursingShiftPort;
import com.cambae.backend.m09.shift.internal.persistence.ShiftStore;
import com.cambae.backend.m15.account.api.ScopeApi;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.*;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.TestUsers;
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
                "INSERT INTO shift_template(name,start_time,end_time,scope_type) VALUES ('[Tích hợp phạm vi]','07:00','15:00','FLOOR')");
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
                "INSERT INTO shift_lead_history(shift_id,staff_id,starts_at,ends_at,basis) VALUES (?,?,?,?,'SCHEDULED')",
                shift,
                oldLead.staffId(),
                ShiftStore.sql(start),
                ShiftStore.sql(change));
        jdbc.update(
                "INSERT INTO shift_lead_history(shift_id,staff_id,starts_at,basis) VALUES (?,?,?,'CHANGED')",
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
            NursingShiftPort.class,
            com.cambae.backend.m07.api.spi.ShiftNursePort.class,
            AlertStaffPort.class
        })
            assertThat(AopUtils.getTargetClass(context.getBean(api)).getPackageName())
                    .startsWith("com.cambae.backend.m09.");
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
        var medication = context.getBean(com.cambae.backend.m07.api.spi.ShiftNursePort.class);
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
                "UPDATE shift_lead_history SET ends_at=? WHERE shift_id=? AND ends_at IS NULL",
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
                "INSERT INTO shift_lead_history(shift_id,staff_id,starts_at,ends_at,basis) VALUES (?,?,?,?,'CHANGED')",
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
