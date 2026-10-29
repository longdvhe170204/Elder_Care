package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.staffing.api.StaffStatisticsApi;
import com.eldercare.backend.staffing.ratio.internal.domain.StaffingRatioCalculator.Result;
import com.eldercare.backend.staffing.ratio.internal.persistence.StaffingRatioStore;
import com.eldercare.backend.staffing.shift.internal.persistence.ShiftStore;
import com.eldercare.backend.support.*;
import com.eldercare.backend.support.fake.roombedallocation.*;
import com.eldercare.backend.support.fake.roombedallocation.FakeLocations;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(FakeLocations.Config.class)
class StaffStatisticsIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    StaffStatisticsApi statistics;

    @Autowired
    StaffingRatioStore store;

    @Autowired
    FakeLocations locations;

    @Autowired
    com.eldercare.backend.common.time.BusinessTime time;

    @Autowired
    com.eldercare.backend.administration.parameter.api.ParameterApi parameters;

    final Instant start = Instant.parse("2026-10-25T00:00:00Z");
    final LocalDate day = LocalDate.of(2026, 10, 25);

    private long shift(String type, long scope) {
        long template = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO shift_template(name,start_time,end_time,scope_type,requires_handover) VALUES ('[Mẫu thống kê]','07:00','15:00',?,FALSE)",
                type);
        long schedule = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO shift_schedule(month,scope_type,scope_id,status) VALUES ('2026-10',?,?,'PUBLISHED')",
                type,
                scope);
        return AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,requires_handover,status) VALUES (?,?,?,?,?,?,FALSE,'PUBLISHED')",
                schedule,
                template,
                ShiftStore.sql(start),
                ShiftStore.sql(start.plusSeconds(28800)),
                type,
                scope);
    }

    private void record(long id, Instant at, boolean passed) {
        store.append(
                id,
                at,
                "CARE_LEVEL_CHANGED",
                new Result(
                        new BigDecimal("35.00"),
                        passed ? 5 : 4,
                        new BigDecimal(passed ? "7.00" : "8.75"),
                        new BigDecimal("8.00"),
                        passed));
    }

    private long staff() {
        return AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO staff_profile(full_name,position) VALUES ('[Nhân viên thống kê kiểm thử]','Chăm sóc')");
    }

    @Test
    void FR_037a_historyIncludesFloorManagerMembersAndLead_withActualIntervals() {
        long floor = TestUsers.nextId(), other = TestUsers.nextId();
        locations.floor(floor, null).floor(other, null);
        long id = shift("FLOOR", floor), person = staff();
        AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO floor_manager_assignment(staff_id,floor_area_id,starts_at,ends_at,status) VALUES (?,?,?,?,'ENDED')",
                person,
                floor,
                ShiftStore.sql(start),
                ShiftStore.sql(start.plusSeconds(7200)));
        AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO shift_staff(shift_id,staff_id,added_at,absent_at) VALUES (?,?,?,?)",
                id,
                person,
                ShiftStore.sql(start.plusSeconds(60)),
                ShiftStore.sql(start.plusSeconds(3600)));
        AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO shift_lead_history(shift_id,staff_id,starts_at,ends_at,basis) VALUES (?,?,?,?,'SCHEDULED')",
                id,
                person,
                ShiftStore.sql(start),
                ShiftStore.sql(start.plusSeconds(1800)));
        var scope = new ScopeFilter(false, Set.of(floor), Set.of(), Set.of());
        var rows = statistics
                .assignmentHistory(scope, start, start.plusSeconds(28800))
                .toList();
        assertThat(rows).filteredOn(r -> r.staffId() == person).hasSize(3);
        assertThat(rows).anySatisfy(r -> {
            assertThat(r.role()).isEqualTo("FLOOR_MANAGER");
            assertThat(r.shiftId()).isNull();
            assertThat(r.targetId()).isEqualTo(floor);
            assertThat(r.to()).isEqualTo(start.plusSeconds(7200));
        });
        assertThat(rows).anySatisfy(r -> {
            assertThat(r.role()).isEqualTo("SHIFT_MEMBER");
            assertThat(r.from()).isEqualTo(start.plusSeconds(60));
            assertThat(r.to()).isEqualTo(start.plusSeconds(3600));
        });
        assertThat(rows).anySatisfy(r -> {
            assertThat(r.role()).isEqualTo("SHIFT_LEAD");
            assertThat(r.to()).isEqualTo(start.plusSeconds(1800));
        });
        assertThat(statistics.assignmentHistory(scope, start.plusSeconds(7200), start.plusSeconds(28800)))
                .noneMatch(r -> r.staffId() == person);
        assertThat(statistics.assignmentHistory(
                        new ScopeFilter(false, Set.of(other), Set.of(), Set.of()), start, start.plusSeconds(28800)))
                .noneMatch(r -> r.staffId() == person);
    }

    private void audit(long account, long handover, long shift, String basis, Instant at) {
        jdbc.update(
                "INSERT INTO audit_log(occurred_at,actor_type,actor_id,role,action,entity_type,entity_id,details) VALUES (?,'USER',?,'CAREGIVER','HANDOVER_PREPARED','M09.HANDOVER',?,JSON_OBJECT('basis',?,'shiftId',?))",
                ShiftStore.sql(at),
                account,
                Long.toString(handover),
                basis,
                shift);
    }

    @Test
    void FR_056_nonRoleBasesReadsAllPagesAndRecordedBasis_withScopeAndTimeBoundaries() {
        long floor = TestUsers.nextId(), other = TestUsers.nextId();
        locations.floor(floor, null).floor(other, null);
        long id = shift("FLOOR", floor), otherShift = shift("FLOOR", other);
        var actor = new TestUsers(jdbc, time).staff(com.eldercare.backend.common.Role.CAREGIVER);
        long source = TestUsers.nextId();
        for (int i = 0; i < 205; i++) audit(actor.accountId(), source, id, "SHIFT_LEAD", start.plusSeconds(i));
        audit(actor.accountId(), source, id, "ROLE", start);
        audit(actor.accountId(), source, id, "FLOOR_MANAGER", start);
        audit(actor.accountId(), source, id, "FR-027", start);
        audit(actor.accountId(), source, otherShift, "SHIFT_LEAD", start);
        audit(actor.accountId(), source, id, "SHIFT_LEAD", start.plusSeconds(3600));
        int before = jdbc.queryForObject("SELECT COUNT(*) FROM audit_log", Integer.class);
        var rows = statistics
                .nonRoleBases(new ScopeFilter(false, Set.of(floor), Set.of(), Set.of()), start, start.plusSeconds(3600))
                .toList();
        assertThat(rows)
                .filteredOn(r -> r.targetId().equals(Long.toString(source)))
                .hasSize(205);
        assertThat(rows).allSatisfy(r -> {
            assertThat(r.staffId()).isEqualTo(actor.staffId());
            assertThat(r.basis()).isEqualTo("SHIFT_LEAD");
            assertThat(r.targetType()).isEqualTo("M09.HANDOVER");
        });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log", Integer.class))
                .isEqualTo(before);
        assertThatThrownBy(() -> statistics.nonRoleBases(ScopeFilter.FACILITY, start, start))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void FR_037_reportUsesLastCalculationStrictlyBeforeEnd_andRetainsEverBelowFlag() {
        long floor = TestUsers.nextId();
        locations.floor(floor, null);
        long id = shift("FLOOR", floor);
        record(id, start.plusSeconds(3600), false);
        record(id, start.plusSeconds(25200), true);
        record(id, start.plusSeconds(28800), false);
        record(id, start.plusSeconds(32400), false);
        var scope = new ScopeFilter(false, Set.of(floor), Set.of(), Set.of());
        var rows = statistics.staffingRatios(scope, day, day).toList();
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().ratio()).isEqualByComparingTo("7.00");
        assertThat(rows.getFirst().passed()).isTrue();
        assertThat(rows.getFirst().everBelowThreshold()).isTrue();
        assertThat(rows.getFirst().calculatedAt()).isEqualTo(start.plusSeconds(25200));
        assertThat(statistics.staffingRatios(
                        new ScopeFilter(false, Set.of(TestUsers.nextId()), Set.of(), Set.of()), day, day))
                .isEmpty();
    }

    @Test
    void FR_048_areaAggregateNeedsWholeAreaScope_notJustOneFloor() {
        long area = TestUsers.nextId(), a = TestUsers.nextId(), b = TestUsers.nextId();
        locations.floor(a, area).floor(b, area);
        long id = shift("AREA", area);
        record(id, start, false);
        assertThat(statistics.staffingRatios(new ScopeFilter(false, Set.of(a), Set.of(), Set.of()), day, day))
                .isEmpty();
        assertThat(statistics.staffingRatios(new ScopeFilter(false, Set.of(a, b), Set.of(), Set.of()), day, day))
                .hasSize(1);
        assertThat(statistics.staffingRatios(new ScopeFilter(false, Set.of(area), Set.of(), Set.of()), day, day))
                .hasSize(1);
    }

    @Test
    void FR_037a_assignmentHistoryHonorsActualEndAndRejectsEmptyPeriod() {
        long floor = TestUsers.nextId();
        locations.floor(floor, null);
        long id = shift("FLOOR", floor), staff = staff();
        long assignment = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO staff_assignment(shift_id,staff_id,target_type,target_id,role,assignment_kind,starts_at,ends_at,actual_end_at,status) VALUES (?,?,'FLOOR',?,'CAREGIVER','PRIMARY',?,?,?,'ENDED')",
                id,
                staff,
                floor,
                ShiftStore.sql(start),
                ShiftStore.sql(start.plusSeconds(28800)),
                ShiftStore.sql(start.plusSeconds(7200)));
        var rows = statistics
                .assignmentHistory(
                        new ScopeFilter(false, Set.of(floor), Set.of(), Set.of()), start, start.plusSeconds(28800))
                .toList();
        assertThat(rows).anySatisfy(r -> {
            assertThat(r.assignmentId()).isEqualTo(assignment);
            assertThat(r.to()).isEqualTo(start.plusSeconds(7200));
        });
        assertThat(statistics.assignmentHistory(
                        new ScopeFilter(false, Set.of(floor), Set.of(), Set.of()),
                        start.plusSeconds(7200),
                        start.plusSeconds(28800)))
                .isEmpty();
        assertThatThrownBy(() -> statistics.staffingRatios(ScopeFilter.FACILITY, day, day.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void FR_049_handoverReportIncludesLateConfirmationAndScope() {
        long floor = TestUsers.nextId();
        locations.floor(floor, null);
        long id = shift("FLOOR", floor), staff = staff();
        var deadline = start.plusSeconds(28800)
                .plus(parameters
                        .read(com.eldercare.backend.administration.parameter.api.ParameterCode.CFG_M09_05)
                        .asDuration());
        long handover = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO shift_handover(shift_id,handover_staff_id,status,created_at,finalized_at,confirmed_at) VALUES (?,?,'CONFIRMED',?,?,?)",
                id,
                staff,
                ShiftStore.sql(start),
                ShiftStore.sql(start.plusSeconds(28000)),
                ShiftStore.sql(deadline.plusSeconds(1)));
        var rows = statistics
                .handovers(new ScopeFilter(false, Set.of(floor), Set.of(), Set.of()), day, day)
                .toList();
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().handoverId()).isEqualTo(handover);
        assertThat(rows.getFirst().late()).isTrue();
        assertThat(statistics.handovers(
                        new ScopeFilter(false, Set.of(TestUsers.nextId()), Set.of(), Set.of()), day, day))
                .isEmpty();
    }

    @Test
    void FR_049_expiringCredentialsIncludesEndOfExpiryDate_butNotRevokedOrExpired() {
        long staff = staff();
        long type = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO credential_type(code,name,credential_group,has_expiry,required_for_roles) VALUES (?,?,'TRAINING',TRUE,JSON_ARRAY())",
                "STAT-" + staff,
                "[Loại thống kê " + staff + "]");
        long valid = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO credential(staff_id,credential_type_id,issue_date,expiry_date) VALUES (?,?,?,?)",
                staff,
                type,
                time.today().minusYears(1),
                time.today());
        AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO credential(staff_id,credential_type_id,issue_date,expiry_date,revoked_from,revocation_reason) VALUES (?,?,?,?,?,'Thu hồi kiểm thử')",
                staff,
                type,
                time.today().minusYears(1),
                time.today(),
                time.today());
        AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO credential(staff_id,credential_type_id,issue_date,expiry_date) VALUES (?,?,?,?)",
                staff,
                type,
                time.today().minusYears(1),
                time.today().minusDays(1));
        var rows = statistics
                .expiringCredentials(0)
                .filter(c -> c.staffId() == staff)
                .toList();
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().credentialId()).isEqualTo(valid);
    }
}
