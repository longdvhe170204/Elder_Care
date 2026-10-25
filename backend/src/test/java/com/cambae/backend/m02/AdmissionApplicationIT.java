package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    void FR_001_secondOpenApplication_isRejected409() throws Exception {
        long residentId = admissionResident();
        user.set(office.user());
        mvc.perform(post("/api/v1/admission-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationBody(residentId)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/admission-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationBody(residentId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("APPLICATION_ALREADY_EXISTS"));

        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM admission_application WHERE resident_id = ?", Integer.class, residentId))
                .isEqualTo(1);
    }

    @Test
    void FR_001_applicationAfterPreviousOneClosed_isAllowed() throws Exception {
        long residentId = admissionResident();
        user.set(office.user());
        mvc.perform(post("/api/v1/admission-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationBody(residentId)))
                .andExpect(status().isCreated());
        jdbc.update("UPDATE admission_application SET status = 'CANCELLED' WHERE resident_id = ?", residentId);

        mvc.perform(post("/api/v1/admission-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationBody(residentId)))
                .andExpect(status().isCreated());
    }

    @Test
    void FR_001_databaseAllowsOnlyOneOpenApplicationPerResident() {
        long residentId = admissionResident();
        String insert = "INSERT INTO admission_application (resident_id, application_date, desired_residence_type,"
                + " expected_care_level, status, created_by, created_at) VALUES (?, CURDATE(), 'DAY_CARE', 'BASIC',"
                + " ?, 1, NOW(6))";
        jdbc.update(insert, residentId, "IN_PROGRESS");

        assertThatThrownBy(() -> jdbc.update(insert, residentId, "WAITING"))
                .hasMessageContaining("uq_admission_application_open");
        jdbc.update(insert, residentId, "CANCELLED");
    }

    @Test
    void FR_001_residentNotInAdmission_isRejected409() throws Exception {
        long residentId = newResident();
        user.set(office.user());

        mvc.perform(post("/api/v1/admission-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationBody(residentId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RESIDENT_NOT_ADMISSION_IN_PROGRESS"));
    }

    @Test
    void FR_001_missingRequiredField_is400() throws Exception {
        long residentId = admissionResident();
        user.set(office.user());

        mvc.perform(post("/api/v1/admission-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"residentId\":" + residentId + ",\"applicationDate\":\"" + today() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void FR_006_managerCanViewButNotCreate_caregiverCannotView() throws Exception {
        long residentId = admissionResident();
        user.set(office.user());
        mvc.perform(post("/api/v1/admission-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationBody(residentId)))
                .andExpect(status().isCreated());
        long id = applicationIdOf(residentId);

        user.set(
                users.staff(com.cambae.backend.common.Role.NURSING_HOME_MANAGER).user());
        mvc.perform(get("/api/v1/admission-applications/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mvc.perform(post("/api/v1/admission-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationBody(residentId)))
                .andExpect(status().isForbidden());

        user.set(users.staff(com.cambae.backend.common.Role.KITCHEN_STAFF).user());
        mvc.perform(get("/api/v1/admission-applications/" + id)).andExpect(status().isForbidden());
    }

    @Test
    void FR_006_listFiltersByStatus() throws Exception {
        long residentId = admissionResident();
        user.set(office.user());
        mvc.perform(post("/api/v1/admission-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationBody(residentId)))
                .andExpect(status().isCreated());
        long id = applicationIdOf(residentId);

        mvc.perform(get("/api/v1/admission-applications").param("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").exists());
        mvc.perform(get("/api/v1/admission-applications").param("status", "CANCELLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").doesNotExist());
    }
}
