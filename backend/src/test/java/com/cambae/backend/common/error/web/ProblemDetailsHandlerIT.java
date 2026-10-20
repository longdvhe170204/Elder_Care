package com.cambae.backend.common.error.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.testsupport.SampleController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/** FR-006, FR-021, FR-040, R-03 (000 T015, T016): lỗi nghiệp vụ trả application/problem+json có errorCode. */
@AutoConfigureMockMvc
@WithMockUser
@Import(SampleController.class)
class ProblemDetailsHandlerIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @ParameterizedTest
    @CsvSource({
        "condition, 409, CONDITION_NOT_MET",
        "permission, 403, PERMISSION_DENIED",
        "processed, 409, ALREADY_PROCESSED",
        "version, 412, VERSION_MISMATCH",
        "reason, 422, REASON_REQUIRED",
        "invalid, 422, INVALID_VALUE",
        "missing, 404, NOT_FOUND"
    })
    void R_03_businessError_mapsToStatusAndErrorCode(String kind, int status, String errorCode) throws Exception {
        mvc.perform(get("/api/v1/test-support/errors/" + kind))
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.errorCode").value(errorCode));
    }

    @Test
    void FR_006_unmetConditions_listBrCodeAndDescription() throws Exception {
        mvc.perform(get("/api/v1/test-support/errors/condition"))
                .andExpect(jsonPath("$.unmetConditions[0].code").value("BR-M03-06"))
                .andExpect(jsonPath("$.unmetConditions[0].description").value("Giường chưa Trống"));
    }

    /** 002 FR-043 (T014): lỗi quyền kèm lớp không thỏa và giấy phép. */
    @Test
    void FR_043_permissionDenied_carriesFailedLayerAndLicense() throws Exception {
        mvc.perform(get("/api/v1/test-support/errors/legal"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("LEGAL_CONDITION_NOT_MET"))
                .andExpect(jsonPath("$.failedLayer").value("LEGAL"))
                .andExpect(jsonPath("$.license.name").value("Chứng chỉ hành nghề"))
                .andExpect(jsonPath("$.license.expiryDate").value("2026-09-30"));
    }

    @Test
    void FR_021_versionMismatch_returnsCurrentValue() throws Exception {
        mvc.perform(get("/api/v1/test-support/errors/version"))
                .andExpect(jsonPath("$.currentValue.currentValue").value(20));
    }

    @Test
    void R_03_notFound_doesNotRevealWhy() throws Exception {
        mvc.perform(get("/api/v1/test-support/errors/missing"))
                .andExpect(jsonPath("$.detail").value("Không tìm thấy"));
    }

    @Test
    void R_03_frameworkError_alsoHasErrorCode() throws Exception {
        mvc.perform(get("/api/v1/test-support/no-such-endpoint"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }
}
