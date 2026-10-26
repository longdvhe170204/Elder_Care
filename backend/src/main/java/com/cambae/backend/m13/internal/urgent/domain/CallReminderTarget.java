package com.cambae.backend.m13.internal.urgent.domain;

import java.util.List;
import java.util.Optional;

/**
 * Người được nhắc gọi ({@code NguoiNhacGoi}; FR-029, research B-08): Người phụ trách ca của tầng/khu vực → Trưởng tầng được
 * giao → Quản lý viện; bỏ người vắng ca hoặc tài khoản không Hoạt động và chính người cần gọi. Sự kiện không gắn tầng thì
 * người gọi truyền rỗng cho hai bậc đầu. Bậc Quản lý viện trả mọi người đủ điều kiện; chọn một người của bậc này để giao lượt
 * làm cùng lượt nhắc gọi (T019).
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
        Optional<Target> first = usable(shiftLead, calleeStaffId).map(id -> new Target(Tier.SHIFT_LEAD, List.of(id)));
        if (first.isPresent()) {
            return first;
        }
        Optional<Target> second =
                usable(floorManager, calleeStaffId).map(id -> new Target(Tier.FLOOR_MANAGER, List.of(id)));
        if (second.isPresent()) {
            return second;
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
