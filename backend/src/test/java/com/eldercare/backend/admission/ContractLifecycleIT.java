package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

/**
 * 004 T029 (US3; FR-019 → FR-023, FR-025, FR-027, BR-M02-09): vòng đời hợp đồng Nháp → Chờ ký → Hiệu lực, Trả về nháp,
 * Hủy; nội dung chỉ sửa được ở Nháp; chuyển không hợp lệ bị chặn; Hủy tiếp nhận (001) kéo theo hợp đồng.
 */
@AutoConfigureMockMvc
@WithMockUser
class ContractLifecycleIT extends ContractTestBase {

    @Test
    void FR_019_create_returnsDraftWithNumber_etag_andNoWarningsWhenConsistent() throws Exception {
        long residentId = residentWithRepresentative();
        confirmedAssessment(residentId, com.eldercare.backend.resident.api.dto.CareLevel.BASIC);

        MvcResult result = createRaw(body(residentId))
                .andExpect(status().isCreated())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.contract.status").value("DRAFT"))
                .andExpect(jsonPath("$.contract.residentId").value(residentId))
                .andExpect(jsonPath("$.contract.awaitingTermsApproval").value(false))
                .andReturn();

        JsonNode created =
                JSON.readTree(result.getResponse().getContentAsString()).get("contract");
        assertThat(created.get("contractNumber").asString()).matches("HD-\\d{4}-\\d{6}");
        assertThat(created.get("allowedCommands").toString()).contains("send-for-signature", "cancel");
        assertThat(created.get("services")).hasSize(1);
        assertThat(JSON.readTree(result.getResponse().getContentAsString()).get("warnings"))
                .isEmpty();
        assertThat(contractStatus(created.get("id").asLong())).isEqualTo("DRAFT");
    }

    @Test
    void FR_020_sendForSignature_locksContent_returnToDraftUnlocks() throws Exception {
        long residentId = residentWithRepresentative();
        long id = createDraft(residentId);

        command(id, "send-for-signature")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contract.status").value("PENDING_SIGNATURE"));
        updateRaw(id, etag(id), body(residentId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONTRACT_LOCKED"));

        commandWithReason(id, "return-to-draft", "Gia đình xin sửa điều khoản").andExpect(status().isOk());
        assertThat(contractStatus(id)).isEqualTo("DRAFT");
        updateRaw(id, etag(id), body(residentId)).andExpect(status().isOk());
    }

    /** FR-025 (quickstart 004 Q3): lập được hợp đồng khi chưa có đánh giá đầu vào Đã xác nhận, kèm cảnh báo "chưa đánh giá". */
    @Test
    void FR_025_create_withoutConfirmedAssessment_warnsNotAssessed() throws Exception {
        long residentId = residentWithRepresentative();

        createRaw(body(residentId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warnings[?(@.code == 'CONTRACT_NOT_ASSESSED')]")
                        .exists());
    }
}
