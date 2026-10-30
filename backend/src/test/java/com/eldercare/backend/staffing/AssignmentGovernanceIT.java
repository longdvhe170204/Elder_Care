package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** T009/T019, FR-051: chỉ loại bỏ phân công đã kết thúc, giữ cha còn bản ghi tham chiếu. */
class AssignmentGovernanceIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    List<RetentionProvider> providers;

    @Autowired
    RetentionDatabase database;

    @Autowired
    List<com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider> scopes;

    private long staff, shift;

    private long insert(String sql, Object... args) {
        return AssignmentFixtures.insert(jdbc, sql, args);
    }

    private RetentionProvider provider(String type) {
        return providers.stream()
                .filter(p -> p.dataType().equals(type))
                .findFirst()
                .orElseThrow();
    }

    private void fixture() {
        fixture("CLOSED");
    }

    private void fixture(String status) {
        staff = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO staff_profile(id,full_name,position) VALUES (?,'[Nhân viên lưu giữ phân công]','Điều dưỡng')",
                staff);
        long template = insert(
                "INSERT INTO shift_template(name,start_time,end_time,scope_type) VALUES ('[Mẫu lưu giữ phân công]','07:00','15:00','FLOOR')");
        long floor = TestUsers.nextId();
        long schedule =
                insert("INSERT INTO shift_schedule(month,scope_type,scope_id) VALUES ('2010-01','FLOOR',?)", floor);
        shift = insert(
                "INSERT INTO work_shift(schedule_id,template_id,scope_type,scope_id,starts_at,ends_at,requires_handover,status) VALUES (?,?,'FLOOR',?,'2010-01-01','2010-01-01 08:00',FALSE,?)",
                schedule,
                template,
                floor,
                status);
    }

    private long assignment(String status, String actualEnd) {
        return insert(
                "INSERT INTO staff_assignment(shift_id,staff_id,target_type,target_id,role,assignment_kind,starts_at,ends_at,status,actual_end_at,created_by) VALUES (?,?,'FLOOR',?,'NURSING_STAFF','PRIMARY','2010-01-01','2010-01-01 08:00',?,?,1)",
                shift,
                staff,
                TestUsers.nextId(),
                status,
                actualEnd);
    }

    @Test
    void FR_034_assignmentAndFloorManagerRecordsHaveTheirOwnScopeProviders() {
        fixture();
        long assignment = assignment("DRAFT", null);
        long floor = jdbc.queryForObject("SELECT scope_id FROM work_shift WHERE id=?", Long.class, shift);
        var target = scopes.stream()
                .filter(s -> s.entityType().equals("M09.ASSIGNMENT"))
                .findFirst()
                .orElseThrow()
                .assignScope(assignment);
        assertThat(target.floorAreaId()).isEqualTo(floor);
        long manager = insert(
                "INSERT INTO floor_manager_assignment(staff_id,floor_area_id,starts_at,assigned_by) VALUES (?,?,'2010-01-01',1)",
                staff,
                floor);
        assertThat(scopes.stream()
                        .filter(s -> s.entityType().equals("M09.FLOOR_MANAGER_ASSIGNMENT"))
                        .findFirst()
                        .orElseThrow()
                        .assignScope(manager)
                        .floorAreaId())
                .isEqualTo(floor);
    }

    @Test
    void FR_051_terminalAssignmentsUseActualEndAndRetainTheirParentsUntilPurged() {
        fixture();
        long assignment = assignment("ENDED", "2010-01-01 04:00:00");
        var cutoff = Instant.parse("2010-01-01T06:00:00Z");
        assertThat(provider("M09.ASSIGNMENT").expiredCandidates(cutoff, 100))
                .anyMatch(c -> c.id() == assignment && c.at().equals(Instant.parse("2010-01-01T04:00:00Z")));
        var reference = new RetentionProvider.DataRef("M09.ASSIGNMENT", assignment);
        assertThat(provider("M09.STAFF").referencesTo(staff)).contains(reference);
        assertThat(provider("M09.SHIFT").referencesTo(shift)).contains(reference);
        database.transaction()
                .executeWithoutResult(s -> provider("M09.ASSIGNMENT").delete(List.of(assignment)));
        assertThat(provider("M09.SHIFT").referencesTo(shift)).doesNotContain(reference);
    }

    @Test
    void FR_051_oldDraftScheduledAndActiveAssignmentsAreNeverPurgeCandidates() {
        fixture();
        var ids = List.of(assignment("DRAFT", null), assignment("SCHEDULED", null), assignment("ACTIVE", null));
        assertThat(provider("M09.ASSIGNMENT").expiredCandidates(Instant.parse("2020-01-01T00:00:00Z"), 100))
                .noneMatch(c -> ids.contains(c.id()));
    }

    @Test
    void FR_051_finishedAssignmentIsKeptWhileItsShiftStillAwaitsHandover() {
        fixture("AWAITING_HANDOVER");
        long assignment = assignment("ENDED", "2010-01-01 04:00:00");
        assertThat(provider("M09.ASSIGNMENT").expiredCandidates(Instant.parse("2020-01-01T00:00:00Z"), 100))
                .noneMatch(c -> c.id() == assignment);
    }

    @Test
    void FR_051_floorManagerHistoryIsRetainedUntilEndedAndKeepsTheStaffReference() {
        fixture();
        long floor = TestUsers.nextId();
        long ended = insert(
                "INSERT INTO floor_manager_assignment(staff_id,floor_area_id,starts_at,ends_at,status,assigned_by) VALUES (?,?,'2010-01-01','2010-02-01','ENDED',1)",
                staff,
                floor);
        long active = insert(
                "INSERT INTO floor_manager_assignment(staff_id,floor_area_id,starts_at,ends_at,status,assigned_by) VALUES (?,?,'2010-01-01','2010-02-01','ACTIVE',1)",
                staff,
                floor);
        var p = provider("M09.FLOOR_MANAGER_ASSIGNMENT");
        assertThat(p.expiredCandidates(Instant.parse("2020-01-01T00:00:00Z"), 100))
                .anyMatch(c -> c.id() == ended)
                .noneMatch(c -> c.id() == active);
        assertThat(provider("M09.STAFF").referencesTo(staff))
                .contains(
                        new RetentionProvider.DataRef("M09.FLOOR_MANAGER_ASSIGNMENT", ended),
                        new RetentionProvider.DataRef("M09.FLOOR_MANAGER_ASSIGNMENT", active));
        database.transaction().executeWithoutResult(s -> p.delete(List.of(ended)));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM floor_manager_assignment WHERE id=?", Integer.class, ended))
                .isZero();
    }
}
