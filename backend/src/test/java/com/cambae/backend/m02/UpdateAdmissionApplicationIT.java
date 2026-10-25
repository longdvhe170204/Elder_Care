package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.test.web.servlet.MvcResult;

/**
 * 004 T013 (US1; FR-001, BR-M02-15): lệnh Cập nhật lượt đăng ký cần {@code If-Match} và lý do, chỉ khi lượt Đang xử lý
 * hoặc Đang chờ; ngày đăng ký không đổi khi đã có hồ sơ chờ (lượt Đang chờ); lượt Đã tiếp nhận, Đã hủy không sửa.
 */
@AutoConfigureMockMvc
@WithMockUser
class UpdateAdmissionApplicationIT extends M02TestBase {

    @Autowired
    MockMvc mvc;

    private String updateBody(String applicationDate, String needs) {
        return """
                {"residentId":0,"applicationDate":"%s","contactPerson":"Người liên hệ mới","needs":"%s",
                 "desiredResidenceType":"SHORT_TERM_RESIDENTIAL","expectedCareLevel":"REGULAR",
                 "note":"Ghi chú mới","reason":"Gia đình đổi nhu cầu"}""".formatted(applicationDate, needs);
    }

    /** Lượt mới tạo qua HTTP; trả ETag. */
    private String create(long residentId) throws Exception {
        user.set(office.user());
        MvcResult created = mvc.perform(post("/api/v1/admission-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationBody(residentId)))
                .andExpect(status().isCreated())
                .andReturn();
        return created.getResponse().getHeader("ETag");
    }

    @Test
    void FR_001_update_changesFieldsKeepsDateAndRecordsBeforeAfter() throws Exception {
        long residentId = admissionResident();
        String etag = create(residentId);
        long id = applicationIdOf(residentId);

        MvcResult updated = mvc.perform(post("/api/v1/admission-applications/" + id + "/update")
                        .header("If-Match", etag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(today().toString(), "Nhu cầu mới")))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andReturn();

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        "/api/v1/admission-applications/" + id))
                .andExpect(jsonPath("$.needs").value("Nhu cầu mới"))
                .andExpect(jsonPath("$.desiredResidenceType").value("SHORT_TERM_RESIDENTIAL"))
                .andExpect(jsonPath("$.expectedCareLevel").value("REGULAR"));

        assertThat(updated.getResponse().getHeader("ETag")).isNotEqualTo(etag);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type = 'M02.ADMISSION_APPLICATION' AND entity_id = ?"
                                + " AND action = 'M02_APPLICATION_UPDATED' AND reason = 'Gia đình đổi nhu cầu'"
                                + " AND data_before IS NOT NULL AND data_after IS NOT NULL",
                        Integer.class,
                        String.valueOf(id)))
                .isEqualTo(1);
    }

    @Test
    void FR_001_updateWithoutReason_is422() throws Exception {
        long residentId = admissionResident();
        String etag = create(residentId);
        long id = applicationIdOf(residentId);

        mvc.perform(post("/api/v1/admission-applications/" + id + "/update")
                        .header("If-Match", etag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(today().toString(), "x").replace("Gia đình đổi nhu cầu", "  ")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));
    }

    @Test
    void FR_001_staleIfMatch_is412() throws Exception {
        long residentId = admissionResident();
        String etag = create(residentId);
        long id = applicationIdOf(residentId);
        mvc.perform(post("/api/v1/admission-applications/" + id + "/update")
                        .header("If-Match", etag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(today().toString(), "Lần một")))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/admission-applications/" + id + "/update")
                        .header("If-Match", etag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(today().toString(), "Lần hai")))
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    void BR_M02_15_applicationDateIsLocked_onceWaiting() throws Exception {
        long residentId = admissionResident();
        String etag = create(residentId);
        long id = applicationIdOf(residentId);
        // Hồ sơ chờ thuộc Phase 4: lượt Đang chờ là dấu hiệu đã có hồ sơ chờ
        jdbc.update("UPDATE admission_application SET status = 'WAITING' WHERE id = ?", id);

        mvc.perform(post("/api/v1/admission-applications/" + id + "/update")
                        .header("If-Match", etag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(today().minusDays(3).toString(), "Đổi ngày")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("APPLICATION_DATE_LOCKED"));

        mvc.perform(post("/api/v1/admission-applications/" + id + "/update")
                        .header("If-Match", etag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(today().toString(), "Giữ nguyên ngày")))
                .andExpect(status().isOk());
    }

    @Test
    void FR_001_closedApplication_cannotBeUpdated() throws Exception {
        long residentId = admissionResident();
        String etag = create(residentId);
        long id = applicationIdOf(residentId);
        jdbc.update("UPDATE admission_application SET status = 'CANCELLED' WHERE id = ?", id);

        mvc.perform(post("/api/v1/admission-applications/" + id + "/update")
                        .header("If-Match", etag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(today().toString(), "Muộn rồi")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("APPLICATION_CLOSED"));
    }
}
