package com.cambae.backend.m13.internal.recipient.domain;

import static com.cambae.backend.m13.internal.recipient.domain.SubstitutionStep.FLOOR_MANAGER;
import static com.cambae.backend.m13.internal.recipient.domain.SubstitutionStep.FLOOR_MANAGER_SUBSTITUTE;
import static com.cambae.backend.m13.internal.recipient.domain.SubstitutionStep.MANAGERS;
import static com.cambae.backend.m13.internal.recipient.domain.SubstitutionStep.SHIFT_LEAD;

import com.cambae.backend.common.notification.RecipientGroup;
import java.util.List;

/**
 * Chuỗi thay mặc định của nhóm loại (a) ({@code ChuoiThay}; FR-010, Q-110, Q-344, research B-04): lấy bậc đầu tiên có
 * người. Điều dưỡng phụ trách → Người phụ trách ca của tầng → Trưởng tầng được giao → Quản lý viện; nhóm khác → Trưởng
 * tầng được giao → Quản lý viện; Trưởng tầng trống thì Người phụ trách ca đang diễn ra của tầng trước Quản lý viện (Q-90).
 */
public final class SubstitutionChain {

    private SubstitutionChain() {}

    public static List<SubstitutionStep> steps(RecipientGroup.DutyGroup group) {
        return switch (group) {
            case RecipientGroup.AssignedNurse n ->
                List.of(SHIFT_LEAD, FLOOR_MANAGER, FLOOR_MANAGER_SUBSTITUTE, MANAGERS);
            case RecipientGroup.FloorManager f -> List.of(FLOOR_MANAGER_SUBSTITUTE, MANAGERS);
            default -> List.of(FLOOR_MANAGER, FLOOR_MANAGER_SUBSTITUTE, MANAGERS);
        };
    }
}
