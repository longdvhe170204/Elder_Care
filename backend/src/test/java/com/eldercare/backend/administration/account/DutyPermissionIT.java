package com.eldercare.backend.administration.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.ScopeApi;
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
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeDuties;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import java.time.Duration;
import java.time.Instant;
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
