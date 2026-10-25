package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * 004 T030 (US3; FR-025a, FR-020, FR-036): hợp đồng có điều khoản khác chuẩn phải được Quản lý viện duyệt qua yêu cầu
 * {@code M02.CONTRACT_TERMS_APPROVAL} trước khi Gửi ký, cho đúng nội dung đã gửi; trong khi chờ duyệt nội dung bị khóa; sửa
 * sau khi duyệt làm bản duyệt mất hiệu lực; từ chối thì mở khóa.
 */
@AutoConfigureMockMvc
@WithMockUser
class TermsApprovalIT extends ContractTestBase {

    private static final String REASON = "Giá ưu đãi cho gia đình có công";

    /** Hợp đồng Nháp có dịch vụ chăm sóc với giá riêng thấp hơn giá chuẩn (điều khoản khác chuẩn). */
    private long discountedDraft(long residentId) throws Exception {
        ObjectNode body = body(residentId);
        ObjectNode care = line(basicCare);
        care.put("customUnitPrice", 4_500_000);
        body.putArray("services").add(care);
        return create(body).get("contract").get("id").asLong();
    }

    private JsonNode submit(long id) throws Exception {
        MvcResult result = commandWithReason(id, "submit-terms-for-approval", REASON)
                .andExpect(status().isCreated())
                .andReturn();
        return JSON.readTree(result.getResponse().getContentAsString());
    }

    private String requestEtag(long requestId) throws Exception {
        return mvc.perform(get("/api/v1/approval-requests/" + requestId))
                .andReturn()
                .getResponse()
                .getHeader("ETag");
    }

    private void decide(long requestId, String decision) throws Exception {
        user.set(manager.user());
        mvc.perform(post("/api/v1/approval-requests/" + requestId + "/" + decision)
                        .with(csrf())
                        .header("If-Match", requestEtag(requestId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Đã xem xét\"}"))
                .andExpect(status().isOk());
        user.set(office.user());
    }

    private boolean awaiting(long id) throws Exception {
        return contract(id).get("awaitingTermsApproval").asBoolean();
    }

    @Test
    void FR_025a_nonStandardTerms_listTheStandardAndTheProposedValue() throws Exception {
        long id = discountedDraft(residentWithRepresentative());

        user.set(office.user());
        mvc.perform(get("/api/v1/contracts/" + id + "/non-standard-terms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("PRICE"))
                .andExpect(jsonPath("$[0].standardValue").value("5000000"))
                .andExpect(jsonPath("$[0].proposedValue").value("4500000"));
    }

    @Test
    void FR_025a_sendForSignature_isBlockedUntilTheTermsAreApproved() throws Exception {
        long id = discountedDraft(residentWithRepresentative());

        MvcResult blocked = command(id, "send-for-signature")
                .andExpect(status().isConflict())
                .andReturn();

        assertThat(unmetCodes(blocked)).contains("TERMS_APPROVAL_REQUIRED");
        assertThat(contractStatus(id)).isEqualTo("DRAFT");
    }

    @Test
    void FR_025a_submit_createsAPendingRequest_andLocksTheContent() throws Exception {
        long residentId = residentWithRepresentative();
        long id = discountedDraft(residentId);

        commandWithReason(id, "submit-terms-for-approval", " ").andExpect(status().isUnprocessableContent());
        JsonNode ref = submit(id);

        assertThat(ref.get("type").asString()).isEqualTo("M02.CONTRACT_TERMS_APPROVAL");
        assertThat(ref.get("status").asString()).isEqualTo("PENDING_APPROVAL");
        assertThat(awaiting(id)).isTrue();
        updateRaw(id, etag(id), body(residentId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONTRACT_LOCKED"));
        commandWithReason(id, "submit-terms-for-approval", REASON)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONTRACT_LOCKED"));
        command(id, "send-for-signature").andExpect(status().isConflict());
    }

    @Test
    void FR_025a_submit_withoutNonStandardTerms_is409() throws Exception {
        long id = createDraft(residentWithRepresentative());

        commandWithReason(id, "submit-terms-for-approval", REASON)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NO_NON_STANDARD_TERMS"));
        assertThat(awaiting(id)).isFalse();
    }

    @Test
    void FR_025a_managerApproves_unlocksAndAllowsSendForSignature_thenRecordsTheNegotiatedPrice() throws Exception {
        long residentId = residentWithRepresentative();
        long id = discountedDraft(residentId);
        long requestId = submit(id).get("approvalRequestId").asLong();

        decide(requestId, "approve");

        assertThat(awaiting(id)).isFalse();
        assertThat(jdbc.queryForObject("SELECT terms_approval_request_id FROM contract WHERE id = ?", Long.class, id))
                .isEqualTo(requestId);
        send(id);
        recordSignedRaw(id, signedBody(residentId, today())).andExpect(status().isOk());

        assertThat(jdbc.queryForObject(
                        "SELECT recorded_unit_price FROM contract_service WHERE contract_id = ? AND service_id = ?",
                        BigDecimal.class,
                        id,
                        basicCare))
                .as("giá đã thỏa thuận, không phải giá chuẩn")
                .isEqualByComparingTo("4500000");
        assertThat(jdbc.queryForObject(
                        "SELECT referenced FROM price_version WHERE service_id = ?", Boolean.class, basicCare))
                .as("giá riêng không tham chiếu phiên bản đơn giá")
                .isFalse();
    }

    @Test
    void FR_025a_editingAfterApproval_invalidatesTheApproval() throws Exception {
        long residentId = residentWithRepresentative();
        long id = discountedDraft(residentId);
        decide(submit(id).get("approvalRequestId").asLong(), "approve");

        ObjectNode changed = body(residentId);
        ObjectNode care = line(basicCare);
        care.put("customUnitPrice", 4_000_000);
        changed.putArray("services").add(care);
        updateRaw(id, etag(id), changed).andExpect(status().isOk());

        assertThat(unmetCodes(command(id, "send-for-signature")
                        .andExpect(status().isConflict())
                        .andReturn()))
                .contains("TERMS_APPROVAL_REQUIRED");
    }

    @Test
    void FR_025a_editingToStandardTermsAfterApproval_needsNoApproval() throws Exception {
        long residentId = residentWithRepresentative();
        long id = discountedDraft(residentId);
        decide(submit(id).get("approvalRequestId").asLong(), "approve");

        updateRaw(id, etag(id), body(residentId)).andExpect(status().isOk());

        send(id);
        assertThat(contractStatus(id)).isEqualTo("PENDING_SIGNATURE");
    }

    @Test
    void FR_025a_rejection_unlocksTheContract_andTheContentCanBeEditedAgain() throws Exception {
        long residentId = residentWithRepresentative();
        long id = discountedDraft(residentId);
        long requestId = submit(id).get("approvalRequestId").asLong();

        decide(requestId, "reject");

        // Luồng chờ của Awaitility không mang SecurityContext của @WithMockUser nên đọc cờ bằng SQL
        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> assertThat(jdbc.queryForObject(
                                "SELECT awaiting_terms_approval FROM contract WHERE id = ?", Boolean.class, id))
                        .isFalse());
        assertThat(jdbc.queryForObject("SELECT terms_approval_request_id FROM contract WHERE id = ?", Long.class, id))
                .isNull();
        updateRaw(id, etag(id), body(residentId)).andExpect(status().isOk());
        send(id);
    }

    @Test
    void FR_025a_onlyTheManagerApproves() throws Exception {
        long id = discountedDraft(residentWithRepresentative());
        long requestId = submit(id).get("approvalRequestId").asLong();

        user.set(office.user());
        mvc.perform(post("/api/v1/approval-requests/" + requestId + "/approve")
                        .with(csrf())
                        .header("If-Match", requestEtag(requestId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Tự duyệt\"}"))
                .andExpect(status().isForbidden());

        assertThat(awaiting(id)).isTrue();
    }

    @Test
    void FR_025a_absencePolicyOverride_differentFromTheTable_alsoNeedsApproval() throws Exception {
        long residentId = residentWithRepresentative();
        ObjectNode body = body(residentId);
        ObjectNode override = body.putArray("absencePolicyOverrides").addObject();
        override.put("residenceType", "LONG_TERM_RESIDENTIAL");
        override.put("absenceType", "HOSPITAL");
        override.put("fromDay", 8);
        override.put("toDay", 30);
        override.put("coefficient", "90");
        override.put("holdBed", "YES");
        long id = create(body).get("contract").get("id").asLong();

        user.set(office.user());
        mvc.perform(get("/api/v1/contracts/" + id + "/non-standard-terms"))
                .andExpect(jsonPath("$[0].type").value("ABSENCE_POLICY"))
                .andExpect(jsonPath("$[0].standardValue").value("70"))
                .andExpect(jsonPath("$[0].proposedValue").value("90"));
        assertThat(unmetCodes(command(id, "send-for-signature")
                        .andExpect(status().isConflict())
                        .andReturn()))
                .contains("TERMS_APPROVAL_REQUIRED");
    }
}
