package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import org.junit.jupiter.api.Test;

/**
 * 012 T039 (quickstart Q48; FR-067, ma trận quyền Phụ lục 27): Nhân viên chăm sóc không có lệnh nào của lượt người thân ở
 * lại (đăng ký, xác nhận, bắt đầu, kết thúc) và Hành chính không đọc nhật ký xem sức khỏe; gọi thì 403, lượt không đổi
 * trạng thái.
 */
class Q48_PermissionDeniedIT extends StayTestBase {

    @Test
    void Q48_caregiverCallsStayCommands_isForbidden_stayUnchanged() throws Exception {
        String caregiver = staff(users.staff(Role.CAREGIVER));

        register(caregiver, rMember, at(day, 18, 0), at(day.plusDays(1), 8, 0)).andExpect(status().isForbidden());

        long pending = registered(rMember, at(day, 18, 0), at(day.plusDays(1), 8, 0));
        command(caregiver, pending, "confirm", "{}").andExpect(status().isForbidden());
        command(caregiver, pending, "reject", "{\"reason\": \"Không đủ chỗ\"}").andExpect(status().isForbidden());
        assertThat(stayStatus(pending)).isEqualTo("PENDING_CONFIRMATION");

        long confirmed = confirmed(mMember, at(day.plusDays(2), 18, 0), at(day.plusDays(3), 8, 0));
        moveTo(at(day.plusDays(2), 18, 0));
        caregiver = staff(users.staff(Role.CAREGIVER));
        command(caregiver, confirmed, "start", "{}").andExpect(status().isForbidden());
        assertThat(stayStatus(confirmed)).isEqualTo("CONFIRMED");

        command(office, confirmed, "start", "{}").andExpect(status().isOk());
        command(caregiver, confirmed, "end", "{}").andExpect(status().isForbidden());
        assertThat(stayStatus(confirmed)).isEqualTo("STAYING");
    }

    @Test
    void Q48_officeStaffReadsHealthViewLog_isForbidden() throws Exception {
        mvc.perform(get("/api/v1/health-view-logs?residentId=" + a).header("Authorization", office))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/health-view-logs?residentId=" + a).header("Authorization", staff(manager)))
                .andExpect(status().isOk());
    }
}
