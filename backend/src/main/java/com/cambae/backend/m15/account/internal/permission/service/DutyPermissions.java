package com.cambae.backend.m15.account.internal.permission.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.PermissionKey;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.AssignmentKind;
import com.cambae.backend.m15.account.api.spi.AssignmentView;
import com.cambae.backend.m15.account.api.spi.Duty;
import com.cambae.backend.m15.account.api.spi.DutyApi;
import com.cambae.backend.m15.account.internal.scope.service.ActiveAssignments;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Quyền theo nhiệm vụ (002 T053; research A-06; FR-019 → FR-019g), bước 2 của {@code PermissionService}: bảng cố định,
 * không nơi nào khác thêm quyền theo nhiệm vụ. Nhiệm vụ hỏi 008 ({@code DutyApi}, phân công {@code SHIFT_LEAD}) tại lúc
 * kiểm; giới hạn của từng dòng là phạm vi của quyền đó nên bước phạm vi không chạy lại.
 *
 * <ul>
 *   <li>FR-019a: Người phụ trách ca, "Xử lý việc quá hạn" trên tầng/khu của ca, trong khoảng FR-032;
 *   <li>FR-019e: Người phụ trách ca, "Kiểm tra chất lượng" khi ca không có Trưởng tầng được giao (Q-254, Q-262);
 *   <li>FR-019f: Phụ trách bộ phận, tài sản, yêu cầu mua sắm (kiểm kê kho bỏ theo Q-392); đối tượng phải thuộc bộ
 *       phận của người đó, module sở hữu kiểm vì {@code Target} không mang bộ phận (13.1a, ³⁷);
 *   <li>FR-019g: Người phụ trách ca (mọi vai trò), "Xác nhận giường sẵn sàng" cho giường thuộc tầng/khu của ca (Q-313,
 *       ⁴⁰);
 *   <li>chú thích ¹⁵ (không phải ngoại lệ FR-019): ô T¹⁵ "Nhận phiếu, báo sai lệch" của Trưởng tầng, Điều dưỡng, Nhân
 *       viên chăm sóc chỉ có hiệu lực khi là Người nhận tại tầng của tầng và ca (BR-M08-11, Q-143).
 * </ul>
 *
 * FR-019b (đính chính, 000 {@code CorrectionMethodSelector}) và FR-019d (ghi nhận vắng ca, 008) là lệnh tự kiểm qua
 * {@code ScopeApi.shiftLead}; FR-019c (mở khóa sớm) ở {@code AccountService.earlyUnlock}.
 */
@Component
class DutyPermissions {

    private enum Limit {
        SHIFT_FLOOR,
        SHIFT_WITHOUT_FLOOR_MANAGER,
        DEPARTMENT,
        FLOOR_RECEIVER
    }

    private record DutyGrant(Duty duty, Limit limit, Set<PermissionKey> keys, Set<Role> roles) {}

    private static final List<DutyGrant> TABLE = List.of(
            new DutyGrant(
                    Duty.SHIFT_LEAD,
                    Limit.SHIFT_FLOOR,
                    keys(Function.OVERDUE_TASK_HANDLING, Operation.VIEW, Operation.CREATE, Operation.UPDATE),
                    Set.of()),
            new DutyGrant(
                    Duty.SHIFT_LEAD,
                    Limit.SHIFT_WITHOUT_FLOOR_MANAGER,
                    keys(Function.QUALITY_CHECK, Operation.VIEW, Operation.CREATE, Operation.UPDATE, Operation.CONFIRM),
                    Set.of()),
            new DutyGrant(
                    Duty.DEPARTMENT_LEAD,
                    Limit.DEPARTMENT,
                    Stream.of(
                                    keys(Function.ASSET_VEHICLE, Operation.VIEW, Operation.CREATE, Operation.UPDATE),
                                    keys(Function.PURCHASE_REQUEST, Operation.VIEW, Operation.CREATE, Operation.UPDATE))
                            .flatMap(Set::stream)
                            .collect(Collectors.toUnmodifiableSet()),
                    Set.of()),
            new DutyGrant(
                    Duty.SHIFT_LEAD,
                    Limit.SHIFT_FLOOR,
                    keys(Function.BED_READINESS, Operation.VIEW, Operation.CREATE, Operation.UPDATE),
                    Set.of()),
            new DutyGrant(
                    Duty.FLOOR_RECEIVER,
                    Limit.FLOOR_RECEIVER,
                    keys(Function.MEAL_RECEIPT, Operation.VIEW, Operation.CREATE, Operation.UPDATE, Operation.CONFIRM),
                    EnumSet.of(Role.FLOOR_MANAGER, Role.NURSING_STAFF, Role.CAREGIVER)));

    private final ActiveAssignments assignments;
    // ObjectProvider: 008 cài; trước đó chỉ test khai bản giả
    private final ObjectProvider<DutyApi> duties;

    DutyPermissions(ActiveAssignments assignments, ObjectProvider<DutyApi> duties) {
        this.assignments = assignments;
        this.duties = duties;
    }

    /** Nhiệm vụ cho phép khóa trên đối tượng tại {@code at}; rỗng khi không dòng nào của bảng thỏa. */
    Optional<Duty> grant(CurrentUser user, PermissionKey key, Target target, Instant at) {
        DutyApi api = duties.getIfAvailable();
        if (api == null || user.staffId().isEmpty()) {
            return Optional.empty();
        }
        long staffId = user.staffId().get();
        for (DutyGrant e : TABLE) {
            if (!e.keys().contains(key)
                    || (!e.roles().isEmpty() && e.roles().stream().noneMatch(user.roles()::contains))) {
                continue;
            }
            boolean holds = switch (e.limit()) {
                case SHIFT_FLOOR -> onShiftFloor(staffId, target, at, a -> a.kind() == AssignmentKind.SHIFT_LEAD);
                case SHIFT_WITHOUT_FLOOR_MANAGER ->
                    onShiftFloor(
                            staffId,
                            target,
                            at,
                            a -> a.kind() == AssignmentKind.SHIFT_LEAD && api.shiftHasNoFloorManager(a.shiftId()));
                case DEPARTMENT -> api.isDepartmentLead(staffId);
                case FLOOR_RECEIVER ->
                    onShiftFloor(
                            staffId,
                            target,
                            at,
                            a -> api.floorReceivers(a.floorAreaId(), a.shiftId())
                                    .contains(staffId));
            };
            if (holds) {
                return Optional.of(e.duty());
            }
        }
        return Optional.empty();
    }

    /** Một phân công gắn ca của nhân viên trên tầng/khu của đối tượng, trong khoảng FR-032, thỏa điều kiện thêm. */
    private boolean onShiftFloor(long staffId, Target target, Instant at, Predicate<AssignmentView> condition) {
        Optional<Long> floor = assignments.floorOf(target, at);
        return floor.isPresent()
                && assignments.at(staffId, at).stream()
                        .filter(a -> a.shiftId() != null && floor.get().equals(a.floorAreaId()))
                        .anyMatch(condition);
    }

    private static Set<PermissionKey> keys(Function function, Operation... operations) {
        return Stream.of(operations).map(o -> new PermissionKey(function, o)).collect(Collectors.toUnmodifiableSet());
    }
}
