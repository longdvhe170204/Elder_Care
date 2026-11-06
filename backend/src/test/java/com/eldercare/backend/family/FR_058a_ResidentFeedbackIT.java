package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.family.api.events.FeedbackNotice;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 012 T063 (US9; FR-058, FR-058a, FR-066; quickstart K6 → K8): người cao tuổi gửi phản hồi qua kênh của mình; Hành chính,
 * Điều dưỡng có phạm vi ghi thay khi người cao tuổi nói trực tiếp (có hay không có tài khoản) và ghi thay Đồng ý đóng,
 * Mở lại; trả lời nhóm "sức khỏe, thuốc" trên kênh chỉ phần chung, người phụ trách được nhắc trả lời trực tiếp.
 */
class FR_058a_ResidentFeedbackIT extends FeedbackTestBase {

    @Test
    void FR_058a_residentSubmitsThroughChannel() throws Exception {
        String self = residentSession(a);
        long id = channelSubmitted(self, "COMPLAINT", "CARE", null);

        assertThat(row(id).get("sender_type")).isEqualTo("RESIDENT");
        assertThat(row(id).get("source")).isEqualTo("RESIDENT_CHANNEL");
        assertThat(row(id).get("family_member_id")).isNull();
        assertThat(row(id).get("recorded_on_behalf_by")).isNull();
        assertThat(row(id).get("assignee_account_id")).isEqualTo(floorManager.accountId());
        assertThat(idsOf(getJsonAs(self, "/api/v1/resident-channel/feedback"))).contains(id);
        getAs(self, "/api/v1/feedback/" + id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderType").value("RESIDENT"))
                .andExpect(jsonPath("$.content").value("Tôi muốn được tắm buổi tối"));
        assertThat(notices(id, FeedbackNotice.Kind.ASSIGNED)).hasSize(1);
    }

    @Test
    void FR_058a_residentCannotSendForFamilyOrOthers() throws Exception {
        String self = residentSession(a);
        long other = resident(com.eldercare.backend.resident.api.dto.ResidentStatus.IN_RESIDENCE);
        postAs(self, "/api/v1/feedback", submission(other, null, "COMPLAINT", "CARE", "Không phải của tôi"))
                .andExpect(status().isForbidden());
    }

    /** K7: Điều dưỡng ghi thay E không có tài khoản; Trưởng tầng trả lời; Điều dưỡng ghi thay Đồng ý đóng. */
    @Test
    void FR_058a_nurseRecordsOnBehalfAndClosesOnBehalf() throws Exception {
        String d = staff(nurse);
        long id = id(onBehalfSubmit(d, a, "SUGGESTION", "CARE").andExpect(status().isCreated()));

        assertThat(row(id).get("sender_type")).isEqualTo("RESIDENT");
        assertThat(row(id).get("source")).isEqualTo("RECORDED_DIRECTLY");
        assertThat(row(id).get("recorded_on_behalf_by")).isEqualTo(nurse.accountId());
        assertThat(row(id).get("family_member_id")).isNull();

        String tt = staff(floorManager);
        feedbackCommand(tt, id, "start-handling", "{}").andExpect(status().isOk());
        feedbackCommand(tt, id, "reply", reply()).andExpect(status().isOk());
        assertThat(commands(d, id)).contains("agree-to-close", "reopen");
        feedbackCommand(d, id, "agree-to-close", "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
        assertThat(row(id).get("close_method")).isEqualTo("RECORDED_ON_BEHALF");
    }

    @Test
    void FR_058a_officeRecordsOnBehalfEvenWithAccount() throws Exception {
        residentSession(a);
        long id = id(onBehalfSubmit(office, a, "QUESTION", "EATING").andExpect(status().isCreated()));
        assertThat(row(id).get("recorded_on_behalf_by")).isEqualTo(officeStaff.accountId());

        String tt = staff(floorManager);
        feedbackCommand(tt, id, "start-handling", "{}").andExpect(status().isOk());
        feedbackCommand(tt, id, "reply", reply()).andExpect(status().isOk());
        // Người cao tuổi có tài khoản tự Mở lại trên kênh; nhân viên chỉ ghi thay khi không có tài khoản (FR-058a (c))
        feedbackCommand(office, id, "reopen", "{\"reason\": \"Bà Lan nói vẫn chưa được\"}")
                .andExpect(status().isForbidden());
        feedbackCommand(residentSession(a), id, "reopen", "{}").andExpect(status().isUnprocessableContent());
        feedbackCommand(residentSession(a), id, "reopen", "{\"reason\": \"Vẫn chưa được\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REOPENED"));
    }

    @Test
    void FR_058a_onlyOfficeAndNursingStaffRecordOnBehalf() throws Exception {
        onBehalfSubmit(staff(floorManager), a, "SUGGESTION", "CARE").andExpect(status().isForbidden());
        onBehalfSubmit(staff(users.staff(Role.CAREGIVER)), a, "SUGGESTION", "CARE")
                .andExpect(status().isForbidden());
    }

    /** K6: người cao tuổi Đồng ý đóng trên phản hồi của mình. */
    @Test
    void FR_066_residentAgreesToClose() throws Exception {
        String self = residentSession(a);
        long id = channelSubmitted(self, "SUGGESTION", "CARE", null);
        String tt = staff(floorManager);
        feedbackCommand(tt, id, "start-handling", "{}").andExpect(status().isOk());
        feedbackCommand(tt, id, "reply", reply()).andExpect(status().isOk());

        assertThat(commands(residentSession(a), id)).containsExactlyInAnyOrder("agree-to-close", "reopen");
        feedbackCommand(residentSession(a), id, "agree-to-close", "{}").andExpect(status().isOk());
        assertThat(row(id).get("close_method")).isEqualTo("SENDER");
        // Nhân viên không ghi thay được khi người cao tuổi tự gửi qua kênh của mình
        assertThat(commands(staff(nurse), id)).doesNotContain("agree-to-close");
    }

    /** K8: nhóm sức khỏe, thuốc: người phụ trách được nhắc trả lời trực tiếp; kênh chỉ thấy phần chung. */
    @Test
    void FR_058a_healthReplyOnlyGeneralOnChannel() throws Exception {
        String self = residentSession(a);
        long id = channelSubmitted(self, "QUESTION", "HEALTH_MEDICATION", null);
        String tt = staff(floorManager);
        getAs(tt, "/api/v1/feedback/" + id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderHealthRightEffective").value(false));
        feedbackCommand(tt, id, "start-handling", "{}").andExpect(status().isOk());
        feedbackCommand(tt, id, "reply", reply()).andExpect(status().isOk());

        JsonNode detail = getJsonAs(residentSession(a), "/api/v1/feedback/" + id);
        List<String> types = new ArrayList<>();
        detail.get("items").forEach(i -> types.add(i.get("type").asText()));
        assertThat(types).containsExactly("CONTENT");
        assertThat(detail.get("healthReplyHidden").asBoolean()).isTrue();

        List<FeedbackNotice> replied = notices(id, FeedbackNotice.Kind.REPLIED);
        assertThat(replied).hasSize(1);
        assertThat(replied.getFirst().recipient()).isEqualTo(new RecipientGroup.Resident(a));
        assertThat(replied.getFirst().healthInformation()).isFalse();
    }

    private List<String> commands(String authorization, long feedbackId) throws Exception {
        List<String> result = new ArrayList<>();
        getJsonAs(authorization, "/api/v1/feedback/" + feedbackId)
                .get("allowedCommands")
                .forEach(c -> result.add(c.asText()));
        return result;
    }
}
