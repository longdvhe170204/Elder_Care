package com.cambae.backend.m15.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.AssignmentKind;
import com.cambae.backend.m15.account.api.AssignmentView;
import com.cambae.backend.m15.account.api.BedAllocationView;
import com.cambae.backend.m15.account.api.Function;
import com.cambae.backend.m15.account.api.Operation;
import com.cambae.backend.m15.account.api.PermissionKey;
import com.cambae.backend.m15.account.api.ScopeApi;
import com.cambae.backend.m15.account.api.ScopeFilter;
import com.cambae.backend.m15.account.api.Target;
import com.cambae.backend.m15.account.auth.internal.ActiveSession;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.fake.m15.FakeAssignments;
import com.cambae.backend.support.fake.m15.FakeBedAllocations;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 002 T048 (US4 Independent Test, #1, #4, #5, #7, #16; FR-029, FR-030, FR-033, FR-035): phạm vi tính lại ở mỗi lần
 * hỏi, đổi phân công hay chuyển giường có hiệu lực ngay ở thao tác kế tiếp; bộ lọc danh sách theo tầng, phòng.
 */
@Import(FakeM15Config.class)
class ScopeAssignmentChangeIT extends MySqlIntegrationTestBase {

    private static final PermissionKey VIEW_PROFILE = new PermissionKey(Function.RESIDENT_PROFILE, Operation.VIEW);

    @Autowired
    ScopeApi scope;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    FakeBedAllocations bedAllocations;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    AccountFixtures fixtures;
    long floor1;
    long floor2;
    long room101;
    long room102;
    long room201;
    long residentA;
    long residentB;
    long residentC;

    @BeforeEach
    void residents() {
        fixtures = new AccountFixtures(jdbc, time);
        floor1 = AccountFixtures.nextId();
        floor2 = AccountFixtures.nextId();
        room101 = AccountFixtures.nextId();
        room102 = AccountFixtures.nextId();
        room201 = AccountFixtures.nextId();
        residentA = place(room101, floor1);
        residentB = place(room201, floor2);
        residentC = place(room102, floor1);
    }

    @Test
    void US4_4_floorAssignmentChanged_nextCheckUsesNewFloor() {
        long account = fixtures.staffAccount("AC-05");
        ActiveSession user = fixtures.user(account);
        assign(account, floor1, null, null);
        assertThat(scope.inScope(user, resident(residentA))).isTrue();
        assertThat(scope.inScope(user, resident(residentB))).isFalse();

        assignments.unassign(fixtures.staffId(account));
        assign(account, floor2, null, null);

        assertThat(scope.inScope(user, resident(residentA))).isFalse();
        assertThat(scope.inScope(user, resident(residentB))).isTrue();
    }

    @Test
    void US4_5_residentMovedToFloor2_scopeFollowsResident() {
        long onFloor1 = fixtures.staffAccount("AC-05");
        long onFloor2 = fixtures.staffAccount("AC-05");
        assign(onFloor1, floor1, null, null);
        assign(onFloor2, floor2, null, null);

        bedAllocations.allocate(
                residentA, new BedAllocationView(AccountFixtures.nextId(), AccountFixtures.nextId(), room201, floor2));

        assertThat(scope.inScope(fixtures.user(onFloor1), resident(residentA))).isFalse();
        assertThat(scope.inScope(fixtures.user(onFloor2), resident(residentA))).isTrue();
    }

    @Test
    void US4_1_floorFilter_listsResidentsWithOpenAllocationOnFloor() {
        long account = fixtures.staffAccount("AC-05");
        assign(account, floor1, null, null);

        ScopeFilter filter = scope.filter(fixtures.user(account), VIEW_PROFILE);

        assertThat(filter.facilityWide()).isFalse();
        assertThat(filter.floorAreaIds()).containsExactly(floor1);
        assertThat(filter.residentIds()).contains(residentA, residentC).doesNotContain(residentB);
    }

    @Test
    void US4_16_roomAssignment_onlyResidentsOfThatRoom() {
        long account = fixtures.staffAccount("AC-05");
        assign(account, null, room101, null);
        ActiveSession user = fixtures.user(account);

        assertThat(scope.inScope(user, resident(residentA))).isTrue();
        assertThat(scope.inScope(user, resident(residentC))).isFalse();
        ScopeFilter filter = scope.filter(user, VIEW_PROFILE);
        assertThat(filter.roomIds()).containsExactly(room101);
        assertThat(filter.residentIds()).contains(residentA).doesNotContain(residentC);
    }

    @Test
    void US4_7_noAssignment_emptyFilter() {
        ActiveSession user = fixtures.user(fixtures.staffAccount("AC-05"));

        ScopeFilter filter = scope.filter(user, VIEW_PROFILE);

        assertThat(filter.facilityWide()).isFalse();
        assertThat(filter.floorAreaIds()).isEmpty();
        assertThat(filter.roomIds()).isEmpty();
        assertThat(filter.residentIds()).isEmpty();
    }

    @Test
    void FR_034_facilityWideRole_facilityFilter() {
        ActiveSession office = fixtures.user(fixtures.staffAccount("AC-09"));

        assertThat(scope.filter(office, VIEW_PROFILE)).isEqualTo(ScopeFilter.FACILITY);
    }

    private long place(long room, long floor) {
        long resident = AccountFixtures.nextId();
        bedAllocations.allocate(
                resident, new BedAllocationView(AccountFixtures.nextId(), AccountFixtures.nextId(), room, floor));
        return resident;
    }

    private void assign(long account, Long floor, Long room, Long resident) {
        assignments.assign(
                fixtures.staffId(account),
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        floor,
                        room,
                        resident,
                        null,
                        time.now().minus(Duration.ofDays(1)),
                        null));
    }

    private static Target resident(long id) {
        return new Target("M01.RESIDENT", String.valueOf(id), id, null);
    }
}
