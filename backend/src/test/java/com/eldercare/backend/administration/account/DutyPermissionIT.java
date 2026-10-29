package com.eldercare.backend.administration.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionBasis;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.administration.account.api.spi.Duty;
import com.eldercare.backend.administration.account.internal.auth.domain.ActiveSession;
import com.eldercare.backend.administration.correction.api.CreationMethod;
import com.eldercare.backend.administration.correction.internal.domain.CorrectionMethodSelector;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeDuties;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 002 T049 (US4 #10, #11; FR-019 → FR-019g, research A-06): bảy ngoại lệ quyền theo nhiệm vụ, mỗi ngoại lệ chỉ trong
 * giới hạn của nó (tầng/khu của ca, khoảng FR-032, bộ phận), căn cứ là nhiệm vụ để module chép vào nhật ký. FR-019c (mở
 * khóa sớm) kiểm ở {@code EarlyUnlockIT}. Kèm người phụ trách ca, trưởng tầng được giao (kể cả giao tạm), người nhận tại
 * tầng (chú thích ¹⁵) và người quyết định xem lại (FR-041c, CHK056).
 */
@Import(FakeM15Config.class)
class DutyPermissionIT extends MySqlIntegrationTestBase {

    private static final PermissionKey HANDLE_OVERDUE =
            new PermissionKey(Function.OVERDUE_TASK_HANDLING, Operation.UPDATE);
    private static final PermissionKey QUALITY_CHECK = new PermissionKey(Function.QUALITY_CHECK, Operation.CREATE);
    private static final PermissionKey BED_READY = new PermissionKey(Function.BED_READINESS, Operation.CREATE);
    private static final PermissionKey SCHEDULE_UPDATE = new PermissionKey(Function.SHIFT_SCHEDULE, Operation.UPDATE);
    private static final PermissionKey PURCHASE = new PermissionKey(Function.PURCHASE_REQUEST, Operation.CREATE);
    private static final PermissionKey MEAL_RECEIPT = new PermissionKey(Function.MEAL_RECEIPT, Operation.CONFIRM);

    @Autowired
    PermissionApi permissions;

    @Autowired
    ScopeApi scope;

    @Autowired
    AccountApi accounts;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    FakeDuties duties;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    AccountFixtures fixtures;
    long floor1;
    long floor2;
    Instant now;

    @BeforeEach
    void floors() {
        fixtures = new AccountFixtures(jdbc, time);
        floor1 = AccountFixtures.nextId();
        floor2 = AccountFixtures.nextId();
        now = time.now();
    }

    @Test
    void US4_10_FR_019a_shiftLead_handlesOverdueOnShiftFloorOnly_basisDuty() {
        ActiveSession nurse = shiftLead("AC-04", floor2, now.minus(Duration.ofHours(1)), now.plus(Duration.ofHours(7)));

        PermissionResult onShiftFloor = permissions.hasPermission(nurse, HANDLE_OVERDUE, floor(floor2));
        assertThat(onShiftFloor.allowed()).isTrue();
        assertThat(onShiftFloor.basis()).contains(PermissionBasis.duty(Duty.SHIFT_LEAD));
        assertThat(permissions
                        .hasPermission(nurse, HANDLE_OVERDUE, floor(floor1))
                        .allowed())
                .isFalse();
    }

    @Test
    void US4_10_FR_032_shiftEndedBeyondMargin_overdueDenied() {
        ActiveSession nurse =
                shiftLead("AC-04", floor2, now.minus(Duration.ofHours(11)), now.minus(Duration.ofHours(3)));

        assertThat(permissions
                        .hasPermission(nurse, HANDLE_OVERDUE, floor(floor2))
                        .allowed())
                .isFalse();
    }

    @Test
    void FR_019a_floorManagerRole_handlesOverdue_basisRole() {
        long account = fixtures.staffAccount("AC-02");
        assignments.assign(
                fixtures.staffId(account),
                new AssignmentView(
                        AssignmentKind.FLOOR_MANAGER, floor2, null, null, null, now.minus(Duration.ofDays(1)), null));

        PermissionResult result = permissions.hasPermission(fixtures.user(account), HANDLE_OVERDUE, floor(floor2));

        assertThat(result.allowed()).isTrue();
        assertThat(result.basis()).contains(PermissionBasis.ROLE);
    }

    @Test
    void US4_11_FR_019b_shiftLead_correctsFloorRecord_methodB_unassignedRecordNot() {
        ActiveSession nurse = shiftLead("AC-04", floor2, now.minus(Duration.ofHours(1)), now.plus(Duration.ofHours(7)));
        duties.shiftLead(floor2, nurse.staffId().orElseThrow());
        long recorder = fixtures.staffAccount("AC-05");
        CorrectionMethodSelector selector = new CorrectionMethodSelector(scope, accounts);

        assertThat(selector.select(
                        nurse, new CorrectionMethodSelector.Facts(recorder, Optional.of(floor2), false, now)))
                .contains(CreationMethod.B);
        assertThat(selector.select(nurse, new CorrectionMethodSelector.Facts(recorder, Optional.empty(), false, now)))
                .isEmpty();
    }

    @Test
    void FR_019d_shiftLead_noOtherScheduleCommand_absenceCheckedBy008ViaShiftLead() {
        ActiveSession nurse = shiftLead("AC-04", floor2, now.minus(Duration.ofHours(1)), now.plus(Duration.ofHours(7)));
        duties.shiftLead(floor2, nurse.staffId().orElseThrow());

        assertThat(permissions
                        .hasPermission(nurse, SCHEDULE_UPDATE, floor(floor2))
                        .allowed())
                .isFalse();
        assertThat(scope.shiftLead(floor2, now)).contains(nurse.accountId());
    }

    @Test
    void FR_019e_qualityCheck_onlyWhenShiftHasNoFloorManager() {
        long shiftWithout = AccountFixtures.nextId();
        long shiftWith = AccountFixtures.nextId();
        duties.shiftWithoutFloorManager(shiftWithout);
        ActiveSession withoutManager = shiftLead("AC-04", floor1, shiftWithout);
        ActiveSession withManager = shiftLead("AC-04", floor2, shiftWith);

        PermissionResult allowed = permissions.hasPermission(withoutManager, QUALITY_CHECK, floor(floor1));
        assertThat(allowed.allowed()).isTrue();
        assertThat(allowed.basis()).contains(PermissionBasis.duty(Duty.SHIFT_LEAD));
        assertThat(permissions
                        .hasPermission(withManager, QUALITY_CHECK, floor(floor2))
                        .allowed())
                .isFalse();
    }

    @Test
    void FR_019f_departmentLead_purchase_nonLeadDenied() {
        // Q-392: bỏ duyệt kiểm kê kho của Phụ trách bộ phận Bếp; Nhân viên bếp đã có quyền mua sắm theo vai trò (³⁷)
        long caregiverLead = fixtures.staffAccount("AC-05");
        long laundry = AccountFixtures.nextId();
        duties.departmentLead(laundry, fixtures.staffId(caregiverLead));
        assertThat(permissions
                        .hasPermission(fixtures.user(caregiverLead), PURCHASE, Target.UNSPECIFIED)
                        .basis())
                .contains(PermissionBasis.duty(Duty.DEPARTMENT_LEAD));
        assertThat(permissions
                        .hasPermission(fixtures.user(fixtures.staffAccount("AC-05")), PURCHASE, Target.UNSPECIFIED)
                        .allowed())
                .isFalse();
    }

    @Test
    void FR_019g_shiftLeadOfAnyRole_confirmsBedOnShiftFloor_otherCaregiverDenied() {
        ActiveSession lead = shiftLead("AC-05", floor2, now.minus(Duration.ofHours(1)), now.plus(Duration.ofHours(7)));
        ActiveSession member = fixtures.user(fixtures.staffAccount("AC-05"));
        assignments.assign(
                member.staffId().orElseThrow(),
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        floor2,
                        null,
                        null,
                        AccountFixtures.nextId(),
                        now.minus(Duration.ofHours(1)),
                        now.plus(Duration.ofHours(7))));
        Target bed = new Target("M03.BED", String.valueOf(AccountFixtures.nextId()), null, floor2);

        assertThat(permissions.hasPermission(lead, BED_READY, bed).basis())
                .contains(PermissionBasis.duty(Duty.SHIFT_LEAD));
        assertThat(permissions.hasPermission(lead, BED_READY, floor(floor1)).allowed())
                .isFalse();
        assertThat(permissions.hasPermission(member, BED_READY, bed).failedLayer())
                .contains(FailedLayer.ROLE);
    }

    @Test
    void footnote15_floorReceiver_confirmsMealReceipt_onlyForFloorAndShift() {
        long shift = AccountFixtures.nextId();
        long account = fixtures.staffAccount("AC-05");
        long staffId = fixtures.staffId(account);
        assignments.assign(
                staffId,
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        floor1,
                        null,
                        null,
                        shift,
                        now.minus(Duration.ofHours(1)),
                        now.plus(Duration.ofHours(7))));
        duties.floorReceivers(floor1, shift, List.of(staffId));
        ActiveSession user = fixtures.user(account);

        assertThat(permissions.hasPermission(user, MEAL_RECEIPT, floor(floor1)).basis())
                .contains(PermissionBasis.duty(Duty.FLOOR_RECEIVER));
        assertThat(permissions.hasPermission(user, MEAL_RECEIPT, floor(floor2)).allowed())
                .isFalse();
    }

    @Test
    void temporaryFloorManager_isAssignedManager_andReviewDecider_thenSubstitute_thenManager() {
        long temporary = fixtures.staffAccount("AC-02");
        duties.floorManager(floor1, fixtures.staffId(temporary));
        long substitute = fixtures.staffAccount("AC-04");
        duties.floorManagerSubstitute(floor2, fixtures.staffId(substitute));
        long manager = fixtures.staffAccount("AC-01");

        assertThat(scope.assignedFloorManager(floor1)).contains(temporary);
        assertThat(scope.reviewDecider(Optional.of(floor1)).accountIds()).containsExactly(temporary);
        assertThat(scope.reviewDecider(Optional.of(floor2)).accountIds()).containsExactly(substitute);
        assertThat(scope.reviewDecider(Optional.empty()).accountIds()).contains(manager);
    }

    /** Người phụ trách ca của tầng trong khoảng giờ ca đã cho. */
    private ActiveSession shiftLead(String role, long floor, Instant from, Instant to) {
        long account = fixtures.staffAccount(role);
        assignments.assign(
                fixtures.staffId(account),
                new AssignmentView(AssignmentKind.SHIFT_LEAD, floor, null, null, AccountFixtures.nextId(), from, to));
        return fixtures.user(account);
    }

    /** Người phụ trách ca đang diễn ra của ca đã cho. */
    private ActiveSession shiftLead(String role, long floor, long shift) {
        long account = fixtures.staffAccount(role);
        assignments.assign(
                fixtures.staffId(account),
                new AssignmentView(
                        AssignmentKind.SHIFT_LEAD,
                        floor,
                        null,
                        null,
                        shift,
                        now.minus(Duration.ofHours(1)),
                        now.plus(Duration.ofHours(7))));
        return fixtures.user(account);
    }

    private static Target floor(long floorAreaId) {
        return new Target("M04.CARE_TASK", String.valueOf(AccountFixtures.nextId()), null, floorAreaId);
    }
}
