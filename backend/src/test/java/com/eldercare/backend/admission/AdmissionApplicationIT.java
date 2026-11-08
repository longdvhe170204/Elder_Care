package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 004 T013 (US1; FR-001, FR-006; contracts/api-004.yaml, tag {@code applications}): lập lượt đăng ký. Mỗi người chỉ một
 * lượt mở (cột sinh {@code UNIQUE}); chỉ người Đang tiếp nhận; Hành chính lập, Quản lý viện chỉ xem.
 */
@AutoConfigureMockMvc
@WithMockUser
class AdmissionApplicationIT extends M02TestBase {

    @Autowired
    MockMvc mvc;

    @Test
    void FR_001_officeCreatesApplication_inProgressWithEtagAndAudit() throws Exception {
        long residentId = admissionResident();
        user.set(office.user());

        mvc.perform(post("/api/v1/admission-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationBody(residentId)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.residentId").value(residentId))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.desiredResidenceType").value("LONG_TERM_RESIDENTIAL"))
                .andExpect(jsonPath("$.expectedCareLevel").value("BASIC"))
                .andExpect(jsonPath("$.contactPerson").value("Nguyễn Thị Liên Hệ"))
                .andExpect(jsonPath("$.admissionOpinion", hasSize(0)));

        long id = applicationIdOf(residentId);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type = 'M02.ADMISSION_APPLICATION'"
                                + " AND entity_id = ? AND action = 'M02_APPLICATION_CREATED'",
                        Integer.class,
                        String.valueOf(id)))
                .isEqualTo(1);
    }
}
