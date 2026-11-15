package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.notification.api.NotificationLookupApi;
import com.eldercare.backend.notification.api.spi.FamilyContactView;
import com.eldercare.backend.notification.api.spi.NotifiableRelationshipView;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 009 US1 (T017, tên logic {@code BR_M13_02_KetQuaGoiIT}, {@code FR_029a_ChuoiGoiIT}; quickstart Q3, Q5, Q7): ghi kết quả
 * gọi qua {@code POST /call-reminders/{id}/results} chỉ người có quyền gọi (Người phụ trách ca, Trưởng tầng được giao của
 * tầng, Quản lý viện); Đã liên lạc được thì thông báo gộp Đã xác nhận qua cuộc gọi; Không liên lạc được thì nhắc gọi
 * người thân kế tiếp của chuỗi, hết người thì báo Quản lý viện Khẩn cấp và thông báo chuyển Không liên lạc được; lần gọi
 * bổ sung Đã liên lạc được thì xác nhận muộn qua cuộc gọi. Đọc lượt của tôi và chi tiết có số điện thoại hiện hành.
 */
class FR_029a_CallResultIT extends CallReminderTestBase {

    @Autowired
    NotificationLookupApi lookup;

    /**
     * T019 (bảng trạng thái lượt nhắc gọi: Chờ kết quả → Đã dừng khi người cần gọi không còn đủ điều kiện; chủ dự án chốt
     * 2026-11-12): lệnh của 012 kết thúc quan hệ của người liên hệ chính (phát {@code RelationshipChanged} trong
     * transaction lệnh) thì 012 gọi {@code CallReminderCommandApi} ngay trong transaction đó; lượt của người đó dừng kèm
     * lý do và chuỗi gọi người kế tiếp như "Không liên lạc được" (FR-029a (a)).
     */
    @Test
    void FR_029a_calleeNoLongerEligible_stopsReminderAndCallsNextFamilyMember() throws Exception {
        TestUser lead = users.staff(Role.NURSING_STAFF);
        duties.shiftLead(floor, lead.staffId());
        long m = TestUsers.nextId();
        long r = TestUsers.nextId();
        TestUser mAccount = users.family(m);
        family.relate(
                        resident,
                        new NotifiableRelationshipView(
                                m,
                                true,
                                false,
                                Set.of(FamilyRight.RECEIVE_URGENT),
                                Optional.of(mAccount.accountId()),
                                true,
                                Optional.of("0904444011")))
                .callOrder(resident, List.of(m, r))
                .contact(
                        new FamilyContactView(r, "[Người thân R]", Optional.of("0904444012"), Optional.empty(), false));
        send(urgent("het-dieu-kien-" + resident, new RecipientGroup.PrimaryContact(resident)));
        advance(PAST_CFG);
        creation.run();
        long first = reminders("FAMILY_MEMBER", m).getFirst();
        assertThat(state(first)).containsEntry("status", "AWAITING_RESULT");

        family.callOrder(resident, List.of(r));
        tx.executeWithoutResult(
                t -> events.publishEvent(new com.eldercare.backend.family.api.events.RelationshipChanged(
                        m,
                        resident,
                        TestUsers.nextId(),
                        com.eldercare.backend.family.api.events.RelationshipChange.ENDED,
                        time.now())));

        assertThat(state(first)).containsEntry("status", "STOPPED").containsEntry("stop_reason", "CALLEE_NOT_ELIGIBLE");
        long next = reminders("FAMILY_MEMBER", r).getFirst();
        assertThat(jdbc.queryForMap("SELECT call_chain_id, creation_reason FROM call_reminder WHERE id = ?", next))
                .containsEntry("call_chain_id", first)
                .containsEntry("creation_reason", "PREVIOUS_NOT_REACHED");
        assertThat(state(next)).containsEntry("current_staff_id", lead.staffId());
    }
}
