package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Reason;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.api.dto.ContractStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.node.JsonNodeFactory;

/**
 * 004 T013 (US1; FR-003): tiến trình theo tám bước của 6.1, suy ra từ dữ liệu có sẵn (đánh giá của 001, hợp đồng, danh
 * sách điều kiện, trạng thái lượt), không lưu và không nhập tay.
 */
@AutoConfigureMockMvc
@WithMockUser
class ProgressIT extends M02TestBase {

    private static final List<String> STEPS = List.of(
            "APPLICATION",
            "INFORMATION_COLLECTION",
            "ASSESSMENT",
            "RESIDENCE_TYPE_DETERMINATION",
            "CARE_DETERMINATION",
            "SERVICE_DETERMINATION",
            "CONDITION_CHECK",
            "ADMISSION");

    @Autowired
    MockMvc mvc;

    private long newApplication(long residentId) throws Exception {
        user.set(office.user());
        mvc.perform(post("/api/v1/admission-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationBody(residentId)))
                .andExpect(status().isCreated());
        return applicationIdOf(residentId);
    }

    private List<String> doneSteps(long applicationId) throws Exception {
        String json = mvc.perform(get("/api/v1/admission-applications/" + applicationId + "/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps[*].name").value(contains(STEPS.toArray())))
                .andReturn()
                .getResponse()
                .getContentAsString();
        var steps = new tools.jackson.databind.json.JsonMapper().readTree(json).get("steps");
        List<String> done = new java.util.ArrayList<>();
        steps.forEach(step -> {
            if (step.get("done").asBoolean()) {
                done.add(step.get("name").asString());
            }
        });
        return done;
    }

    @Test
    void FR_003_newApplication_onlyRegistrationStepIsDone() throws Exception {
        long residentId = admissionResident();
        long id = newApplication(residentId);

        assertThat(doneSteps(id)).containsExactly("APPLICATION");
    }

    @Test
    void FR_003_stepsFollowAssessmentContractAndConditions() throws Exception {
        long residentId = admissionResident();
        long id = newApplication(residentId);

        confirmedAssessment(residentId, CareLevel.BASIC);
        assertThat(doneSteps(id)).containsExactly("APPLICATION", "ASSESSMENT");

        contracts.contract(
                residentId, ContractStatus.DRAFT, ResidenceType.LONG_TERM_RESIDENTIAL, CareLevel.BASIC, today());
        assertThat(doneSteps(id))
                .containsExactly(
                        "APPLICATION",
                        "ASSESSMENT",
                        "RESIDENCE_TYPE_DETERMINATION",
                        "CARE_DETERMINATION",
                        "SERVICE_DETERMINATION");

        contracts.reset();
        contracts.active(residentId, CareLevel.BASIC, today());
        assertThat(doneSteps(id))
                .containsExactly(
                        "APPLICATION",
                        "ASSESSMENT",
                        "RESIDENCE_TYPE_DETERMINATION",
                        "CARE_DETERMINATION",
                        "SERVICE_DETERMINATION",
                        "CONDITION_CHECK");
    }

    @Test
    void FR_003_afterCompleteAdmission_admissionStepIsDone() throws Exception {
        long residentId = admissionResident();
        long id = newApplication(residentId);
        readyToAdmit(residentId);
        user.set(office.user());

        statusService.completeAdmission(
                target(residentId), new Reason("Hoàn tất"), JsonNodeFactory.instance.objectNode());

        assertThat(doneSteps(id)).contains("APPLICATION", "CONDITION_CHECK", "ADMISSION");
    }

    @Test
    void FR_003_unknownApplication_is404() throws Exception {
        user.set(office.user());

        mvc.perform(get("/api/v1/admission-applications/999999999/progress")).andExpect(status().isNotFound());
    }
}
