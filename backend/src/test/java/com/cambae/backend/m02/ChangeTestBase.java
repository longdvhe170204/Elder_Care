package com.cambae.backend.m02;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.m15.approval.scheduling.ApplyDueRequestsJob;
import com.cambae.backend.support.fake.m15.FakeFamilyRelationships;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Nền chung cho test thay đổi lưu trú của 004 US5 (T042, T043, T075): hợp đồng Hiệu lực dựng qua HTTP như {@link
 * ContractTestBase}, yêu cầu thay đổi lập qua {@code POST /residency-change-requests}, duyệt qua các endpoint của 000, áp
 * dụng bằng job {@link ApplyDueRequestsJob} với đồng hồ giả lập. Lớp con khai {@code @AutoConfigureMockMvc} và {@code
 * @WithMockUser}.
 */
abstract class ChangeTestBase extends ContractTestBase {

    protected static final String REASON = "Gia đình đề nghị";

    @Autowired
    protected ApplyDueRequestsJob applyJob;

    @Autowired
    protected FakeFamilyRelationships families;

    /** Người cao tuổi có người đại diện và hợp đồng nội trú dài hạn Hiệu lực (Cơ bản, dịch vụ chăm sóc, suất ăn thuộc gói). */
    protected record Setup(long residentId, long contractId) {}

    protected Setup activeSetup() throws Exception {
        long residentId = residentWithRepresentative();
        ObjectNode body = body(residentId);
        ((ArrayNode) body.get("services")).add(line(meal));
        return new Setup(residentId, activeContract(residentId, body));
    }

    protected ObjectNode request(long contractId, String changeTypes, String newValue, LocalDate effective) {
        return (ObjectNode) JSON.readTree("""
                {"contractId": %d, "changeTypes": %s, "newValue": %s, "requestedEffectiveDate": "%s",
                 "reason": "%s"}""".formatted(contractId, changeTypes, newValue, effective, REASON));
    }

    protected ResultActions createRaw(String path, ObjectNode body) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body.toString()));
    }

    /** Hành chính lập yêu cầu thay đổi (201); trả {@code ResidencyChangeRequest}. */
    protected JsonNode createChange(ObjectNode body) throws Exception {
        user.set(office.user());
        MvcResult result = createRaw("/api/v1/residency-change-requests", body)
                .andExpect(status().isCreated())
                .andReturn();
        return JSON.readTree(result.getResponse().getContentAsString());
    }

    protected JsonNode getChange(long approvalRequestId) throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/residency-change-requests/" + approvalRequestId))
                .andExpect(status().isOk())
                .andReturn();
        return JSON.readTree(result.getResponse().getContentAsString());
    }

    protected String requestEtag(long requestId) throws Exception {
        return mvc.perform(get("/api/v1/approval-requests/" + requestId))
                .andReturn()
                .getResponse()
                .getHeader("ETag");
    }

    protected ResultActions generic(long requestId, String command, String body) throws Exception {
        return mvc.perform(post("/api/v1/approval-requests/" + requestId + "/" + command)
                .with(csrf())
                .header("If-Match", requestEtag(requestId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    /** Gửi duyệt bởi người lập (Hành chính). */
    protected void submit(long requestId) throws Exception {
        user.set(office.user());
        generic(requestId, "submit", "{}").andExpect(status().isOk());
    }

    protected ResultActions approveRaw(long requestId) throws Exception {
        return generic(requestId, "approve", "{\"comment\":\"Đồng ý\"}");
    }

    /** Quản lý viện duyệt (200). */
    protected void managerApproves(long requestId) throws Exception {
        user.set(manager.user());
        approveRaw(requestId).andExpect(status().isOk());
        user.set(office.user());
    }

    /** Lập, gửi duyệt rồi Quản lý viện duyệt; trả mã yêu cầu phê duyệt. */
    protected long createAndApprove(ObjectNode body) throws Exception {
        long id = createChange(body).get("approvalRequestId").asLong();
        submit(id);
        managerApproves(id);
        return id;
    }

    protected JsonNode addenda(long contractId) throws Exception {
        user.set(office.user());
        MvcResult result = mvc.perform(get("/api/v1/contracts/" + contractId + "/addenda"))
                .andExpect(status().isOk())
                .andReturn();
        return JSON.readTree(result.getResponse().getContentAsString());
    }

    protected String requestStatus(long requestId) {
        return jdbc.queryForObject("SELECT status FROM approval_request WHERE id = ?", String.class, requestId);
    }

    /** Đưa đồng hồ tới 12:00 ngày {@code day} rồi chạy một đợt áp dụng yêu cầu đến hạn. */
    protected void applyDueOn(LocalDate day) {
        noonOf(day);
        applyJob.run();
    }

    /** Phiên bản nội dung mới nhất của hợp đồng (JSON). */
    protected JsonNode latestContent(long contractId) {
        String content = jdbc.queryForObject(
                "SELECT content FROM contract_content_version WHERE contract_id = ? ORDER BY sequence_no DESC LIMIT 1",
                String.class,
                contractId);
        return JSON.readTree(content);
    }

    protected int versionCount(long contractId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM contract_content_version WHERE contract_id = ?", Integer.class, contractId);
    }
}
