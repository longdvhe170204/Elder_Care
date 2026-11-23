package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.events.ResidencyChangeNeedsConfirmation;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Bác sĩ lập yêu cầu đổi mức chăm sóc theo người cao tuổi (004 FR-039; Bác sĩ không có quyền xem hợp đồng, Phụ lục 27): thân
 * gửi {@code residentId}, hệ thống tìm hợp đồng Hiệu lực của người đó. Yêu cầu chuyển cho Hành chính (thông báo): Hành chính
 * xác nhận với người đại diện, kèm bằng chứng, rồi mới gửi Quản lý viện duyệt (chủ dự án yêu cầu 2026-11-20). Bác sĩ không tự
 * gửi duyệt hay tự ghi xác nhận được.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_039a_DoctorRequestByResidentIT extends ChangeTestBase {

    private ObjectNode byResident(long residentId) {
        return (ObjectNode) JSON.readTree("""
                {"residentId": %d, "changeTypes": ["CARE_LEVEL"], "newValue": {"careLevel": "REGULAR"},
                 "requestedEffectiveDate": "%s", "reason": "Đánh giá lại"}""".formatted(residentId, today().plusDays(3)));
    }

    private long doctorCreates(long residentId) throws Exception {
        // FR-043j (R20): yêu cầu đổi mức cần lần đánh giá lại đã xác nhận ra đúng mức mới
        assessedAs(
                jdbc.queryForObject(
                        "SELECT id FROM contract WHERE resident_id = ? AND status = 'ACTIVE'", Long.class, residentId),
                com.eldercare.backend.resident.api.dto.CareLevel.REGULAR);
        user.set(doctor.user());
        JsonNode created = JSON.readTree(createRaw("/api/v1/residency-change-requests", byResident(residentId))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
        return created.get("approvalRequestId").asLong();
    }

    private void officeConfirms(long id, long residentId) throws Exception {
        user.set(office.user());
        ObjectNode completed = byResident(residentId);
        completed.set("representativeConfirmation", confirmation(today()));
        mvc.perform(put("/api/v1/residency-change-requests/" + id)
                        .with(csrf())
                        .header("If-Match", requestEtag(id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(completed.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void doctorCreatesByResident_officeNotified_confirmsWithRepresentative_managerApproves() throws Exception {
        Setup s = activeSetup();
        long id = doctorCreates(s.residentId());
        assertThat(getChange(id).get("contractId").asLong()).isEqualTo(s.contractId());
        assertThat(events.stream(ResidencyChangeNeedsConfirmation.class))
                .anyMatch(e -> e.requestId() == id && e.residentId() == s.residentId());

        officeConfirms(id, s.residentId());
        submit(id);
        managerApproves(id);

        assertThat(requestStatus(id)).isEqualTo("APPROVED");
    }

    @Test
    void doctorSubmitsWithoutConfirmation_blocked() throws Exception {
        Setup s = activeSetup();
        long id = doctorCreates(s.residentId());

        MvcResult result =
                generic(id, "submit", "{}").andExpect(status().isConflict()).andReturn();

        assertThat(unmetCodes(result)).containsExactly("REPRESENTATIVE_CONFIRMATION_REQUIRED");
        assertThat(requestStatus(id)).isEqualTo("DRAFT");
    }

    @Test
    void doctorCannotRecordConfirmation() throws Exception {
        Setup s = activeSetup();
        ObjectNode body = byResident(s.residentId());
        body.set("representativeConfirmation", confirmation(today()));
        user.set(doctor.user());

        createRaw("/api/v1/residency-change-requests", body).andExpect(status().isForbidden());
    }

    @Test
    void residentWithoutActiveContract_blocked() throws Exception {
        long residentId = residentWithRepresentative();
        user.set(doctor.user());

        createRaw("/api/v1/residency-change-requests", byResident(residentId)).andExpect(status().isConflict());
    }
}
