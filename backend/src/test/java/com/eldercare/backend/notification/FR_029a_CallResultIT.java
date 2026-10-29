package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.notification.api.NotificationLookupApi;
import com.eldercare.backend.notification.api.dto.RecipientType;
import com.eldercare.backend.notification.api.spi.FamilyContactView;
import com.eldercare.backend.notification.api.spi.NotifiableRelationshipView;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

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

    private ResultActions record(long reminderId, TestUser by, String body) throws Exception {
        return mvc.perform(post("/api/v1/call-reminders/" + reminderId + "/results")
                .header("Authorization", bearer(by))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String reached() {
        return "{\"result\":\"REACHED\",\"calledAt\":\"" + time.now() + "\",\"note\":\"Đã báo tin\"}";
    }

    private String notReached() {
        return "{\"result\":\"NOT_REACHED\",\"calledAt\":\"" + time.now() + "\",\"reason\":\"NO_ANSWER\"}";
    }

    /** Thông báo Khẩn cấp quản lý viện nhận khi hết người để gọi (FR-029a). */
    private int managerUnreachableNotices(long reminderId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM notification n JOIN notification_request q ON q.id = n.request_id"
                        + " WHERE q.source_record_type = 'M13.CALL_REMINDER' AND q.source_record_id = ?"
                        + " AND q.event_type = 'CALL_CHAIN_NOT_REACHED' AND n.level = 'URGENT'",
                Integer.class,
                Long.toString(reminderId));
    }

    @Test
    void FR_029_reachedByFloorManagerWhileAtShiftLead_othersRefused() throws Exception {
        TestUser lead = users.staff(Role.NURSING_STAFF);
        TestUser floorManager = users.staff(Role.FLOOR_MANAGER);
        TestUser caregiver = users.staff(Role.CAREGIVER);
        TestUser late = users.staff(Role.NURSING_STAFF);
        duties.shiftLead(floor, lead.staffId()).floorManager(floor, floorManager.staffId());
        long notification =
                staffNotification(send(urgent("kq-" + resident, new RecipientGroup.Staff(late.staffId(), "X"))), late);
        advance(PAST_CFG);
        creation.run();
        long id = reminders("STAFF", late.staffId()).getFirst();

        record(id, caregiver, reached())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NOT_ALLOWED_TO_CALL"));
        record(id, floorManager, "{\"result\":\"REACHED\",\"calledAt\":\"" + time.now() + "\"}")
                .andExpect(status().isUnprocessableEntity());
        record(id, floorManager, reached()).andExpect(status().isCreated());
        record(id, lead, reached())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CALL_REMINDER_ENDED"));

        assertThat(state(id)).containsEntry("status", "RESULT_RECORDED").containsEntry("final_result", "REACHED");
        assertThat(notificationStatus(notification)).isEqualTo("ACKNOWLEDGED/BY_CALL");
        awaitReminderNotices(id, lead.staffId(), "LOW", 1);
        // FR-031: kết quả gọi cung cấp cho module nguồn
        assertThat(lookup.bySource("M05.INCIDENT", "kq-" + resident))
                .singleElement()
                .satisfies(v -> assertThat(v.callReminders()).singleElement().satisfies(c -> {
                    assertThat(c.callReminderId()).isEqualTo(id);
                    assertThat(c.status()).isEqualTo("RESULT_RECORDED");
                    assertThat(c.results()).extracting(r -> r.result()).containsExactly("REACHED");
                }));
    }

    @Test
    void FR_029a_familyNotReached_nextFamilyMember_thenManagersUrgent_unreachable_thenSupplementaryReached()
            throws Exception {
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
                                Optional.of("0904444001")))
                .callOrder(resident, List.of(r, m))
                .contact(
                        new FamilyContactView(r, "[Người thân R]", Optional.of("0904444002"), Optional.empty(), false));
        long notification = send(urgent("chuoi-" + resident, new RecipientGroup.PrimaryContact(resident)))
                .get("FAMILY_MEMBER:" + m);
        advance(PAST_CFG);
        creation.run();
        long first = reminders("FAMILY_MEMBER", m).getFirst();

        record(first, lead, notReached()).andExpect(status().isCreated());

        long second = reminders("FAMILY_MEMBER", r).getFirst();
        assertThat(jdbc.queryForMap("SELECT call_chain_id, creation_reason FROM call_reminder WHERE id = ?", second))
                .containsEntry("call_chain_id", first)
                .containsEntry("creation_reason", "PREVIOUS_NOT_REACHED");
        assertThat(state(second)).containsEntry("current_staff_id", lead.staffId());
        assertThat(notificationStatus(notification)).doesNotStartWith("UNREACHABLE");

        record(second, lead, notReached()).andExpect(status().isCreated());

        assertThat(notificationStatus(notification)).startsWith("UNREACHABLE/");
        await().atMost(Duration.ofSeconds(10)).until(() -> managerUnreachableNotices(second) >= 1);
        assertThat(reminders("FAMILY_MEMBER", m)).hasSize(1);

        record(first, lead, reached()).andExpect(status().isCreated());
        assertThat(notificationStatus(notification)).isEqualTo("ACKNOWLEDGED/LATE_BY_CALL");
        assertThat(jdbc.queryForObject(
                        "SELECT supplementary FROM call_result WHERE call_reminder_id = ? ORDER BY id DESC LIMIT 1",
                        Boolean.class,
                        first))
                .isTrue();
    }

    @Test
    void FR_030_nextFamilyMemberReached_originalNotificationStaysUnacknowledged_sourceSeesWholeChain()
            throws Exception {
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
                                Optional.of("0904444101")))
                .callOrder(resident, List.of(r, m))
                .contact(
                        new FamilyContactView(r, "[Người thân R]", Optional.of("0904444102"), Optional.empty(), false));
        String key = "chuoi-r-" + resident;
        long notification =
                send(urgent(key, new RecipientGroup.PrimaryContact(resident))).get("FAMILY_MEMBER:" + m);
        advance(PAST_CFG);
        creation.run();
        long first = reminders("FAMILY_MEMBER", m).getFirst();
        record(first, lead, notReached()).andExpect(status().isCreated());
        long second = reminders("FAMILY_MEMBER", r).getFirst();

        record(second, lead, reached()).andExpect(status().isCreated());

        // FR-030: chỉ thông báo gộp vào lượt vừa gọi được mới xác nhận qua cuộc gọi; M chưa biết tin (FR-026).
        // Q-104: chưa phải mọi cuộc gọi đều không liên lạc được, nên cũng không chuyển Không liên lạc được.
        assertThat(state(second)).containsEntry("final_result", "REACHED");
        assertThat(notificationStatus(notification))
                .doesNotStartWith("ACKNOWLEDGED")
                .doesNotStartWith("UNREACHABLE");
        assertThat(reminders("FAMILY_MEMBER", m)).hasSize(1);
        assertThat(managerUnreachableNotices(second)).isZero();
        // FR-031, Q-388: module nguồn thấy mọi lượt của chuỗi, kèm người cần gọi và kết quả của từng lượt
        assertThat(lookup.bySource("M05.INCIDENT", key))
                .filteredOn(v -> v.notificationId() == notification)
                .singleElement()
                .satisfies(v -> assertThat(v.callReminders())
                        .extracting(
                                c -> c.callReminderId(),
                                c -> c.personToCallType(),
                                c -> c.personToCallId(),
                                c -> c.results().getLast().result())
                        .containsExactly(
                                org.assertj.core.groups.Tuple.tuple(
                                        first, RecipientType.FAMILY_MEMBER, m, "NOT_REACHED"),
                                org.assertj.core.groups.Tuple.tuple(
                                        second, RecipientType.FAMILY_MEMBER, r, "REACHED")));
    }

    @Test
    void FR_029a_staffNotReached_unreachable_managersToldUrgent() throws Exception {
        TestUser lead = users.staff(Role.NURSING_STAFF);
        TestUser late = users.staff(Role.NURSING_STAFF);
        duties.shiftLead(floor, lead.staffId());
        long notification = staffNotification(
                send(urgent("nv-kll-" + resident, new RecipientGroup.Staff(late.staffId(), "X"))), late);
        advance(PAST_CFG);
        creation.run();
        long id = reminders("STAFF", late.staffId()).getFirst();

        record(id, lead, notReached()).andExpect(status().isCreated());

        assertThat(state(id)).containsEntry("final_result", "NOT_REACHED");
        assertThat(notificationStatus(notification)).startsWith("UNREACHABLE/");
        await().atMost(Duration.ofSeconds(10)).until(() -> managerUnreachableNotices(id) >= 1);
    }

    @Test
    void FR_027_myRemindersAndDetail_showCurrentPhoneAtOpen() throws Exception {
        TestUser lead = users.staff(Role.NURSING_STAFF);
        TestUser late = users.staff(Role.NURSING_STAFF);
        TestUser caregiver = users.staff(Role.CAREGIVER);
        duties.shiftLead(floor, lead.staffId());
        profiles.phone(late.staffId(), "0905555001");
        send(urgent("doc-" + resident, new RecipientGroup.Staff(late.staffId(), "X")));
        advance(PAST_CFG);
        creation.run();
        long id = reminders("STAFF", late.staffId()).getFirst();
        profiles.phone(late.staffId(), "0905555009");

        mvc.perform(get("/api/v1/me/call-reminders").header("Authorization", bearer(lead)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].status").value("AWAITING_RESULT"));
        mvc.perform(get("/api/v1/call-reminders/" + id).header("Authorization", bearer(lead)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personToCall.type").value("STAFF"))
                .andExpect(jsonPath("$.personToCall.phoneNumber").value("0905555009"))
                .andExpect(jsonPath("$.residentId").value(resident))
                .andExpect(jsonPath("$.contentToReport[0].content").value("Có sự cố khẩn cấp doc-" + resident))
                .andExpect(jsonPath("$.canRecordResult").value(true));
        mvc.perform(get("/api/v1/me/call-reminders").header("Authorization", bearer(caregiver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").isEmpty());
        mvc.perform(get("/api/v1/call-reminders/" + id).header("Authorization", bearer(caregiver)))
                .andExpect(status().isForbidden());
    }
}
