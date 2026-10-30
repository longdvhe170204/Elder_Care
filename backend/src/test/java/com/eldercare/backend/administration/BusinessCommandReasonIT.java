package com.eldercare.backend.administration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.sample.SampleService;
import com.eldercare.backend.administration.sample.SampleUsers;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.testsupport.SampleDataController;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * FR-005, FR-007, SC-008 (000 T044): lệnh nhóm 2 thiếu lý do trả 422 REASON_REQUIRED trước khi chạm dữ liệu; tài
 * nguyên nhóm 2, 3 không có endpoint PUT, PATCH, DELETE.
 */
@AutoConfigureMockMvc
@WithMockUser
@Import({SampleDataController.class, SampleUsers.class})
class BusinessCommandReasonIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    SampleService samples;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    RequestMappingHandlerMapping mappings;

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"reason\":null}", "{\"reason\":\"   \"}"})
    void SC_008_commandWithoutReason_isRejectedBeforeTouchingData(String body) throws Exception {
        long caseId = samples.createCase(samples.createCatalogItem("Lý do " + UUID.randomUUID()), 1);

        mvc.perform(post("/api/v1/test-support/sample-cases/" + caseId + "/cancel")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));

        assertThat(jdbc.queryForObject("SELECT status FROM sample_case WHERE id = ?", String.class, caseId))
                .isEqualTo("OPEN");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type = 'M15.SAMPLE_CASE' AND entity_id = ?"
                                + " AND action = 'SAMPLE_CASE_CANCELLED'",
                        Integer.class,
                        String.valueOf(caseId)))
                .isZero();
    }

    @Test
    void FR_005_businessAndRecordResources_haveNoPutPatchDelete() {
        List<String> violations = mappings.getHandlerMethods().keySet().stream()
                .filter(info -> info.getPatternValues().stream().anyMatch(p -> p.contains("/sample-cases")))
                .filter(info -> info.getMethodsCondition().getMethods().stream()
                        .anyMatch(m -> m == RequestMethod.PUT || m == RequestMethod.PATCH || m == RequestMethod.DELETE))
                .map(Object::toString)
                .toList();

        assertThat(violations).isEmpty();
    }
}
