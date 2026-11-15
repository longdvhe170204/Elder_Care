package com.eldercare.backend.staffing.swap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.staffing.internal.swap.service.LeaveService;
import com.eldercare.backend.support.TestUsers;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** FR-026 → FR-031, FR-026a, FR-045: nghỉ đột xuất theo ca và theo ngày, duyệt một phần, lập thay, tách ca. */
class FR_029_ShiftLeaveIT extends SwapTestBase {

    @Autowired
    LeaveService leaveService;

    @Autowired
    com.eldercare.backend.staffing.scheduling.M09MinuteJob minuteJob;

    @Autowired
    org.springframework.context.ApplicationEventPublisher events;

    @Autowired
    org.springframework.transaction.support.TransactionTemplate transactions;

    private String statusOf(long requestId) {
        return jdbc.queryForObject("SELECT status FROM approval_request WHERE id=?", String.class, requestId);
    }

    private long createByShift(TestUsers.TestUser by, TestUsers.TestUser staff, long... shiftIds) throws Exception {
        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("kind", "BY_SHIFT");
        if (staff != null) {
            body.put("staffId", staff.staffId());
        }
        body.put("shiftIds", java.util.Arrays.stream(shiftIds).boxed().toList());
        body.put("reason", "[Lý do nghỉ riêng tư]");
        var response = send("/api/v1/leave-requests", by, body).andReturn().getResponse();
        if (response.getStatus() != 201) {
            throw new AssertionError(
                    "Lập nghỉ thất bại: " + response.getStatus() + " " + response.getContentAsString());
        }
        return json.readTree(response.getContentAsString())
                .get("requestIds")
                .get(0)
                .asLong();
    }

    private boolean onLeave(long shiftId, TestUsers.TestUser user) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM shift_staff WHERE shift_id=? AND staff_id=? AND approved_leave_request_id IS NOT NULL",
                Integer.class,
                shiftId,
                user.staffId());
        return n != null && n > 0;
    }

    @Test
    void FR_029_approvedLeaveMarksStaffOnLeaveAndKeepsOtherShifts() throws Exception {
        long x = shift(base.plus(Duration.ofHours(7)), 8, null, giver);
        long other = shift(base.plus(Duration.ofDays(1)).plus(Duration.ofHours(7)), 8, null, giver);
        long id = createByShift(giver, null, x);
        assertThat(statusOf(id)).isEqualTo("PENDING_APPROVAL");
        send("/api/v1/leave-requests/" + id + "/approve", head, null).andExpect(status().isOk());
        assertThat(statusOf(id)).isEqualTo("APPLIED");
        assertThat(onLeave(x, giver)).isTrue();
        assertThat(onLeave(other, giver)).isFalse();
        // Khóa đã nhả: xin nghỉ lại ca khác không bị chặn bởi ca đã quyết định.
        createByShift(giver, null, other);
    }

    @Test
    void FR_029_d_leadMustBeChosenWhenAnotherEligibleMemberIsPresent() throws Exception {
        long x = shift(base.plus(Duration.ofHours(7)), 8, giver.staffId(), giver, recipient);
        long id = createByShift(giver, null, x);
        // FR-027, 015 T002: tác động trước khi duyệt có tỷ lệ phục vụ trước/sau của ca.
        fetch("/api/v1/leave-requests/" + id + "/pre-approval", head)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].staffingRatioBeforeAfter.shiftId").value(x));
        send("/api/v1/leave-requests/" + id + "/approve", head, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("LEAD_REQUIRED"));
        send(
                        "/api/v1/leave-requests/" + id + "/approve",
                        head,
                        Map.of(
                                "items",
                                List.of(Map.of("shiftId", x, "result", "APPROVED", "newLeadId", recipient.staffId()))))
                .andExpect(status().isOk());
        Long lead = jdbc.queryForObject("SELECT lead_staff_id FROM work_shift WHERE id=?", Long.class, x);
        assertThat(lead).isEqualTo(recipient.staffId());
    }
}
