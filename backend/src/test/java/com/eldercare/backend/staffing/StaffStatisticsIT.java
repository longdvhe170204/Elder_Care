package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.staffing.api.StaffStatisticsApi;
import com.eldercare.backend.staffing.internal.ratio.domain.StaffingRatioCalculator.Result;
import com.eldercare.backend.staffing.internal.ratio.persistence.StaffingRatioStore;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
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
@org.springframework.transaction.annotation.Transactional
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
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) SELECT 'M09.SHIFT_TEMPLATE','M09',src.name,'ACTIVE',JSON_OBJECT('createdAt',DATE_FORMAT(UTC_TIMESTAMP(6),'%Y-%m-%dT%H:%i:%s.%fZ'),'startTime',TIME_FORMAT(src.start_time,'%H:%i:%s'),'endTime',TIME_FORMAT(src.end_time,'%H:%i:%s'),'scopeType',src.scope_type,'requiresHandover',IF(src.requires_handover,CAST('true' AS JSON),CAST('false' AS JSON)),'originalName',src.name) FROM (SELECT '[Mẫu thống kê] #"
                        + com.eldercare.backend.support.TestUsers.nextId()
                        + "' AS name,'07:00' AS start_time,'15:00' AS end_time,? AS scope_type,FALSE AS requires_handover) src",
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
                "INSERT INTO staff_assignment(kind,staff_id,floor_area_id,starts_at,ends_at,status,assigned_by,reason) SELECT 'FLOOR_MANAGER',src.staff_id,src.floor_area_id,src.starts_at,src.ends_at,src.status,1,'[Khởi tạo fixture]' FROM (SELECT ? AS staff_id,? AS floor_area_id,? AS starts_at,? AS ends_at,'ENDED' AS status) src",
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
                "INSERT INTO status_history(entity_type,entity_id,attribute,module,kind,value_id,starts_at,ends_at,command,by_system) SELECT 'M09.SHIFT',src.shift_id,'LEAD_STAFF','M09','PERIOD',src.staff_id,src.starts_at,src.ends_at,src.basis,FALSE FROM (SELECT ? AS shift_id,? AS staff_id,? AS starts_at,? AS ends_at,'SCHEDULED' AS basis) src",
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
                        .read(com.eldercare.backend.administration.parameter.api.dto.ParameterCode.CFG_M09_05)
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
                "INSERT INTO catalog_item(catalog_type,module,code,name,status,attributes) SELECT 'M09.CREDENTIAL_TYPE','M09',src.code,src.name,'ACTIVE',JSON_OBJECT('createdAt',DATE_FORMAT(UTC_TIMESTAMP(6),'%Y-%m-%dT%H:%i:%s.%fZ'),'credentialGroup',src.credential_group,'hasExpiry',IF(src.has_expiry,CAST('true' AS JSON),CAST('false' AS JSON)),'requiredForRoles',CAST(src.required_for_roles AS JSON),'hasPracticeScopes',IF(FALSE,CAST('true' AS JSON),CAST('false' AS JSON)),'legacyCode',src.code) FROM (SELECT ? AS code,? AS name,'TRAINING' AS credential_group,TRUE AS has_expiry,JSON_ARRAY() AS required_for_roles) src",
                "STAT-" + staff,
                "[Loại thống kê " + staff + "]");
        long valid = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO credential(staff_id,credential_type_id,issue_date,expiry_date) SELECT src.staff_id,(SELECT c.id FROM catalog_item c WHERE c.catalog_type='M09.CREDENTIAL_TYPE' AND COALESCE(c.m09_legacy_id,c.id)=src.credential_type_id),src.issue_date,src.expiry_date FROM (SELECT ? AS staff_id,? AS credential_type_id,? AS issue_date,? AS expiry_date) src",
                staff,
                type,
                time.today().minusYears(1),
                time.today());
        AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO credential(staff_id,credential_type_id,issue_date,expiry_date,revoked_from,revocation_reason) SELECT src.staff_id,(SELECT c.id FROM catalog_item c WHERE c.catalog_type='M09.CREDENTIAL_TYPE' AND COALESCE(c.m09_legacy_id,c.id)=src.credential_type_id),src.issue_date,src.expiry_date,src.revoked_from,src.revocation_reason FROM (SELECT ? AS staff_id,? AS credential_type_id,? AS issue_date,? AS expiry_date,? AS revoked_from,'Thu hồi kiểm thử' AS revocation_reason) src",
                staff,
                type,
                time.today().minusYears(1),
                time.today(),
                time.today());
        AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO credential(staff_id,credential_type_id,issue_date,expiry_date) SELECT src.staff_id,(SELECT c.id FROM catalog_item c WHERE c.catalog_type='M09.CREDENTIAL_TYPE' AND COALESCE(c.m09_legacy_id,c.id)=src.credential_type_id),src.issue_date,src.expiry_date FROM (SELECT ? AS staff_id,? AS credential_type_id,? AS issue_date,? AS expiry_date) src",
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
