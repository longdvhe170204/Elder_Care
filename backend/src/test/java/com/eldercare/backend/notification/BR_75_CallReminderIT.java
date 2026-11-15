package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 009 US1 (T017, tên logic {@code BR_M13_02_LuotNhacGoiIT}, {@code FR_027a_GopLuotIT}, {@code FR_028_ThatBaiMoiKenhIT},
 * {@code FR_030a_ChuyenNhacIT}, {@code FR_032_MotLuotIT}; quickstart Q2, Q3, Q4, Q6): lượt nhắc gọi khi Khẩn cấp chưa xác
 * nhận sau CFG-M13-01 hoặc gửi thất bại mọi kênh; người được nhắc theo FR-029; gộp theo (người cần gọi, người cao tuổi);
 * mỗi thông báo một lượt; người nhận tự xác nhận thì dừng; quá hạn CFG-M13-04 hoặc hết ca thì chuyển nhắc.
 */
class BR_75_CallReminderIT extends CallReminderTestBase {

    @Test
    void BR_75_unacknowledgedAfterCfg01_remindsShiftLead_onceEachNotification() throws Exception {
        TestUser lead = users.staff(Role.NURSING_STAFF);
        TestUser late = users.staff(Role.NURSING_STAFF);
        TestUser quick = users.staff(Role.DOCTOR);
        duties.shiftLead(floor, lead.staffId());
        Map<String, Long> sent = send(urgent(
                "nhac-" + resident,
                new RecipientGroup.Staff(late.staffId(), "X"),
                new RecipientGroup.Staff(quick.staffId(), "X")));
        acknowledge(staffNotification(sent, quick), quick);

        advance(Duration.ofMinutes(4));
        creation.run();
        assertThat(reminders("STAFF", late.staffId())).isEmpty();

        advance(Duration.ofMinutes(2));
        creation.run();
        creation.run();

        List<Long> forLate = reminders("STAFF", late.staffId());
        assertThat(forLate).hasSize(1);
        assertThat(reminders("STAFF", quick.staffId())).isEmpty();
        long id = forLate.getFirst();
        assertThat(state(id))
                .containsEntry("status", "AWAITING_RESULT")
                .containsEntry("current_staff_id", lead.staffId());
        assertThat(handoffRows(id))
                .singleElement()
                .satisfies(h -> assertThat(h)
                        .containsEntry("reminded_staff_id", lead.staffId())
                        .containsEntry("tier", "SHIFT_LEAD")
                        .containsEntry("reason", "INITIAL"));
        awaitReminderNotices(id, lead.staffId(), "URGENT", 1);
    }
}
