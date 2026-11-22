package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.resident.api.dto.CareLevel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * FR-043j (R20): yêu cầu đổi mức chăm sóc phải mang mức của lần đánh giá gần nhất đã xác nhận, và mức đó khác mức hợp đồng;
 * sai thì 409 {@code CARE_LEVEL_NOT_FROM_ASSESSMENT} khi lập, gửi duyệt. Phản hồi có mức theo đánh giá để giao diện điền sẵn.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_043j_CareLevelFromAssessmentIT extends ChangeTestBase {

    /** Thân đổi mức không tự tạo đánh giá (dựng {@code request} rồi đặt lại mức sau khi helper đã đánh giá). */
    private ObjectNode careLevelBody(Setup s, String level) {
        ObjectNode body = (ObjectNode) JSON.readTree("""
                {"contractId": %d, "changeTypes": ["CARE_LEVEL"], "newValue": {"careLevel": "%s"},
                 "requestedEffectiveDate": "%s", "reason": "%s"}""".formatted(s.contractId(), level, today().plusDays(3), REASON));
        body.set("representativeConfirmation", confirmation(today(), representativeOf(s.residentId())));
        return body;
    }

    @Test
    void R20_noReassessmentDifferentFromContract_409() throws Exception {
        Setup s = activeSetup();
        user.set(office.user());
        MvcResult result = createRaw("/api/v1/residency-change-requests", careLevelBody(s, "REGULAR"))
                .andExpect(status().isConflict())
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).contains("CARE_LEVEL_NOT_FROM_ASSESSMENT");
    }

    @Test
    void R20_levelDiffersFromAssessment_409_matchingLevel_created() throws Exception {
        Setup s = activeSetup();
        assessedAs(s.contractId(), CareLevel.SPECIAL);

        user.set(office.user());
        createRaw("/api/v1/residency-change-requests", careLevelBody(s, "REGULAR"))
                .andExpect(status().isConflict());
        JsonNode created = createChange(careLevelBody(s, "SPECIAL"));
        assertThat(created.get("assessedCareLevel").get("careLevel").asString()).isEqualTo("SPECIAL");
    }

    @Test
    void R20_newerReassessmentAfterCreation_submitBlocked() throws Exception {
        Setup s = activeSetup();
        assessedAs(s.contractId(), CareLevel.REGULAR);
        long id = createChange(careLevelBody(s, "REGULAR"))
                .get("approvalRequestId")
                .asLong();
        // Bản giả của 001 không chuyển yêu cầu cũ sang Được thay thế, nên yêu cầu còn Nháp khi có đánh giá lại mới
        assessedAs(s.contractId(), CareLevel.SPECIAL);

        user.set(office.user());
        MvcResult result =
                generic(id, "submit", "{}").andExpect(status().isConflict()).andReturn();
        assertThat(result.getResponse().getContentAsString()).contains("CARE_LEVEL_NOT_FROM_ASSESSMENT");
    }
}
