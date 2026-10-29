package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.notification.api.spi.NotifiableRelationshipView;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 009 US1 (T017, tên logic {@code BR_M13_02_LuotNhacGoiIT}, {@code FR_027a_GopLuotIT}, {@code FR_028_ThatBaiMoiKenhIT},
 * {@code FR_030a_ChuyenNhacIT}, {@code FR_032_MotLuotIT}; quickstart Q2, Q3, Q4, Q6): lượt nhắc gọi khi Khẩn cấp chưa xác
 * nhận sau CFG-M13-01 hoặc gửi thất bại mọi kênh; người được nhắc theo FR-029; gộp theo (người cần gọi, người cao tuổi);
 * mỗi thông báo một lượt; người nhận tự xác nhận thì dừng; quá hạn CFG-M13-04 hoặc hết ca thì chuyển nhắc.
 */
class BR_M13_02_CallReminderIT extends CallReminderTestBase {

    @Test
    void BR_M13_02_unacknowledgedAfterCfg01_remindsShiftLead_onceEachNotification() throws Exception {
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

    @Test
    void FR_029_calleeIsShiftLead_remindsFloorManager_noFloorPeople_remindsFirstManager() {
        TestUser lead = users.staff(Role.NURSING_STAFF);
        TestUser floorManager = users.staff(Role.FLOOR_MANAGER);
        duties.shiftLead(floor, lead.staffId()).floorManager(floor, floorManager.staffId());
        send(urgent("nhac-lead-" + resident, new RecipientGroup.Staff(lead.staffId(), "X")));
        long otherFloor = floor;
        floor = TestUsers.nextId();
        TestUser alone = users.staff(Role.CAREGIVER);
        send(urgent("nhac-none-" + resident, new RecipientGroup.Staff(alone.staffId(), "X")));

        advance(PAST_CFG);
        creation.run();

        long forLead = reminders("STAFF", lead.staffId()).getFirst();
        assertThat(state(forLead)).containsEntry("current_staff_id", floorManager.staffId());
        assertThat(otherFloor).isNotEqualTo(floor);
        long forAlone = reminders("STAFF", alone.staffId()).getFirst();
        assertThat(state(forAlone)).containsEntry("current_staff_id", firstManagerStaffId());
        assertThat(handoffRows(forAlone).getFirst()).containsEntry("tier", "MANAGERS");
    }

    @Test
    void FR_027a_sameCalleeSameResident_mergedIntoAwaitingReminder() {
        TestUser lead = users.staff(Role.NURSING_STAFF);
        TestUser late = users.staff(Role.NURSING_STAFF);
        duties.shiftLead(floor, lead.staffId());
        send(urgent("gop-1-" + resident, new RecipientGroup.Staff(late.staffId(), "X")));
        advance(PAST_CFG);
        creation.run();
        long id = reminders("STAFF", late.staffId()).getFirst();

        send(urgent("gop-2-" + resident, new RecipientGroup.Staff(late.staffId(), "X")));
        advance(PAST_CFG);
        creation.run();

        assertThat(reminders("STAFF", late.staffId())).containsExactly(id);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM call_reminder_notification WHERE call_reminder_id = ?",
                        Integer.class,
                        id))
                .isEqualTo(2);
        assertThat(handoffRows(id)).extracting(h -> h.get("reason")).containsExactly("INITIAL", "CONTENT_UPDATED");
        awaitReminderNotices(id, lead.staffId(), "LOW", 1);
    }

    @Test
    void FR_028_familyWithoutAnyChannel_remindedImmediately() {
        TestUser lead = users.staff(Role.NURSING_STAFF);
        duties.shiftLead(floor, lead.staffId());
        long m = TestUsers.nextId();
        family.relate(
                resident,
                new NotifiableRelationshipView(
                        m, true, false, Set.of(FamilyRight.RECEIVE_URGENT), Optional.empty(), false, Optional.empty()));
        send(urgent("that-bai-" + resident, new RecipientGroup.PrimaryContact(resident)));

        creation.run();

        long id = reminders("FAMILY_MEMBER", m).getFirst();
        assertThat(jdbc.queryForObject("SELECT creation_reason FROM call_reminder WHERE id = ?", String.class, id))
                .isEqualTo("DELIVERY_FAILED");
        assertThat(state(id)).containsEntry("current_staff_id", lead.staffId());
    }

    @Test
    void FR_030_selfAcknowledged_stopsReminder_remindedToldNoNeedToCall() throws Exception {
        TestUser lead = users.staff(Role.NURSING_STAFF);
        TestUser late = users.staff(Role.NURSING_STAFF);
        duties.shiftLead(floor, lead.staffId());
        long notification = staffNotification(
                send(urgent("tu-xn-" + resident, new RecipientGroup.Staff(late.staffId(), "X"))), late);
        advance(PAST_CFG);
        creation.run();
        long id = reminders("STAFF", late.staffId()).getFirst();

        acknowledge(notification, late);

        assertThat(state(id)).containsEntry("status", "STOPPED").containsEntry("stop_reason", "SELF_ACKNOWLEDGED");
        awaitReminderNotices(id, lead.staffId(), "LOW", 1);
    }

    @Test
    void FR_030a_overdue_floorManagerThenManager_thenManagerRepeated() {
        TestUser lead = users.staff(Role.NURSING_STAFF);
        TestUser floorManager = users.staff(Role.FLOOR_MANAGER);
        TestUser late = users.staff(Role.NURSING_STAFF);
        duties.shiftLead(floor, lead.staffId()).floorManager(floor, floorManager.staffId());
        send(urgent("qua-han-" + resident, new RecipientGroup.Staff(late.staffId(), "X")));
        advance(PAST_CFG);
        creation.run();
        long id = reminders("STAFF", late.staffId()).getFirst();

        advance(Duration.ofMinutes(4));
        handoffs.run();
        assertThat(state(id)).containsEntry("current_staff_id", lead.staffId());

        advance(Duration.ofMinutes(2));
        handoffs.run();
        assertThat(state(id)).containsEntry("current_staff_id", floorManager.staffId());
        awaitReminderNotices(id, floorManager.staffId(), "URGENT", 1);
        awaitReminderNotices(id, lead.staffId(), "LOW", 1);

        advance(PAST_CFG);
        handoffs.run();
        long manager = firstManagerStaffId();
        assertThat(state(id)).containsEntry("current_staff_id", manager);

        advance(PAST_CFG);
        handoffs.run();
        assertThat(handoffRows(id))
                .extracting(h -> h.get("reason"))
                .containsExactly("INITIAL", "OVERDUE", "OVERDUE", "MANAGER_REPEAT");
        assertThat(handoffRows(id).getLast()).containsEntry("previous_staff_id", manager);
        await().atMost(Duration.ofSeconds(10)).until(() -> reminderNotices(id, manager, "URGENT") == 2);
    }

    @Test
    void FR_030a_shiftLeadShiftEnded_remindsCurrentShiftLeadWithNewDueTime() {
        TestUser day = users.staff(Role.NURSING_STAFF);
        TestUser night = users.staff(Role.NURSING_STAFF);
        TestUser late = users.staff(Role.NURSING_STAFF);
        duties.shiftLead(floor, day.staffId());
        send(urgent("het-ca-" + resident, new RecipientGroup.Staff(late.staffId(), "X")));
        advance(PAST_CFG);
        creation.run();
        long id = reminders("STAFF", late.staffId()).getFirst();

        duties.shiftLead(floor, night.staffId());
        advance(Duration.ofMinutes(1));
        handoffs.run();

        assertThat(state(id)).containsEntry("current_staff_id", night.staffId());
        assertThat(handoffRows(id).getLast())
                .containsEntry("reason", "SHIFT_ENDED")
                .containsEntry("tier", "SHIFT_LEAD")
                .containsEntry("previous_staff_id", day.staffId());
    }
}
