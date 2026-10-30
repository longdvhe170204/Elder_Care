package com.eldercare.backend.notification.internal.urgent.domain;

import java.util.List;
import java.util.Optional;

/**
 * Người được nhắc gọi ({@code NguoiNhacGoi}; FR-029, research B-08): Người phụ trách ca của tầng/khu vực → Trưởng tầng được
 * giao → Quản lý viện; bỏ người vắng ca hoặc tài khoản không Hoạt động và chính người cần gọi. Sự kiện không gắn tầng thì
 * người gọi truyền rỗng cho hai bậc đầu. Bậc Quản lý viện trả mọi người đủ điều kiện theo thứ tự người gọi truyền; lượt giao
 * cho người đầu tiên (tài khoản tạo sớm nhất, chủ dự án chọn 2026-10-26), báo những người còn lại.
 */
public final class CallReminderTarget {

    private CallReminderTarget() {}

    /** Bậc trong thứ tự FR-029. */
    public enum Tier {
        SHIFT_LEAD,
        FLOOR_MANAGER,
        MANAGERS
    }

    /**
     * @param eligible không vắng ca trong ca đang diễn ra, tài khoản Hoạt động hoặc Khóa tạm
     */
    public record Candidate(long staffId, boolean eligible) {}

    public record Target(Tier tier, List<Long> staffIds) {}

    /** @param calleeStaffId người cần gọi khi là nhân viên */
    public static Optional<Target> of(
            Optional<Candidate> shiftLead,
            Optional<Candidate> floorManager,
            List<Candidate> managers,
            Optional<Long> calleeStaffId) {
        return from(Tier.SHIFT_LEAD, shiftLead, floorManager, managers, calleeStaffId);
    }

    /** Người kế tiếp khi quá hạn (FR-030a (a)): bậc sau bậc hiện tại; đã ở Quản lý viện thì vẫn là Quản lý viện. */
    public static Optional<Target> after(
            Tier current,
            Optional<Candidate> shiftLead,
            Optional<Candidate> floorManager,
            List<Candidate> managers,
            Optional<Long> calleeStaffId) {
        Tier start = current == Tier.SHIFT_LEAD ? Tier.FLOOR_MANAGER : Tier.MANAGERS;
        return from(start, shiftLead, floorManager, managers, calleeStaffId);
    }

    private static Optional<Target> from(
            Tier start,
            Optional<Candidate> shiftLead,
            Optional<Candidate> floorManager,
            List<Candidate> managers,
            Optional<Long> calleeStaffId) {
        if (start == Tier.SHIFT_LEAD) {
            Optional<Long> lead = usable(shiftLead, calleeStaffId);
            if (lead.isPresent()) {
                return Optional.of(new Target(Tier.SHIFT_LEAD, List.of(lead.get())));
            }
        }
        if (start != Tier.MANAGERS) {
            Optional<Long> manager = usable(floorManager, calleeStaffId);
            if (manager.isPresent()) {
                return Optional.of(new Target(Tier.FLOOR_MANAGER, List.of(manager.get())));
            }
        }
        List<Long> ids = managers.stream()
                .flatMap(m -> usable(Optional.of(m), calleeStaffId).stream())
                .toList();
        return ids.isEmpty() ? Optional.empty() : Optional.of(new Target(Tier.MANAGERS, ids));
    }

    private static Optional<Long> usable(Optional<Candidate> c, Optional<Long> calleeStaffId) {
        return c.filter(Candidate::eligible)
                .map(Candidate::staffId)
                .filter(id -> !calleeStaffId.equals(Optional.of(id)));
    }
}
