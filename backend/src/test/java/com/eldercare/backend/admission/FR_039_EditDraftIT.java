package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.support.TestUsers.TestUser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * 004 T043, T047 (FR-038, FR-039; 000 module-interfaces mục 1.5): sửa yêu cầu thay đổi lưu trú còn Nháp qua {@code PUT
 * /residency-change-requests/{id}}. Người yêu cầu sửa được; Hành chính hoàn thiện và gửi duyệt Nháp do người đại diện gửi
 * qua cổng người thân, người yêu cầu giữ nguyên, chỉ người yêu cầu được Hủy; người khác 403; sai {@code If-Match} 412; hết
 * Nháp 409. Hệ thống điền lại giá trị trước và ảnh hưởng chi phí; thuộc tính bỏ đi không còn giữ chỗ khi duyệt (FR-046).
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_039_EditDraftIT extends ChangeTestBase {

    private static final String CARE_LEVEL = "[\"CARE_LEVEL\"]";
    private static final String REGULAR = "{\"careLevel\": \"REGULAR\"}";

    private TestUser representative(long residentId) {
        long familyMemberId = representativeOf(residentId);
        families.representative(families.relate(familyMemberId, residentId));
        return users.family(familyMemberId);
    }

    private String injection(long serviceId) {
        return "{\"services\": [{\"serviceId\": " + serviceId + ", \"action\": \"ADD\"}]}";
    }

    private String changeEtag(long requestId) throws Exception {
        return mvc.perform(get("/api/v1/residency-change-requests/" + requestId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("ETag");
    }

    private ResultActions edit(long requestId, String ifMatch, ObjectNode body) throws Exception {
        return mvc.perform(put("/api/v1/residency-change-requests/" + requestId)
                .with(csrf())
                .header("If-Match", ifMatch)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()));
    }

    private long requester(long requestId) {
        return jdbc.queryForObject("SELECT requester_id FROM approval_request WHERE id = ?", Long.class, requestId);
    }

    @Test
    void FR_038_theRequesterEditsTheDraft_andTheSystemRefillsPreviousValueAndCostImpact() throws Exception {
        Setup s = activeSetup();
        long injection = service("INJECTION", "Tiêm", null, 150_000);
        long id = createChange(request(s.contractId(), "[\"SERVICE\"]", injection(injection), today().plusDays(3)))
                .get("approvalRequestId")
                .asLong();
        String before = changeEtag(id);

        MvcResult result = edit(id, before, request(s.contractId(), CARE_LEVEL, REGULAR, today().plusDays(7)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getHeader("ETag")).isNotEqualTo(before);
        JsonNode edited = getChange(id);
        assertThat(edited.get("status").asString()).isEqualTo("DRAFT");
        assertThat(edited.get("changeTypes").get(0).asString()).isEqualTo("CARE_LEVEL");
        assertThat(edited.get("requestedEffectiveDate").asString())
                .isEqualTo(today().plusDays(7).toString());
        assertThat(edited.get("previousValue").get("careLevel").asString()).isEqualTo("BASIC");
        assertThat(edited.get("costImpact").get(0).get("newUnitPrice").asLong()).isEqualTo(8_000_000L);
        assertThat(requester(id)).isEqualTo(office.accountId());
    }

    @Test
    void FR_039_officeCompletesTheRepresentativesDraft_submits_butCannotCancel() throws Exception {
        Setup s = activeSetup();
        TestUser relative = representative(s.residentId());
        user.set(relative.user());
        MvcResult created = createRaw(
                        "/api/v1/residency-change-requests",
                        request(s.contractId(), CARE_LEVEL, REGULAR, today().plusDays(10)))
                .andExpect(status().isCreated())
                .andReturn();
        long id = JSON.readTree(created.getResponse().getContentAsString())
                .get("approvalRequestId")
                .asLong();

        user.set(office.user());
        edit(id, changeEtag(id), request(s.contractId(), CARE_LEVEL, REGULAR, today().plusDays(12)))
                .andExpect(status().isOk());
        generic(id, "cancel", "{\"reason\":\"Không cần nữa\"}").andExpect(status().isForbidden());
        generic(id, "submit", "{}").andExpect(status().isOk());

        assertThat(requestStatus(id)).isEqualTo("PENDING_APPROVAL");
        assertThat(requester(id)).isEqualTo(relative.accountId());
        assertThat(getChange(id).get("requestedEffectiveDate").asString())
                .isEqualTo(today().plusDays(12).toString());
    }

    @Test
    void FR_033_anotherOfficeStaff_cannotEditOrSubmitAnOfficeDraft() throws Exception {
        Setup s = activeSetup();
        long id = createChange(request(s.contractId(), CARE_LEVEL, REGULAR, today().plusDays(3)))
                .get("approvalRequestId")
                .asLong();
        String tag = changeEtag(id);

        user.set(users.staff(Role.OFFICE_STAFF).user());
        edit(id, tag, request(s.contractId(), CARE_LEVEL, REGULAR, today().plusDays(4)))
                .andExpect(status().isForbidden());
        generic(id, "submit", "{}").andExpect(status().isForbidden());

        assertThat(requestStatus(id)).isEqualTo("DRAFT");
        user.set(office.user());
        assertThat(getChange(id).get("requestedEffectiveDate").asString())
                .isEqualTo(today().plusDays(3).toString());
    }

    @Test
    void staleIfMatch_is412() throws Exception {
        Setup s = activeSetup();
        long id = createChange(request(s.contractId(), CARE_LEVEL, REGULAR, today().plusDays(3)))
                .get("approvalRequestId")
                .asLong();
        String stale = changeEtag(id);
        edit(id, stale, request(s.contractId(), CARE_LEVEL, REGULAR, today().plusDays(4)))
                .andExpect(status().isOk());

        edit(id, stale, request(s.contractId(), CARE_LEVEL, REGULAR, today().plusDays(5)))
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    void FR_033_aSubmittedRequest_isNoLongerEditable() throws Exception {
        Setup s = activeSetup();
        long id = createChange(request(s.contractId(), CARE_LEVEL, REGULAR, today().plusDays(3)))
                .get("approvalRequestId")
                .asLong();
        submit(id);

        edit(id, changeEtag(id), request(s.contractId(), CARE_LEVEL, REGULAR, today().plusDays(4)))
                .andExpect(status().isConflict());
        assertThat(requestStatus(id)).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    void FR_040_anEditedDateBeforeTheContractStart_is422() throws Exception {
        Setup s = activeSetup();
        long id = createChange(request(s.contractId(), CARE_LEVEL, REGULAR, today().plusDays(3)))
                .get("approvalRequestId")
                .asLong();

        edit(id, changeEtag(id), request(s.contractId(), CARE_LEVEL, REGULAR, today().minusYears(5)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void FR_046_anAttributeRemovedByTheEdit_doesNotBlockAnotherRequest() throws Exception {
        Setup s = activeSetup();
        long injection = service("INJECTION", "Tiêm", null, 150_000);
        long first = createChange(request(s.contractId(), CARE_LEVEL, REGULAR, today().plusDays(5)))
                .get("approvalRequestId")
                .asLong();
        edit(
                        first,
                        changeEtag(first),
                        request(s.contractId(), "[\"SERVICE\"]", injection(injection), today().plusDays(5)))
                .andExpect(status().isOk());
        submit(first);
        managerApproves(first);

        long second = createChange(request(s.contractId(), CARE_LEVEL, REGULAR, today().plusDays(6)))
                .get("approvalRequestId")
                .asLong();
        submit(second);
        user.set(manager.user());
        approveRaw(second).andExpect(status().isOk());

        assertThat(requestStatus(second)).isEqualTo("APPROVED");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM simple_record WHERE kind = 'M02.CHANGE_ATTRIBUTE' AND entity_id = ?"
                                + " AND status = 'PENDING'",
                        Integer.class,
                        first))
                .isEqualTo(1);
    }
}
