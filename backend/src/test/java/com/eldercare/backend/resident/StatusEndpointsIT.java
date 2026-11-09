package com.eldercare.backend.resident;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.resident.api.dto.ResidentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 001 T050 (US4; contracts/api-001.yaml, tag {@code status}): đường HTTP của lệnh trạng thái. Thân thiếu lý do 422,
 * điều kiện chưa thỏa 409 {@code UNMET_CONDITIONS} liệt kê đủ, cặp ngoài bảng 409 {@code INVALID_TRANSITION}, lệnh
 * thành công trả trạng thái mới kèm dòng lịch sử; {@code allowedCommands} chỉ gồm lệnh hợp trạng thái và vai trò.
 */
@AutoConfigureMockMvc
@WithMockUser
class StatusEndpointsIT extends StatusTestBase {

    @Autowired
    MockMvc mvc;

    private static final String BODY = "{\"reason\":\"Gia đình đón về cuối tuần\",\"details\":{}}";

    @Test
    void US4_grantTemporaryAbsence_returnsNewStatusAndHistory() throws Exception {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(office.user());

        mvc.perform(post("/api/v1/residents/" + id + "/grant-temporary-absence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TEMPORARILY_ABSENT"))
                .andExpect(jsonPath("$.history.command").value("GRANT_TEMPORARY_ABSENCE"))
                .andExpect(jsonPath("$.history.from").value("IN_RESIDENCE"))
                // lý do thuộc nhóm HEALTH (Opus #6): Hành chính không thấy lại ở phản hồi; xem
                // StatusHistoryVisibilityIT
                .andExpect(jsonPath("$.history.reason").doesNotExist())
                .andExpect(jsonPath("$.history.hiddenFields", org.hamcrest.Matchers.hasItem("reason")))
                .andExpect(jsonPath("$.history.bySystem").value(false));

        mvc.perform(get("/api/v1/residents/" + id + "/status-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[-1:].to").value("TEMPORARILY_ABSENT"));
    }
}
