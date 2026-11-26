package com.eldercare.backend.care.internal.careplan.domain;

import com.eldercare.backend.common.Role;
import java.util.Set;

/**
 * Vai trò thực hiện của mục kế hoạch chăm sóc (Q-449, FR-002): chỉ vai trò có trong phân công chăm sóc của ca (FR-024,
 * Q-325), để công việc lập từ mục chọn được người thực hiện.
 */
public final class CarePlanPerformers {

    private static final Set<Role> ALLOWED = Set.of(Role.CAREGIVER, Role.NURSING_STAFF);

    private CarePlanPerformers() {}

    public static boolean allowed(Role role) {
        return ALLOWED.contains(role);
    }
}
