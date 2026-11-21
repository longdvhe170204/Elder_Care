package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.events.AddendumAppliedNotice;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Xác nhận với người đại diện trước khi gửi duyệt (chủ dự án yêu cầu 2026-11-20, thay bản scan phụ lục ký của FR-043a):
 * yêu cầu thay đổi lưu trú do Hành chính hoặc Bác sĩ lập chỉ gửi Quản lý viện duyệt được khi Hành chính đã ghi xác nhận
 * với người đại diện kèm bằng chứng; yêu cầu người đại diện tự gửi qua cổng không cần. Phụ lục áp dụng thì người đại diện
 * được báo.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_043b_RepresentativeConfirmationIT extends ChangeTestBase {

    private static ObjectNode unconfirmed(ObjectNode body) {
        body.remove("representativeConfirmation");
        return body;
    }

    private ObjectNode careLevel(Setup s) {
        return request(s.contractId(), "[\"CARE_LEVEL\"]", "{\"careLevel\": \"REGULAR\"}", today().plusDays(2));
    }

    private ResultActions submitRaw(long id) throws Exception {
        return generic(id, "submit", "{}");
    }

    private ResultActions editRaw(long id, ObjectNode body) throws Exception {
        return mvc.perform(put("/api/v1/residency-change-requests/" + id)
                .with(csrf())
                .header("If-Match", requestEtag(id))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()));
    }

    @Test
    void officeRequest_withoutConfirmation_submitBlocked() throws Exception {
        Setup s = activeSetup();
        JsonNode created = createChange(unconfirmed(careLevel(s)));
        long id = created.get("approvalRequestId").asLong();
        assertThat(created.get("representativeConfirmationRequired").asBoolean())
                .isTrue();

        user.set(office.user());
        MvcResult result = submitRaw(id).andExpect(status().isConflict()).andReturn();

        assertThat(unmetCodes(result)).containsExactly("REPRESENTATIVE_CONFIRMATION_REQUIRED");
        assertThat(requestStatus(id)).isEqualTo("DRAFT");
    }

    /** Thay đổi nhỏ (Q-338: chỉ dịch vụ thuộc gói, chi phí không đổi) viện tự quyết, không cần xác nhận (sửa 2026-11-20). */
    @Test
    void minorChange_needsNoConfirmation() throws Exception {
        Setup s = activeSetup();
        // Bớt suất ăn thuộc gói của hợp đồng: thay đổi nhỏ (R19: dấu thuộc gói theo hợp đồng hiện hành)
        ObjectNode body = request(
                s.contractId(),
                "[\"SERVICE\"]",
                "{\"services\": [{\"serviceId\": " + meal + ", \"action\": \"REMOVE\"}]}",
                today().plusDays(3));
        JsonNode created = createChange(unconfirmed(body));
        long id = created.get("approvalRequestId").asLong();
        assertThat(created.get("minorChange").asBoolean()).isTrue();
        assertThat(created.get("representativeConfirmationRequired").asBoolean())
                .isFalse();

        user.set(office.user());
        submitRaw(id).andExpect(status().isOk());
        assertThat(requestStatus(id)).isEqualTo("PENDING_APPROVAL");
    }

    /** Dịch vụ ngoài gói làm đổi chi phí: không phải thay đổi nhỏ, vẫn cần xác nhận. */
    @Test
    void serviceOutsidePackage_needsConfirmation() throws Exception {
        Setup s = activeSetup();
        long extra = service("ACTIVITY", "Dịch vụ ngoài gói", null, 100_000);
        ObjectNode body = request(
                s.contractId(),
                "[\"SERVICE\"]",
                "{\"services\": [{\"serviceId\": " + extra + ", \"action\": \"ADD\", \"inPackage\": false}]}",
                today().plusDays(3));
        long id = createChange(unconfirmed(body)).get("approvalRequestId").asLong();

        user.set(office.user());
        MvcResult result = submitRaw(id).andExpect(status().isConflict()).andReturn();
        assertThat(unmetCodes(result)).containsExactly("REPRESENTATIVE_CONFIRMATION_REQUIRED");
        assertThat(requestStatus(id)).isEqualTo("DRAFT");
    }

    @Test
    void confirmed_submitAndApprove_confirmationShown_representativeNotifiedOnApply() throws Exception {
        Setup s = activeSetup();
        LocalDate effective = today().plusDays(1);
        ObjectNode body = request(s.contractId(), "[\"CARE_LEVEL\"]", "{\"careLevel\": \"REGULAR\"}", effective);
        long fileId =
                body.get("representativeConfirmation").get("evidenceFileId").asLong();

        long id = createAndApprove(body);
        JsonNode confirmation = getChange(id).get("representativeConfirmation");
        assertThat(confirmation.get("method").asString()).isEqualTo("IN_PERSON");
        assertThat(confirmation.get("confirmedOn").asString()).isEqualTo(today().toString());
        assertThat(confirmation.get("evidenceFileId").asLong()).isEqualTo(fileId);
        assertThat(confirmation.hasNonNull("recordedAt")).isTrue();

        applyDueOn(effective);

        assertThat(requestStatus(id)).isEqualTo("APPLIED");
        assertThat(events.stream(AddendumAppliedNotice.class))
                .anyMatch(e -> e.residentId() == s.residentId()
                        && e.notificationRequests().getFirst().recipientGroups().stream()
                                .anyMatch(g -> g.equals(new RecipientGroup.Representative(s.residentId()))));
    }

    @Test
    void confirmationDateInFuture_rejected() throws Exception {
        Setup s = activeSetup();
        ObjectNode body = careLevel(s);
        body.set("representativeConfirmation", confirmation(today().plusDays(1)));
        user.set(office.user());

        MvcResult result = createRaw("/api/v1/residency-change-requests", body)
                .andExpect(status().isUnprocessableContent())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("CONFIRMATION_DATE_IN_FUTURE");
    }

    /** R16 (Q-414): xác nhận phải ghi người đại diện đã xác nhận. */
    @Test
    void R16_confirmationWithoutConfirmer_rejected() throws Exception {
        Setup s = activeSetup();
        ObjectNode body = careLevel(s);
        ((ObjectNode) body.get("representativeConfirmation")).remove("representativeFamilyMemberId");
        user.set(office.user());

        // Thiếu trường bắt buộc của thân lệnh: lỗi kiểm dữ liệu vào như ca thiếu bằng chứng bên dưới
        createRaw("/api/v1/residency-change-requests", body).andExpect(status().is4xxClientError());
        assertThat(requestsOf(s)).isZero();
    }

    @Test
    void R16_confirmerNotRepresentative_409() throws Exception {
        Setup other = activeSetup();
        Setup s = activeSetup();
        ObjectNode body = careLevel(s);
        body.set("representativeConfirmation", confirmation(today(), representativeOf(other.residentId())));
        user.set(office.user());

        MvcResult result = createRaw("/api/v1/residency-change-requests", body)
                .andExpect(status().isConflict())
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).contains("CONFIRMER_NOT_REPRESENTATIVE");
    }

    /** Xác nhận ghi trước khi có trường người đại diện (dữ liệu cũ) đọc được nhưng phải xác nhận lại trước khi gửi duyệt. */
    @Test
    void R16_legacyConfirmationWithoutConfirmer_submitBlocked() throws Exception {
        Setup s = activeSetup();
        long id = createChange(careLevel(s)).get("approvalRequestId").asLong();
        jdbc.update(
                "UPDATE approval_request SET content = JSON_REMOVE(content,"
                        + " '$.representativeConfirmation.representativeFamilyMemberId') WHERE id = ?",
                id);

        assertThat(getChange(id)
                        .get("representativeConfirmation")
                        .path("representativeFamilyMemberId")
                        .isNull())
                .isTrue();
        MvcResult result = submitRaw(id).andExpect(status().isConflict()).andReturn();
        assertThat(result.getResponse().getContentAsString()).contains("REPRESENTATIVE_CONFIRMATION_REQUIRED");
    }

    private int requestsOf(Setup s) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM approval_request WHERE type = 'M02.RESIDENCY_CHANGE' AND resident_id = ?",
                Integer.class,
                s.residentId());
    }

    @Test
    void confirmationWithoutEvidence_rejected() throws Exception {
        Setup s = activeSetup();
        ObjectNode body = careLevel(s);
        ((ObjectNode) body.get("representativeConfirmation")).remove("evidenceFileId");
        user.set(office.user());

        createRaw("/api/v1/residency-change-requests", body).andExpect(status().is4xxClientError());
    }

    @Test
    void editWithoutConfirmation_dropsIt_submitBlockedAgain() throws Exception {
        Setup s = activeSetup();
        long id = createChange(careLevel(s)).get("approvalRequestId").asLong();

        user.set(office.user());
        editRaw(id, unconfirmed(careLevel(s))).andExpect(status().isOk());

        assertThat(getChange(id).has("representativeConfirmation")).isFalse();
        submitRaw(id).andExpect(status().isConflict());
    }

    /**
     * Người đại diện gửi qua cổng: người yêu cầu là tài khoản người thân. Phạm vi quyền của tài khoản người thân không dựng
     * trong test của 004 (lệnh lập qua cổng trả 404), nên lập bằng Hành chính rồi đổi người yêu cầu thành tài khoản người
     * thân để kiểm luật gửi duyệt.
     */
    @Test
    void representativeRequest_needsNoConfirmation() throws Exception {
        Setup s = activeSetup();
        long id =
                createChange(unconfirmed(careLevel(s))).get("approvalRequestId").asLong();
        TestUser representative = users.family(representativeOf(s.residentId()));
        jdbc.update("UPDATE approval_request SET requester_id = ? WHERE id = ?", representative.accountId(), id);

        user.set(office.user());
        assertThat(getChange(id).get("representativeConfirmationRequired").asBoolean())
                .isFalse();
        submitRaw(id).andExpect(status().isOk());
        assertThat(requestStatus(id)).isEqualTo("PENDING_APPROVAL");
    }
}
