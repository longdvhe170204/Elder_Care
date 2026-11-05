package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.family.api.events.FeedbackNotice;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * Phản hồi, khiếu nại có hạn và leo thang ({@code BR_M10_05_PhanHoiIT}; 012 T029, Q43 → Q46; US6 kịch bản 1 → 9; FR-058
 * → FR-066, FR-067, BR-M10-05).
 */
class BR_M10_05_FeedbackIT extends FeedbackTestBase {

    /** Q43, US6 kịch bản 1: khiếu nại nhóm chăm sóc ở Mới, mức Cao, hạn gửi + 24 giờ, phụ trách Trưởng tầng. */
    @Test
    void Q43_complaintIsHighDue24hAssignedToFloorManager() throws Exception {
        long id = submitted(family(mMember), null, "COMPLAINT", "CARE");
        Instant sent = at(id, "submitted_at");

        JsonNode detail = getJsonAs(staff(floorManager), "/api/v1/feedback/" + id);
        assertThat(detail.get("status").asText()).isEqualTo("NEW");
        assertThat(detail.get("priority").asText()).isEqualTo("HIGH");
        assertThat(Instant.parse(detail.get("dueAt").asText())).isEqualTo(sent.plus(Duration.ofHours(24)));
        assertThat(detail.get("overdue").asBoolean()).isFalse();
        assertThat(detail.get("familyMemberId").asLong()).isEqualTo(mMember);
        assertThat(detail.get("source").asText()).isEqualTo("PORTAL");
        assertThat(row(id).get("assignee_account_id")).isEqualTo(floorManager.accountId());

        List<FeedbackNotice> assigned = notices(id, FeedbackNotice.Kind.ASSIGNED);
        assertThat(assigned).hasSize(1);
        NotificationRequest request = assigned.getFirst().notificationRequests().getFirst();
        assertThat(request.level()).contains(NotificationLevel.MEDIUM);
        assertThat(request.recipientGroups())
                .containsExactly(new RecipientGroup.Staff(floorManager.staffId(), "ASSIGNEE"));
    }

    /** FR-062: Góp ý, Hỏi đáp mặc định Thường (72 giờ); FR-059: chi phí, hợp đồng giao Hành chính. */
    @Test
    void FR_059_062_normalPriorityAndOfficeGroup() throws Exception {
        long id = submitted(family(rMember), null, "QUESTION", "COSTS_CONTRACT");
        Instant sent = at(id, "submitted_at");

        JsonNode detail = getJsonAs(office, "/api/v1/feedback/" + id);
        assertThat(detail.get("priority").asText()).isEqualTo("NORMAL");
        assertThat(Instant.parse(detail.get("dueAt").asText())).isEqualTo(sent.plus(Duration.ofHours(72)));
        assertThat(row(id).get("assignee_role")).isEqualTo("OFFICE_STAFF");
        assertThat(row(id).get("assignee_account_id")).isNull();
        assertThat(notices(id, FeedbackNotice.Kind.ASSIGNED)
                        .getFirst()
                        .notificationRequests()
                        .getFirst()
                        .recipientGroups())
                .containsExactly(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF));

        // Hành chính là nhóm: người Nhận xử lý thành người phụ trách
        feedbackCommand(office, id, "start-handling", "{}").andExpect(status().isOk());
        assertThat(row(id).get("assignee_account_id")).isEqualTo(officeStaff.accountId());
        assertThat(row(id).get("status")).isEqualTo("IN_PROGRESS");
    }

    /** FR-059: tầng không có Trưởng tầng được giao thì giao Quản lý viện. */
    @Test
    void FR_059_floorWithoutManagerAssignsFacilityManager() throws Exception {
        long otherFloor = com.eldercare.backend.support.TestUsers.nextId();
        long room = com.eldercare.backend.support.TestUsers.nextId();
        locations.closeStays(a, now());
        locations.floor(otherFloor, null).room(room, otherFloor).stay(a, room, now(), null);

        long id = submitted(family(mMember), null, "SUGGESTION", "EATING");

        assertThat(row(id).get("assignee_role")).isEqualTo("NURSING_HOME_MANAGER");
        assertThat(notices(id, FeedbackNotice.Kind.ASSIGNED)
                        .getFirst()
                        .notificationRequests()
                        .getFirst()
                        .recipientGroups())
                .containsExactly(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
    }

    /** US6 kịch bản 2, 4: Nhận xử lý, Trả lời (báo người gửi), Đồng ý đóng với cách đóng người gửi. */
    @Test
    void US6_startReplyAgreeToClose() throws Exception {
        long id = submitted(family(mMember), null, "COMPLAINT", "CARE");
        String tt = staff(floorManager);

        feedbackCommand(tt, id, "reply", reply())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));
        feedbackCommand(tt, id, "start-handling", "{}").andExpect(status().isOk());
        feedbackCommand(tt, id, "reply", "{\"content\": \"Thiếu hướng xử lý\"}")
                .andExpect(status().isUnprocessableContent());
        feedbackCommand(tt, id, "reply", reply()).andExpect(status().isOk());

        assertThat(row(id).get("status")).isEqualTo("REPLIED");
        List<FeedbackNotice> replied = notices(id, FeedbackNotice.Kind.REPLIED);
        assertThat(replied).hasSize(1);
        NotificationRequest request = replied.getFirst().notificationRequests().getFirst();
        assertThat(request.level()).contains(NotificationLevel.LOW);
        assertThat(request.recipientGroups())
                .containsExactly(new RecipientGroup.FamilyMember(mMember, "FEEDBACK_SENDER"));
        assertThat(request.content().getFirst().type()).isEqualTo(InformationType.GENERAL);

        JsonNode mine = getJsonAs(family(mMember), "/api/v1/feedback/" + id);
        assertThat(texts(mine, "REPLY")).containsExactly("Đã dọn và nhắc tổ chăm sóc");
        assertThat(texts(mine, "HANDLING_DIRECTION")).containsExactly("Bố trí dọn phòng hằng ngày");

        feedbackCommand(family(mMember), id, "agree-to-close", "{}").andExpect(status().isOk());
        assertThat(row(id).get("status")).isEqualTo("CLOSED");
        assertThat(row(id).get("close_method")).isEqualTo("SENDER");
        assertThat(row(id).get("closed_at")).isNotNull();

        // US6 kịch bản 7: phản hồi Đóng không đổi được nội dung, kết quả
        feedbackCommand(tt, id, "reply", reply()).andExpect(status().isConflict());
        feedbackCommand(family(mMember), id, "reopen", reason("Chưa ổn")).andExpect(status().isConflict());
    }

    /** Q44, US6 kịch bản 3, 9: quá hạn thì dấu quá hạn, Quản lý viện nhận Trung bình đúng một lần mỗi hạn. */
    @Test
    void Q44_overdueEscalatesOncePerDue() throws Exception {
        long id = submitted(family(mMember), null, "COMPLAINT", "CARE");
        Instant sent = at(id, "submitted_at");

        runFeedbackTasks(sent.plus(Duration.ofHours(24)).minusSeconds(60));
        assertThat(notices(id, FeedbackNotice.Kind.ESCALATED)).isEmpty();

        runFeedbackTasks(sent.plus(Duration.ofHours(24)));
        assertThat(row(id).get("overdue")).isEqualTo(true);
        List<FeedbackNotice> escalated = notices(id, FeedbackNotice.Kind.ESCALATED);
        assertThat(escalated).hasSize(1);
        NotificationRequest request =
                escalated.getFirst().notificationRequests().getFirst();
        assertThat(request.level()).contains(NotificationLevel.MEDIUM);
        assertThat(request.recipientGroups())
                .containsExactly(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
        // người phụ trách không đổi
        assertThat(row(id).get("assignee_account_id")).isEqualTo(floorManager.accountId());

        runFeedbackTasks(sent.plus(Duration.ofDays(3)));
        assertThat(notices(id, FeedbackNotice.Kind.ESCALATED)).hasSize(1);
        assertThat(ids(getJsonAs(staff(manager), "/api/v1/feedback?overdue=true")))
                .contains(id);

        // Trả lời, người thân mở lại, hạn mới lại quá: leo thang mới
        String tt = staff(floorManager);
        feedbackCommand(tt, id, "start-handling", "{}").andExpect(status().isOk());
        feedbackCommand(tt, id, "reply", reply()).andExpect(status().isOk());
        assertThat(ids(getJsonAs(staff(manager), "/api/v1/feedback?overdue=true")))
                .doesNotContain(id);
        feedbackCommand(family(mMember), id, "reopen", reason("Phòng vẫn chưa dọn"))
                .andExpect(status().isOk());
        assertThat(row(id).get("overdue")).isEqualTo(false);
        Instant reopened = at(id, "due_basis_at");
        assertThat(at(id, "due_at")).isEqualTo(reopened.plus(Duration.ofHours(24)));
        runFeedbackTasks(reopened.plus(Duration.ofHours(24)));
        assertThat(notices(id, FeedbackNotice.Kind.ESCALATED)).hasSize(2);
    }

    /** Q45, US6 kịch bản 8: người gửi không có quyền xem sức khỏe: cảnh báo lúc soạn, chỉ thấy phần chung. */
    @Test
    void Q45_healthReplyHiddenFromSenderWithoutRight() throws Exception {
        long id = submitted(family(nMember), null, "QUESTION", "HEALTH_MEDICATION");
        String tt = staff(floorManager);

        JsonNode compose = getJsonAs(tt, "/api/v1/feedback/" + id);
        assertThat(compose.get("senderHealthRightEffective").asBoolean()).isFalse();
        feedbackCommand(tt, id, "start-handling", "{}").andExpect(status().isOk());
        feedbackCommand(tt, id, "reply", reply())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderHealthRightEffective").value(false));

        JsonNode seen = getJsonAs(family(nMember), "/api/v1/feedback/" + id);
        assertThat(seen.get("status").asText()).isEqualTo("REPLIED");
        assertThat(texts(seen, "REPLY")).isEmpty();
        assertThat(texts(seen, "HANDLING_DIRECTION")).isEmpty();
        assertThat(seen.get("healthReplyHidden").asBoolean()).isTrue();
        assertThat(notices(id, FeedbackNotice.Kind.REPLIED)
                        .getFirst()
                        .notificationRequests()
                        .getFirst()
                        .content()
                        .getFirst()
                        .type())
                .isEqualTo(InformationType.HEALTH);

        // M có quyền xem sức khỏe có tác dụng: thấy nội dung trả lời
        long mine = submitted(family(mMember), null, "QUESTION", "HEALTH_MEDICATION");
        assertThat(getJsonAs(tt, "/api/v1/feedback/" + mine)
                        .get("senderHealthRightEffective")
                        .asBoolean())
                .isTrue();
        feedbackCommand(tt, mine, "start-handling", "{}").andExpect(status().isOk());
        feedbackCommand(tt, mine, "reply", reply()).andExpect(status().isOk());
        JsonNode mSeen = getJsonAs(family(mMember), "/api/v1/feedback/" + mine);
        assertThat(texts(mSeen, "REPLY")).containsExactly("Đã dọn và nhắc tổ chăm sóc");
        assertThat(mSeen.get("healthReplyHidden").asBoolean()).isFalse();
    }

    /** Q46, US6 kịch bản 4, 5: Mở lại có lý do, hạn mới từ lúc mở lại; Đã phản hồi quá 7 ngày thì tự đóng. */
    @Test
    void Q46_reopenNewDueThenAutoClose() throws Exception {
        long id = submitted(family(mMember), null, "SUGGESTION", "CARE");
        String tt = staff(floorManager);
        feedbackCommand(tt, id, "start-handling", "{}").andExpect(status().isOk());
        feedbackCommand(tt, id, "reply", reply()).andExpect(status().isOk());

        moveTo(now().plus(Duration.ofHours(10)));
        feedbackCommand(family(mMember), id, "reopen", "{}").andExpect(status().isUnprocessableContent());
        feedbackCommand(family(mMember), id, "reopen", reason("Chưa hài lòng")).andExpect(status().isOk());
        assertThat(row(id).get("status")).isEqualTo("REOPENED");
        Instant reopened = at(id, "due_basis_at");
        assertThat(reopened).isAfter(at(id, "submitted_at").plus(Duration.ofHours(10)));
        JsonNode detail = getJsonAs(family(mMember), "/api/v1/feedback/" + id);
        assertThat(Instant.parse(detail.get("dueAt").asText())).isEqualTo(reopened.plus(Duration.ofHours(72)));
        assertThat(texts(detail, "REOPEN")).containsExactly("Chưa hài lòng");
        assertThat(notices(id, FeedbackNotice.Kind.ASSIGNED)).hasSize(2);

        feedbackCommand(staff(floorManager), id, "start-handling", "{}").andExpect(status().isOk());
        feedbackCommand(staff(floorManager), id, "reply", reply()).andExpect(status().isOk());
        Instant replied = at(id, "replied_at");

        runFeedbackTasks(replied.plus(Duration.ofDays(7)).minusSeconds(60));
        assertThat(row(id).get("status")).isEqualTo("REPLIED");
        runFeedbackTasks(replied.plus(Duration.ofDays(7)));
        assertThat(row(id).get("status")).isEqualTo("CLOSED");
        assertThat(row(id).get("close_method")).isEqualTo("AUTO");
    }

    /** US6 kịch bản 6: Hành chính ghi thay R tại quầy; R thấy phản hồi trên cổng, người khác không thấy. */
    @Test
    void US6_officeRecordsAtCounter() throws Exception {
        long id = submitted(office, rMember, "COMPLAINT", "COSTS_CONTRACT");

        assertThat(row(id).get("source")).isEqualTo("COUNTER");
        assertThat(row(id).get("family_member_id")).isEqualTo(rMember);
        assertThat(ids(getJsonAs(family(rMember), "/api/v1/feedback"))).contains(id);
        assertThat(ids(getJsonAs(family(mMember), "/api/v1/feedback"))).doesNotContain(id);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/feedback/" + id)
                        .header("Authorization", family(mMember)))
                .andExpect(status().isNotFound());

        // Hành chính ghi thay phải chọn người thân có quan hệ Hiệu lực; Điều dưỡng không ghi thay người thân
        submit(office, null, "COMPLAINT", "CARE").andExpect(status().isUnprocessableContent());
        submit(staff(nurse), mMember, "COMPLAINT", "CARE").andExpect(status().isForbidden());
        // người thân chỉ gửi cho chính mình
        submit(family(mMember), rMember, "COMPLAINT", "CARE").andExpect(status().isForbidden());
    }

    /** US6 kịch bản 9, FR-067: Nhân viên chăm sóc không có quyền phản hồi; người không phụ trách không trả lời được. */
    @Test
    void FR_067_onlyAssigneeHandles() throws Exception {
        long id = submitted(family(mMember), null, "COMPLAINT", "CARE");

        feedbackCommand(staffSession(Role.CAREGIVER), id, "start-handling", "{}")
                .andExpect(status().isForbidden());
        feedbackCommand(staff(nurse), id, "start-handling", "{}").andExpect(status().isForbidden());
        feedbackCommand(family(mMember), id, "start-handling", "{}").andExpect(status().isForbidden());
    }

    /** FR-060: giao lại bắt buộc lý do; người được giao (Điều dưỡng) xử lý và được báo. */
    @Test
    void FR_060_reassignWithReason() throws Exception {
        long id = submitted(family(mMember), null, "QUESTION", "HEALTH_MEDICATION");
        String tt = staff(floorManager);

        feedbackCommand(tt, id, "reassign", "{\"assigneeId\": " + nurse.accountId() + "}")
                .andExpect(status().isUnprocessableContent());
        feedbackCommand(
                        tt,
                        id,
                        "reassign",
                        "{\"assigneeId\": " + staffAccount(Role.CAREGIVER) + ", \"reason\": \"Nhờ hỗ trợ\"}")
                .andExpect(status().isUnprocessableContent());
        feedbackCommand(
                        tt,
                        id,
                        "reassign",
                        "{\"assigneeId\": " + nurse.accountId() + ", \"reason\": \"Điều dưỡng nắm thuốc\"}")
                .andExpect(status().isOk());

        assertThat(row(id).get("assignee_account_id")).isEqualTo(nurse.accountId());
        assertThat(notices(id, FeedbackNotice.Kind.ASSIGNED)
                        .getLast()
                        .notificationRequests()
                        .getFirst()
                        .recipientGroups())
                .containsExactly(new RecipientGroup.Staff(nurse.staffId(), "ASSIGNEE"));
        feedbackCommand(tt, id, "start-handling", "{}").andExpect(status().isForbidden());
        feedbackCommand(staff(nurse), id, "start-handling", "{}").andExpect(status().isOk());

        // Điều dưỡng không giao lại được (FR-060)
        feedbackCommand(
                        staff(nurse),
                        id,
                        "reassign",
                        "{\"assigneeId\": " + floorManager.accountId() + ", \"reason\": \"Trả lại\"}")
                .andExpect(status().isForbidden());
        // Quản lý viện giao lại được mọi phản hồi (FR-063)
        feedbackCommand(
                        staff(manager),
                        id,
                        "reassign",
                        "{\"assigneeId\": " + floorManager.accountId() + ", \"reason\": \"Trưởng tầng xử lý\"}")
                .andExpect(status().isOk());
        assertThat(row(id).get("assignee_account_id")).isEqualTo(floorManager.accountId());
    }

    /** FR-062: đổi mức có lý do, hạn tính lại từ thời điểm gửi; hạn mới đã qua thì quá hạn và leo thang ngay. */
    @Test
    void FR_062_changePriorityRecomputesDue() throws Exception {
        long id = submitted(family(mMember), null, "SUGGESTION", "CARE");
        Instant sent = at(id, "submitted_at");
        moveTo(sent.plus(Duration.ofHours(30)));
        String tt = staff(floorManager);

        feedbackCommand(tt, id, "change-priority", "{\"priority\": \"HIGH\"}")
                .andExpect(status().isUnprocessableContent());
        feedbackCommand(tt, id, "change-priority", "{\"priority\": \"HIGH\", \"reason\": \"Ảnh hưởng sức khỏe\"}")
                .andExpect(status().isOk());

        assertThat(row(id).get("priority")).isEqualTo("HIGH");
        assertThat(Instant.parse(
                        getJsonAs(tt, "/api/v1/feedback/" + id).get("dueAt").asText()))
                .isEqualTo(sent.plus(Duration.ofHours(24)));
        assertThat(row(id).get("overdue")).isEqualTo(true);
        assertThat(notices(id, FeedbackNotice.Kind.ESCALATED)).hasSize(1);
        // việc theo phút không leo thang lần hai cho cùng hạn
        runFeedbackTasks(now().plus(Duration.ofMinutes(1)));
        assertThat(notices(id, FeedbackNotice.Kind.ESCALATED)).hasSize(1);
    }

    /** FR-065: ghi chú bổ sung là mục mới, nội dung đã gửi không đổi. */
    @Test
    void FR_065_noteAppends() throws Exception {
        long id = submitted(family(mMember), null, "COMPLAINT", "CARE");
        String tt = staff(floorManager);

        feedbackCommand(tt, id, "add-note", "{\"content\": \"Đã kiểm tra phòng\"}")
                .andExpect(status().isOk());

        JsonNode detail = getJsonAs(tt, "/api/v1/feedback/" + id);
        assertThat(texts(detail, "CONTENT")).containsExactly("Phòng P201 không được dọn hai ngày");
        assertThat(texts(detail, "NOTE")).containsExactly("Đã kiểm tra phòng");
        assertThat(detail.get("content").asText()).isEqualTo("Phòng P201 không được dọn hai ngày");
    }

    /** Đổi api 2026-11-05: tên người gửi, thời điểm gửi, lệnh được phép theo người xem và trạng thái. */
    @Test
    void FR_066_detailHasSenderAndAllowedCommands() throws Exception {
        long id = submitted(family(mMember), null, "COMPLAINT", "CARE");

        JsonNode list = getJsonAs(staff(floorManager), "/api/v1/feedback");
        JsonNode row = null;
        for (JsonNode f : list) {
            if (f.get("id").asLong() == id) {
                row = f;
            }
        }
        assertThat(row).isNotNull();
        assertThat(row.get("familyMemberName").asText()).isEqualTo("Lê M");
        assertThat(Instant.parse(row.get("submittedAt").asText())).isEqualTo(at(id, "submitted_at"));

        assertThat(commands(staff(floorManager), id))
                .contains("start-handling", "reassign", "change-priority", "add-note")
                .doesNotContain("reply", "agree-to-close");
        assertThat(commands(staff(nurse), id)).isEmpty();
        assertThat(commands(family(mMember), id)).isEmpty();

        String tt = staff(floorManager);
        feedbackCommand(tt, id, "start-handling", "{}").andExpect(status().isOk());
        feedbackCommand(tt, id, "reply", reply())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REPLIED"))
                .andExpect(jsonPath("$.allowedCommands").isArray());
        assertThat(commands(family(mMember), id)).containsExactlyInAnyOrder("agree-to-close", "reopen");
        assertThat(commands(tt, id)).containsExactly("add-note");
    }

    /** FR-060, đổi api 2026-11-05: người nhận giao lại là Trưởng tầng, Điều dưỡng, Hành chính có phạm vi. */
    @Test
    void FR_060_assignableStaffInScope() throws Exception {
        long id = submitted(family(mMember), null, "COMPLAINT", "CARE");
        long outOfScopeNurse = staffAccount(Role.NURSING_STAFF);

        JsonNode list = getJsonAs(staff(floorManager), "/api/v1/feedback/" + id + "/assignable-staff");
        List<Long> accounts = new ArrayList<>();
        list.forEach(s -> accounts.add(s.get("accountId").asLong()));
        assertThat(accounts)
                .contains(floorManager.accountId(), nurse.accountId(), officeStaff.accountId())
                .doesNotContain(manager.accountId(), outOfScopeNurse);
        list.forEach(s -> {
            if (s.get("accountId").asLong() == nurse.accountId()) {
                assertThat(s.get("role").asText()).isEqualTo("NURSING_STAFF");
            }
        });

        feedbackCommand(
                        staff(floorManager),
                        id,
                        "reassign",
                        "{\"assigneeId\": " + outOfScopeNurse + ", \"reason\": \"Nhờ hỗ trợ\"}")
                .andExpect(status().isUnprocessableContent());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                "/api/v1/feedback/" + id + "/assignable-staff")
                        .header("Authorization", family(mMember)))
                .andExpect(status().isForbidden());
    }

    private List<String> commands(String authorization, long feedbackId) throws Exception {
        List<String> result = new ArrayList<>();
        getJsonAs(authorization, "/api/v1/feedback/" + feedbackId)
                .get("allowedCommands")
                .forEach(c -> result.add(c.asText()));
        return result;
    }

    private long staffAccount(Role role) {
        return users.staff(role).accountId();
    }

    private static List<String> texts(JsonNode detail, String type) {
        List<String> result = new ArrayList<>();
        detail.get("items").forEach(item -> {
            if (item.get("type").asText().equals(type)) {
                result.add(item.get("content").asText());
            }
        });
        return result;
    }

    private static List<Long> ids(JsonNode list) {
        List<Long> result = new ArrayList<>();
        list.forEach(item -> result.add(item.get("id").asLong()));
        return result;
    }
}
