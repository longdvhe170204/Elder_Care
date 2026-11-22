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
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

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
        assertThat(created.get("contractNumber").asString()).matches("HD-\\d{4}-\\d{6,}");
        assertThat(created.get("allowedCommands").toString()).contains("send-for-signature", "cancel");
        assertThat(created.get("services")).hasSize(1);
        assertThat(JSON.readTree(result.getResponse().getContentAsString()).get("warnings"))
                .isEmpty();
        assertThat(contractStatus(created.get("id").asLong())).isEqualTo("DRAFT");
    }

    // 4b (chủ dự án duyệt 2026-11-21): phí lưu trú của loại phòng khác loại phòng trên hợp đồng thì không Gửi ký được
    @Test
    void FR_025_residencyFeeOfOtherRoomType_blocksSend_untilMatching() throws Exception {
        long residentId = residentWithRepresentative();
        ObjectNode body = body(residentId);
        ((ArrayNode) body.get("services")).add(line(residencyFee(202)));
        long id = create(body).get("contract").get("id").asLong();

        MvcResult blocked = command(id, "send-for-signature")
                .andExpect(status().isConflict())
                .andReturn();
        assertThat(unmetCodes(blocked)).containsExactly("CONTRACT_RESIDENCY_FEE_MISMATCH");

        ObjectNode matching = body(residentId);
        ((ArrayNode) matching.get("services")).add(line(residencyFee(101)));
        updateRaw(id, etag(id), matching).andExpect(status().isOk());
        send(id);
        assertThat(contractStatus(id)).isEqualTo("PENDING_SIGNATURE");
    }

    /** Phí lưu trú theo tháng của loại phòng {@code roomTypeId}, một phiên bản đơn giá mở từ 2020-01-01. */
    private long residencyFee(long roomTypeId) {
        String name = "Phí lưu trú loại phòng " + roomTypeId + " " + System.nanoTime();
        jdbc.update(
                M02Sql.INSERT_SERVICE,
                name,
                M02Sql.attributes("RESIDENCY_FEE", "tháng", "MONTH", null, roomTypeId, null));
        long feeId = jdbc.queryForObject(M02Sql.SERVICE_ID, Long.class, name);
        jdbc.update(
                "INSERT INTO price_version (service_id, unit_price, effective_from) VALUES (?, 6000000, '2020-01-01')",
                feeId);
        return feeId;
    }
}
