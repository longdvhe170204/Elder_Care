package com.eldercare.backend.administration.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.BedAllocationView;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeBedAllocations;
import com.eldercare.backend.support.fake.administration.FakeDuties;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import java.time.Instant;
import java.util.List;
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

    @Autowired
    FakeDuties duties;

    @Test
    void FR_031_groupLeaderAndCompanionsSeeTheirTrip_onAnotherFloor_othersDoNot() {
        long trip = AccountFixtures.nextId();
        long tripFloor = AccountFixtures.nextId();
        long leaderAccount = fixtures.staffAccount("AC-05");
        long companionAccount = fixtures.staffAccount("AC-04");
        long otherAccount = fixtures.staffAccount("AC-05");
        duties.trip(trip, fixtures.staffId(leaderAccount), List.of(fixtures.staffId(companionAccount)));
        var target = new Target("M04.TRIP", String.valueOf(trip), null, tripFloor);

        assertThat(scope.inScope(fixtures.user(leaderAccount), target)).isTrue();
        assertThat(scope.inScope(fixtures.user(companionAccount), target)).isTrue();
        assertThat(scope.inScope(fixtures.user(otherAccount), target)).isFalse();

        // 002 T049 (FR-031, Q-161): người tham gia chuyến đang diễn ra thuộc phạm vi trưởng đoàn, người đi cùng
        long participant = AccountFixtures.nextId();
        duties.tripResidents(fixtures.staffId(leaderAccount), participant)
                .tripResidents(fixtures.staffId(companionAccount), participant);
        var onTrip = new Target("M01.RESIDENT", String.valueOf(participant), participant, null);
        assertThat(scope.inScope(fixtures.user(leaderAccount), onTrip)).isTrue();
        assertThat(scope.inScope(fixtures.user(companionAccount), onTrip)).isTrue();
        assertThat(scope.inScope(fixtures.user(otherAccount), onTrip)).isFalse();
        assertThat(scope.filter(fixtures.user(leaderAccount), VIEW_PROFILE).residentIds())
                .contains(participant);
    }
}
