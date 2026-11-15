package com.eldercare.backend.common.error.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.testsupport.SampleController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/** FR-006, FR-021, FR-040, R-03 (000 T015, T016): lỗi nghiệp vụ trả application/problem+json có errorCode. */
@AutoConfigureMockMvc
@WithMockUser
@Import(SampleController.class)
class ProblemDetailsHandlerIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Test
    void FR_006_unmetConditions_listBrCodeAndDescription() throws Exception {
        mvc.perform(get("/api/v1/test-support/errors/condition"))
                .andExpect(jsonPath("$.unmetConditions[0].code").value("BR-M03-06"))
                .andExpect(jsonPath("$.unmetConditions[0].description").value("Giường chưa Trống"))
                .andExpect(jsonPath("$.unmetConditions[0].basis").doesNotExist())
                .andExpect(jsonPath("$.unmetConditions[0].waivable").doesNotExist());
    }
}
