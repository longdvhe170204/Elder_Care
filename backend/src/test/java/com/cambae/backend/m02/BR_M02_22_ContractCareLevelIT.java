package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m02.api.dto.ContractStatus;
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
 * 004 T014 (US1; BR-M02-22, FR-004 (e), FR-005, Q-300): mức chăm sóc của hợp đồng Hiệu lực phải trùng mức hiện hành
 * (từ đánh giá đầu vào) thì mới Hoàn tất tiếp nhận; chưa có mức hiện hành thì Chưa đạt. Lệch loại lưu trú chỉ cảnh báo.
 */
@AutoConfigureMockMvc
@WithMockUser
class BR_M02_22_ContractCareLevelIT extends M02TestBase {

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

    private static boolean hasWarning(JsonNode list, String code) {
        return StreamSupport.stream(list.get("warnings").spliterator(), false)
                .anyMatch(warning -> code.equals(warning.get("code").asString()));
    }

    private static String statusOf(JsonNode list, String code) {
        return StreamSupport.stream(list.get("conditions").spliterator(), false)
                .filter(line -> code.equals(line.get("code").asString()))
                .map(line -> line.get("status").asString())
                .findFirst()
                .orElse("ABSENT");
    }

    @Test
    void BR_M02_22_contractLevelDiffersFromCurrentLevel_blocksCommand_andWarns() throws Exception {
        long residentId = admissionResident();
        long id = application(residentId);
        confirmedAssessment(residentId, CareLevel.BASIC);
        contracts.active(residentId, CareLevel.SPECIAL, today());

        JsonNode list = conditions(id);

        assertThat(statusOf(list, "ADM_E_CONTRACT_LEVEL")).isEqualTo("NOT_MET");
        assertThat(hasWarning(list, "CONTRACT_CARE_LEVEL_MISMATCH")).isTrue();
        BusinessException blocked = failure(() -> statusService.completeAdmission(
                target(residentId), new Reason("Thử"), JsonNodeFactory.instance.objectNode()));
        assertThat(unmetCodes(blocked)).containsExactly("ADM_E_CONTRACT_LEVEL");
        assertThat(currentStatus(residentId)).isEqualTo(ResidentStatus.ADMISSION_IN_PROGRESS);
    }

    @Test
    void BR_M02_22_sameLevel_isMetAndAdmissionCompletes() throws Exception {
        long residentId = admissionResident();
        long id = application(residentId);
        readyToAdmit(residentId);

        JsonNode list = conditions(id);
        assertThat(statusOf(list, "ADM_E_CONTRACT_LEVEL")).isEqualTo("MET");
        assertThat(hasWarning(list, "CONTRACT_CARE_LEVEL_MISMATCH")).isFalse();

        var outcome = statusService.completeAdmission(
                target(residentId), new Reason("Đủ điều kiện"), JsonNodeFactory.instance.objectNode());
        assertThat(outcome.status()).isEqualTo(ResidentStatus.IN_RESIDENCE);
    }

    @Test
    void BR_M02_22_currentLevelChanges_conditionFollowsTheCurrentLevel() throws Exception {
        long residentId = admissionResident();
        long id = application(residentId);
        readyToAdmit(residentId);
        jdbc.update("UPDATE resident SET current_care_level = 'REGULAR' WHERE id = ?", residentId);

        assertThat(statusOf(conditions(id), "ADM_E_CONTRACT_LEVEL")).isEqualTo("NOT_MET");
    }

    @Test
    void BR_M02_22_noCurrentLevel_isNotMet() throws Exception {
        long residentId = admissionResident();
        long id = application(residentId);
        contracts.active(residentId, CareLevel.BASIC, today());

        assertThat(statusOf(conditions(id), "ADM_E_CONTRACT_LEVEL")).isEqualTo("NOT_MET");
    }

    @Test
    void FR_005_residenceTypeMismatch_onlyWarns() throws Exception {
        long residentId = admissionResident();
        long id = application(residentId);
        confirmedAssessment(residentId, CareLevel.BASIC);
        contracts.contract(
                residentId, ContractStatus.ACTIVE, ResidenceType.SHORT_TERM_RESIDENTIAL, CareLevel.BASIC, today());

        JsonNode list = conditions(id);

        assertThat(hasWarning(list, "RESIDENCE_TYPE_MISMATCH")).isTrue();
        assertThat(statusOf(list, "ADM_B_CONTRACT")).isEqualTo("MET");
        assertThat(statusOf(list, "ADM_E_CONTRACT_LEVEL")).isEqualTo("MET");
    }
}
