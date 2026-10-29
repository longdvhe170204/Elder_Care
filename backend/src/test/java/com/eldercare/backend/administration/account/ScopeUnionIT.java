package com.eldercare.backend.administration.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.administration.account.api.spi.BedAllocationView;
import com.eldercare.backend.administration.account.internal.auth.domain.ActiveSession;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeBedAllocations;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 002 T048 (US4 #3, #6, #15; BR-M15-02, FR-031, FR-034): nhiều phân công thì phạm vi là hợp; phân công theo người cao
 * tuổi chỉ đúng người đó; nhiều vai trò thì phạm vi lấy theo vai trò đem lại quyền (toàn viện thắng).
 */
@Import(FakeM15Config.class)
class ScopeUnionIT extends MySqlIntegrationTestBase {

    private static final PermissionKey VIEW_PROFILE = new PermissionKey(Function.RESIDENT_PROFILE, Operation.VIEW);
    private static final PermissionKey RECORD_ABSENCE = new PermissionKey(Function.ABSENCE_RETURN, Operation.CREATE);

    @Autowired
    ScopeApi scope;

    @Autowired
    PermissionApi permissions;

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
    long floor3;
    long room101;

    @BeforeEach
    void floors() {
        fixtures = new AccountFixtures(jdbc, time);
        floor1 = AccountFixtures.nextId();
        floor2 = AccountFixtures.nextId();
        floor3 = AccountFixtures.nextId();
        room101 = AccountFixtures.nextId();
    }

    @Test
    void US4_6_room101AndFloor2_unionOfBoth() {
        long inRoom = place(room101, floor1);
        long otherRoom = place(AccountFixtures.nextId(), floor1);
        long onFloor2 = place(AccountFixtures.nextId(), floor2);
        long account = fixtures.staffAccount("AC-05");
        assign(account, null, room101, null);
        assign(account, floor2, null, null);
        ActiveSession user = fixtures.user(account);

        assertThat(scope.inScope(user, resident(inRoom))).isTrue();
        assertThat(scope.inScope(user, resident(onFloor2))).isTrue();
        assertThat(scope.inScope(user, resident(otherRoom))).isFalse();
        ScopeFilter filter = scope.filter(user, VIEW_PROFILE);
        assertThat(filter.residentIds()).contains(inRoom, onFloor2).doesNotContain(otherRoom);
    }

    @Test
    void US4_3_threeResidentAssignments_exactlyThoseThree() {
        long a = place(room101, floor1);
        long b = place(room101, floor1);
        long c = place(room101, floor1);
        long roommate = place(room101, floor1);
        long account = fixtures.staffAccount("AC-05");
        assign(account, null, null, a);
        assign(account, null, null, b);
        assign(account, null, null, c);
        ActiveSession user = fixtures.user(account);

        assertThat(scope.inScope(user, resident(roommate))).isFalse();
        ScopeFilter filter = scope.filter(user, VIEW_PROFILE);
        assertThat(filter.residentIds()).containsExactlyInAnyOrder(a, b, c);
        assertThat(filter.roomIds()).isEmpty();
        assertThat(filter.floorAreaIds()).isEmpty();
    }

    @Test
    void US4_15_officeAndNurse_officeRoleGivesFacilityScope() {
        long onFloor3 = place(AccountFixtures.nextId(), floor3);
        long account = fixtures.staffAccount("AC-09", "AC-04");
        assign(account, floor1, null, null);

        assertThat(permissions
                        .hasPermission(fixtures.user(account), RECORD_ABSENCE, resident(onFloor3))
                        .allowed())
                .isTrue();
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
