package com.eldercare.backend.care.internal.careplan.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.Role;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Q-449, 005 FR-002 (test luồng BF-01 2026-11-26): vai trò thực hiện của mục kế hoạch chăm sóc chỉ là Nhân viên chăm
 * sóc hoặc Điều dưỡng, vai trò có trong phân công chăm sóc (FR-024, Q-325).
 */
class CarePlanPerformersTest {

    @Test
    void Q_449_onlyCaregiverAndNursingStaffArePerformers() {
        assertThat(Arrays.stream(Role.values()).filter(CarePlanPerformers::allowed))
                .containsExactlyInAnyOrder(Role.CAREGIVER, Role.NURSING_STAFF);
    }
}
