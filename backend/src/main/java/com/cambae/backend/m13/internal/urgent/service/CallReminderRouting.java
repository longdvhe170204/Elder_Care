package com.cambae.backend.m13.internal.urgent.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m13.internal.urgent.domain.CallReminderTarget;
import com.cambae.backend.m13.internal.urgent.domain.CallReminderTarget.Candidate;
import com.cambae.backend.m13.internal.urgent.domain.CallReminderTarget.Target;
import com.cambae.backend.m13.internal.urgent.domain.CallReminderTarget.Tier;
import com.cambae.backend.m15.account.api.AccountApi;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.dto.AccountStatus;
import com.cambae.backend.m15.account.api.dto.AccountView;
import com.cambae.backend.m15.account.api.spi.DutyApi;
import com.cambae.backend.m15.account.api.spi.StaffProfileApi;
import com.cambae.backend.m15.account.api.spi.StaffStatus;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Người được nhắc gọi và người có quyền gọi của một tầng tại thời điểm nhắc (FR-029, FR-030a). Người phụ trách ca, Trưởng
 * tầng được giao qua {@code DutyApi} của 008 (đã bỏ người vắng ca); Quản lý viện theo tài khoản, tạo sớm nhất trước (id tài
 * khoản tăng dần; chủ dự án chọn 2026-10-26).
 */
@Component
class CallReminderRouting {

    /** Người được giao lượt và các Quản lý viện khác cần báo khi lượt tới bậc Quản lý viện. */
    record Assignment(long staffId, Tier tier, List<Long> otherManagers) {}

    private final ObjectProvider<DutyApi> duties;
    private final ObjectProvider<StaffProfileApi> profiles;
    private final AccountApi accounts;
    private final BusinessTime time;

    CallReminderRouting(
            ObjectProvider<DutyApi> duties,
            ObjectProvider<StaffProfileApi> profiles,
            AccountApi accounts,
            BusinessTime time) {
        this.duties = duties;
        this.profiles = profiles;
        this.accounts = accounts;
        this.time = time;
    }

    Optional<Assignment> initial(Long floorAreaId, Optional<Long> calleeStaffId, Instant at) {
        return CallReminderTarget.of(lead(floorAreaId, at), floorManager(floorAreaId, at), managers(), calleeStaffId)
                .map(CallReminderRouting::assignment);
    }

    Optional<Assignment> after(Tier current, Long floorAreaId, Optional<Long> calleeStaffId, Instant at) {
        return CallReminderTarget.after(
                        current, lead(floorAreaId, at), floorManager(floorAreaId, at), managers(), calleeStaffId)
                .map(CallReminderRouting::assignment);
    }

    /** Người phụ trách ca đang diễn ra của tầng, nếu đủ điều kiện (FR-030a (b)). */
    Optional<Long> eligibleShiftLead(Long floorAreaId, Instant at) {
        return lead(floorAreaId, at).filter(Candidate::eligible).map(Candidate::staffId);
    }

    /** Người có quyền gọi: Người phụ trách ca đang diễn ra, Trưởng tầng được giao của tầng, mọi Quản lý viện (FR-029). */
    boolean canCall(CurrentUser user, Long floorAreaId) {
        if (user.roles().contains(Role.NURSING_HOME_MANAGER)) {
            return true;
        }
        Optional<Long> staff = user.staffId();
        if (staff.isEmpty() || floorAreaId == null) {
            return false;
        }
        Instant now = time.now();
        DutyApi du = duties.getIfAvailable();
        return du != null
                && (du.shiftLead(floorAreaId, now).equals(staff)
                        || du.assignedFloorManager(floorAreaId, now).equals(staff));
    }

    /** Quản lý viện đang Hoạt động hoặc Khóa tạm, trừ người cần gọi (FR-029a (b)). */
    List<Long> managerStaffIds(Optional<Long> exceptStaffId) {
        return accounts.activeWithRole(Role.NURSING_HOME_MANAGER).stream()
                .sorted(Comparator.comparingLong(AccountView::accountId))
                .map(AccountView::staffId)
                .filter(Objects::nonNull)
                .filter(id -> !exceptStaffId.equals(Optional.of(id)))
                .toList();
    }

    private Optional<Candidate> lead(Long floorAreaId, Instant at) {
        DutyApi du = duties.getIfAvailable();
        if (floorAreaId == null || du == null) {
            return Optional.empty();
        }
        return du.shiftLead(floorAreaId, at).map(this::candidate);
    }

    private Optional<Candidate> floorManager(Long floorAreaId, Instant at) {
        DutyApi du = duties.getIfAvailable();
        if (floorAreaId == null || du == null) {
            return Optional.empty();
        }
        return du.assignedFloorManager(floorAreaId, at).map(this::candidate);
    }

    private List<Candidate> managers() {
        return managerStaffIds(Optional.empty()).stream().map(this::candidate).toList();
    }

    /** Đủ điều kiện: còn làm việc, tài khoản Hoạt động hoặc Khóa tạm (FR-008, FR-029). */
    private Candidate candidate(long staffId) {
        StaffProfileApi staff = profiles.getIfAvailable();
        boolean working = staff == null || staff.status(staffId, time.today()) != StaffStatus.RESIGNED;
        boolean active = accounts.byStaff(staffId)
                .filter(a -> a.status() == AccountStatus.ACTIVE || a.status() == AccountStatus.TEMPORARILY_LOCKED)
                .isPresent();
        return new Candidate(staffId, working && active);
    }

    private static Assignment assignment(Target t) {
        return new Assignment(
                t.staffIds().getFirst(),
                t.tier(),
                t.tier() == Tier.MANAGERS ? t.staffIds().subList(1, t.staffIds().size()) : List.of());
    }
}
