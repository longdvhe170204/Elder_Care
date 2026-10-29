package com.eldercare.backend.administration.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
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
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 002 T021 (US1 #7, #12, #13; FR-035, FR-035a; Q-192, Q-195, Q-196): danh sách, dashboard theo phạm vi hiện hành; báo
 * cáo khoảng đã qua theo lịch sử phân công ({@code historyFilter}): ca của điều dưỡng ± CFG-M15-07 (mặc định 2 giờ) dù
 * phân công đã hết, trưởng tầng theo tầng đang giao lúc xem, người phụ trách ca theo tầng của ca mình phụ trách. Bộ lọc
 * lịch sử chỉ cho xem: mở hồ sơ hiện tại, sửa bản ghi vẫn theo phạm vi hiện hành. Khoảng thời gian của từng ca do 016
 * lọc tiếp từ lịch sử ca của 008 (016 research O-03); 002 trả phạm vi thô.
 */
@Import(FakeM15Config.class)
class HistoryReportScopeIT extends MySqlIntegrationTestBase {

    private static final PermissionKey VIEW_PROFILE = new PermissionKey(Function.RESIDENT_PROFILE, Operation.VIEW);
    private static final PermissionKey VIEW_DOSES =
            new PermissionKey(Function.MEDICATION_ADMINISTRATION, Operation.VIEW);
    private static final PermissionKey UPDATE_DOSE =
            new PermissionKey(Function.MEDICATION_ADMINISTRATION, Operation.UPDATE);
    private static final PermissionKey DASHBOARD = new PermissionKey(Function.DASHBOARD_REPORT, Operation.VIEW);

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
    Instant now;
    Instant from;
    Instant to;

    @BeforeEach
    void floors() {
        fixtures = new AccountFixtures(jdbc, time);
        floor1 = AccountFixtures.nextId();
        floor2 = AccountFixtures.nextId();
        floor3 = AccountFixtures.nextId();
        now = time.now();
        from = now.minus(Duration.ofDays(40));
        to = now.minus(Duration.ofDays(10));
    }

    @Test
    void US1_13_nurseReportPastMonth_floorsOfHerShiftsOnly_evenAfterAssignmentEnded() {
        long nurse = fixtures.staffAccount("AC-04");
        shift(nurse, AssignmentKind.SHIFT_MEMBER, floor1, from.plus(Duration.ofDays(2)));
        shift(nurse, AssignmentKind.SHIFT_MEMBER, floor1, from.plus(Duration.ofDays(3)));
        shift(nurse, AssignmentKind.SHIFT_MEMBER, floor3, now.minus(Duration.ofDays(2)));
        ActiveSession user = fixtures.user(nurse);

        ScopeFilter history = scope.historyFilter(user, VIEW_DOSES, from, to);

        assertThat(history.facilityWide()).isFalse();
        assertThat(history.floorAreaIds()).containsExactly(floor1);
        assertThat(scope.filter(user, VIEW_DOSES).floorAreaIds()).isEmpty();
    }

    @Test
    void US1_13_shiftEndingBeforeReport_countsWithinMarginOnly() {
        long nurse = fixtures.staffAccount("AC-04");
        assign(
                nurse,
                AssignmentKind.SHIFT_MEMBER,
                floor1,
                from.minus(Duration.ofHours(9)),
                from.minus(Duration.ofMinutes(90)));
        assign(
                nurse,
                AssignmentKind.SHIFT_MEMBER,
                floor2,
                from.minus(Duration.ofHours(11)),
                from.minus(Duration.ofHours(3)));

        assertThat(scope.historyFilter(fixtures.user(nurse), VIEW_DOSES, from, to)
                        .floorAreaIds())
                .containsExactly(floor1);
    }

    @Test
    void US1_13_residentMovedToFloor2_reportDoesNotOpenProfileNorAllowCorrection() {
        long nurse = fixtures.staffAccount("AC-04");
        shift(nurse, AssignmentKind.SHIFT_MEMBER, floor1, from.plus(Duration.ofDays(2)));
        long moved = place(floor2);
        ActiveSession user = fixtures.user(nurse);

        assertThat(scope.historyFilter(user, VIEW_DOSES, from, to).floorAreaIds())
                .contains(floor1);
        assertThat(permissions
                        .hasPermission(user, VIEW_PROFILE, resident(moved))
                        .failedLayer())
                .contains(FailedLayer.SCOPE);
        assertThat(permissions.hasPermission(user, UPDATE_DOSE, resident(moved)).allowed())
                .isFalse();
    }

    @Test
    void FR_035a_floorManager_currentFloorsIncludingRecordsBeforeAssignment_notPastFloors() {
        long manager = fixtures.staffAccount("AC-02");
        assign(manager, AssignmentKind.FLOOR_MANAGER, floor1, now.minus(Duration.ofDays(5)), null);
        assign(manager, AssignmentKind.FLOOR_MANAGER, floor2, from, to.minus(Duration.ofDays(2)));

        assertThat(scope.historyFilter(fixtures.user(manager), DASHBOARD, from, to)
                        .floorAreaIds())
                .containsExactly(floor1);
    }

    @Test
    void FR_035a_shiftLead_wholeFloorOfShiftsLed() {
        long nurse = fixtures.staffAccount("AC-04");
        shift(nurse, AssignmentKind.SHIFT_LEAD, floor2, from.plus(Duration.ofDays(5)));

        assertThat(scope.historyFilter(fixtures.user(nurse), VIEW_DOSES, from, to)
                        .floorAreaIds())
                .containsExactly(floor2);
    }

    @Test
    void US1_7_listOfPUser_onlyResidentsInScope() {
        long onFloor1 = place(floor1);
        long onFloor2 = place(floor2);
        long caregiver = fixtures.staffAccount("AC-05");
        assign(caregiver, AssignmentKind.SHIFT_MEMBER, floor1, now.minus(Duration.ofDays(1)), null);

        ScopeFilter list = scope.filter(fixtures.user(caregiver), VIEW_PROFILE);

        assertThat(list.facilityWide()).isFalse();
        assertThat(list.residentIds()).contains(onFloor1).doesNotContain(onFloor2);
    }

    @Test
    void US1_12_dashboard_officeAndDoctorFacility_floorManagerOwnFloor() {
        long office = fixtures.staffAccount("AC-09");
        long doctor = fixtures.staffAccount("AC-03");
        long manager = fixtures.staffAccount("AC-02");
        assign(manager, AssignmentKind.FLOOR_MANAGER, floor1, now.minus(Duration.ofDays(1)), null);

        assertThat(scope.filter(fixtures.user(office), DASHBOARD).facilityWide())
                .isTrue();
        assertThat(scope.filter(fixtures.user(doctor), DASHBOARD).facilityWide())
                .isTrue();
        ScopeFilter floorManager = scope.filter(fixtures.user(manager), DASHBOARD);
        assertThat(floorManager.facilityWide()).isFalse();
        assertThat(floorManager.floorAreaIds()).containsExactly(floor1);
    }

    /** Một ca 8 giờ bắt đầu lúc {@code start}. */
    private void shift(long account, AssignmentKind kind, long floor, Instant start) {
        assign(account, kind, floor, start, start.plus(Duration.ofHours(8)));
    }

    private void assign(long account, AssignmentKind kind, long floor, Instant start, Instant end) {
        Long shiftId = kind == AssignmentKind.FLOOR_MANAGER ? null : AccountFixtures.nextId();
        assignments.assign(fixtures.staffId(account), new AssignmentView(kind, floor, null, null, shiftId, start, end));
    }

    private long place(long floor) {
        long resident = AccountFixtures.nextId();
        bedAllocations.allocate(
                resident,
                new BedAllocationView(
                        AccountFixtures.nextId(), AccountFixtures.nextId(), AccountFixtures.nextId(), floor));
        return resident;
    }

    private static Target resident(long id) {
        return new Target("M01.RESIDENT", String.valueOf(id), id, null);
    }
}
