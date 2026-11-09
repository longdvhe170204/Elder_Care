package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.family.api.events.FeedbackNotice;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

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

    private List<String> commands(String authorization, long feedbackId) throws Exception {
        List<String> result = new ArrayList<>();
        getJsonAs(authorization, "/api/v1/feedback/" + feedbackId)
                .get("allowedCommands")
                .forEach(c -> result.add(c.asText()));
        return result;
    }
}
