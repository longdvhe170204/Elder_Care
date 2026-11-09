package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.resident.api.dto.Condition;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;

/**
 * 004 T014 (US1; FR-004, FR-075; research T-09): danh sách điều kiện tiếp nhận (a) → (f) của 004 và kết quả lệnh Hoàn
 * tất tiếp nhận của 001 dùng cùng một nguồn nên luôn khớp: điều kiện Chưa đạt trong danh sách chính là điều kiện lệnh
 * từ chối. (a) do 001, (d) do 003, (f) do 012 trả (ở đây bản giả {@code otherModules}); (b), (c), (e) do 004 kiểm.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_004_ConditionsMatchCommandIT extends M02TestBase {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    MockMvc mvc;

    private long application(long residentId) throws Exception {
        user.set(office.user());
        mvc.perform(post("/api/v1/admission-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationBody(residentId)))
                .andExpect(status().isCreated());
        return applicationIdOf(residentId);
    }

    private JsonNode conditions(long applicationId) throws Exception {
        return JSON.readTree(
                mvc.perform(get("/api/v1/admission-applications/" + applicationId + "/admission-conditions"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
    }

    private static List<String> codesWith(JsonNode list, String status) {
        return StreamSupport.stream(list.get("conditions").spliterator(), false)
                .filter(line -> status.equals(line.get("status").asString()))
                .map(line -> line.get("code").asString())
                .toList();
    }

    private static JsonNode line(JsonNode list, String code) {
        return StreamSupport.stream(list.get("conditions").spliterator(), false)
                .filter(line -> code.equals(line.get("code").asString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Không có dòng " + code));
    }

    private List<String> commandUnmet(long residentId) {
        return statusService.preview(residentId, StatusCommand.COMPLETE_ADMISSION).stream()
                .map(Condition::code)
                .toList();
    }

    @Test
    void FR_004_listHasSixConditionsInOrder_andUnmetMatchTheCommand() throws Exception {
        long residentId = admissionResident();
        long id = application(residentId);

        JsonNode list = conditions(id);

        assertThat(list.get("conditions")).hasSize(6);
        assertThat(codesWith(list, "NOT_MET"))
                .containsExactlyInAnyOrderElementsOf(commandUnmet(residentId))
                .contains(
                        "INITIAL_ASSESSMENT_NOT_CONFIRMED", "ADM_B_CONTRACT", "ADM_C_DEPOSIT", "ADM_E_CONTRACT_LEVEL");
        BusinessException blocked = failure(() -> statusService.completeAdmission(
                target(residentId), new Reason("Thử"), JsonNodeFactory.instance.objectNode()));
        assertThat(blocked.errorCode()).isEqualTo("UNMET_CONDITIONS");
        assertThat(unmetCodes(blocked)).containsExactlyInAnyOrderElementsOf(codesWith(list, "NOT_MET"));
    }
}
