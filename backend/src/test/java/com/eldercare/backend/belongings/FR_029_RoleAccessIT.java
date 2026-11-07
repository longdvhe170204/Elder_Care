package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * 013 T032 (FR-029; quickstart Q44): Nhân viên chăm sóc, Bác sĩ không có quyền ở dòng "Đồ gửi" của Phụ lục 27 nên xem đồ gửi
 * (danh sách, chi tiết) bị từ chối. Lớp quyền của 002 trả 403 (Nhân viên chăm sóc) hoặc 404
 * (Bác sĩ, FR-043 không lộ người cao tuổi, như plan 013 điểm 11); quickstart Q44 ghi 403.
 */
class FR_029_RoleAccessIT extends IntakeTestBase {

    @Test
    void Q44_caregiverAndDoctor_cannotReadBelongings() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long belonging = heldBelonging(resident, null);
        for (Role role : new Role[] {Role.CAREGIVER, Role.DOCTOR}) {
            String token = TestUsers.bearer(users.session(users.staff(role).accountId()));
            getJson(token, "/api/v1/residents/" + resident + "/belongings").andExpect(denied());
            getJson(token, "/api/v1/belongings/" + belonging).andExpect(denied());
        }
    }

    private static ResultMatcher denied() {
        return result -> assertThat(result.getResponse().getStatus()).isIn(403, 404);
    }
}
