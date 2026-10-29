package com.eldercare.backend.administration.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
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
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 002 T048 (US4 #8, #9; FR-032, BR-M15-02): phân công gắn ca góp vào phạm vi trong {@code [bắt đầu ca − CFG-M15-07, kết
 * thúc ca + CFG-M15-07]} (mặc định 2 giờ); phân công không gắn ca chỉ trong thời gian hiệu lực; kiểm tại thời điểm thiết
 * bị dùng phân công tại thời điểm đó (FR-041a).
 */
@Import(FakeM15Config.class)
class ScopeByShiftIT extends MySqlIntegrationTestBase {

    private static final PermissionKey VIEW_PROFILE = new PermissionKey(Function.RESIDENT_PROFILE, Operation.VIEW);

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
    long residentA;
    Instant now;

    @BeforeEach
    void residentOnFloor() {
        fixtures = new AccountFixtures(jdbc, time);
        residentA = AccountFixtures.nextId();
        bedAllocations.allocate(
                residentA,
                new BedAllocationView(AccountFixtures.nextId(), AccountFixtures.nextId(), AccountFixtures.nextId(), 1));
        now = time.now();
    }

    @Test
    void US4_8_shiftEnded90MinutesAgo_inScope_150MinutesAgo_denied() {
        ActiveSession late = caregiverWithShift(now.minus(Duration.ofHours(9)), now.minus(Duration.ofMinutes(90)));
        ActiveSession tooLate = caregiverWithShift(now.minus(Duration.ofHours(10)), now.minus(Duration.ofMinutes(150)));

        assertThat(scope.inScope(late, resident())).isTrue();
        assertThat(permissions.hasPermission(late, VIEW_PROFILE, resident()).allowed())
                .isTrue();
        assertThat(scope.inScope(tooLate, resident())).isFalse();
        assertThat(permissions.hasPermission(tooLate, VIEW_PROFILE, resident()).failedLayer())
                .contains(FailedLayer.SCOPE);
    }

    @Test
    void US4_9_shiftStartsIn90Minutes_inScope_in150Minutes_denied() {
        ActiveSession early = caregiverWithShift(now.plus(Duration.ofMinutes(90)), now.plus(Duration.ofHours(9)));
        ActiveSession tooEarly = caregiverWithShift(now.plus(Duration.ofMinutes(150)), now.plus(Duration.ofHours(10)));

        assertThat(scope.inScope(early, resident())).isTrue();
        assertThat(scope.inScope(tooEarly, resident())).isFalse();
    }

    @Test
    void FR_032_assignmentWithoutShift_noMargin_afterItEnds() {
        long account = fixtures.staffAccount("AC-02");
        long staffId = fixtures.staffId(account);
        assignments.assign(
                staffId,
                new AssignmentView(
                        AssignmentKind.FLOOR_MANAGER,
                        1L,
                        null,
                        null,
                        null,
                        now.minus(Duration.ofDays(3)),
                        now.minus(Duration.ofMinutes(5))));

        assertThat(scope.inScope(fixtures.user(account), resident())).isFalse();
    }

    @Test
    void FR_041a_permissionAtDeviceTime_usesAssignmentAtThatTime() {
        ActiveSession user = caregiverWithShift(now.minus(Duration.ofHours(14)), now.minus(Duration.ofHours(6)));
        Instant deviceTime = now.minus(Duration.ofHours(7));

        assertThat(permissions
                        .hasPermissionAt(user, VIEW_PROFILE, resident(), deviceTime)
                        .allowed())
                .isTrue();
        assertThat(permissions.hasPermission(user, VIEW_PROFILE, resident()).allowed())
                .isFalse();
    }

    /** Nhân viên chăm sóc có một ca trên tầng 1 trong khoảng đã cho. */
    private ActiveSession caregiverWithShift(Instant from, Instant to) {
        long account = fixtures.staffAccount("AC-05");
        assignments.assign(
                fixtures.staffId(account),
                new AssignmentView(AssignmentKind.SHIFT_MEMBER, 1L, null, null, AccountFixtures.nextId(), from, to));
        return fixtures.user(account);
    }

    private Target resident() {
        return new Target("M01.RESIDENT", String.valueOf(residentA), residentA, null);
    }
}
