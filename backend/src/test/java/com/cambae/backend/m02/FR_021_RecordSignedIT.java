package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.m02.api.dto.ContractStatus;
import com.cambae.backend.m02.api.events.ContractStatusChanged;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * 004 T030 (US3; FR-021, FR-036, DBR-02, Q-297): "Ghi nhận đã ký" cần ngày ký, người đại diện hiệu lực (012) và bản scan;
 * thành công thì hợp đồng Hiệu lực, ghi phiên bản nội dung 0 với đơn giá của mọi khoản và đánh dấu phiên bản giá đã tham
 * chiếu. Kiểm giường (nội trú) và sức chứa (bán trú) chờ giao diện của 003 nên chưa có ở đây.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_021_RecordSignedIT extends ContractTestBase {

    private long pendingContract(long residentId, ObjectNode body) throws Exception {
        long id = create(body).get("contract").get("id").asLong();
        send(id);
        return id;
    }

    @Test
    void FR_021_recordSigned_makesTheContractActive_withVersionZeroAndRecordedPrices() throws Exception {
        long residentId = residentWithRepresentative();
        ObjectNode body = body(residentId);
        body.put("endDate", today().plusDays(365).toString());
        body.putArray("services").add(line(basicCare)).add(line(meal));
        long id = pendingContract(residentId, body);
        long file = scan();
        ObjectNode sign = JSON.createObjectNode();
        sign.put("signedDate", today().toString());
        sign.put("signingRepresentativeId", representativeOf(residentId));
        sign.put("scanFileId", file);

        recordSignedRaw(id, sign)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contract.status").value("ACTIVE"))
                .andExpect(jsonPath("$.contract.signedDate").value(today().toString()))
                .andExpect(jsonPath("$.contract.currentEndDate")
                        .value(today().plusDays(365).toString()));

        assertThat(contractStatus(id)).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT signing_representative_id FROM contract WHERE id = ?", Long.class, id))
                .isEqualTo(representativeOf(residentId));
        assertThat(jdbc.queryForObject("SELECT scan_file_id FROM contract WHERE id = ?", Long.class, id))
                .isEqualTo(file);
        assertThat(jdbc.queryForObject("SELECT owner_type FROM file WHERE id = ?", String.class, file))
                .isEqualTo("M02.CONTRACT_SCAN");

        assertThat(jdbc.queryForList(
                        "SELECT sequence_no, effective_from FROM contract_content_version WHERE contract_id = ?", id))
                .singleElement()
                .satisfies(row -> {
                    assertThat(((Number) row.get("sequence_no")).intValue()).isZero();
                    assertThat(row.get("effective_from").toString()).isEqualTo(today().toString());
                });
        assertThat(jdbc.queryForObject(
                        "SELECT recorded_unit_price FROM contract_service WHERE contract_id = ? AND service_id = ?",
                        BigDecimal.class,
                        id,
                        basicCare))
                .isEqualByComparingTo("5000000");
        assertThat(jdbc.queryForObject(
                        "SELECT recorded_unit_price FROM contract_service WHERE contract_id = ? AND service_id = ?",
                        BigDecimal.class,
                        id,
                        meal))
                .isEqualByComparingTo("600000");
        assertThat(jdbc.queryForObject(
                        "SELECT referenced FROM price_version WHERE service_id = ?", Boolean.class, basicCare))
                .isTrue();
        assertThat(events.stream(ContractStatusChanged.class)
                        .filter(event -> event.contractId() == id && event.to() == ContractStatus.ACTIVE))
                .hasSize(1);
    }

    @Test
    void FR_021_recordSigned_needsAllThreeFields() throws Exception {
        long residentId = residentWithRepresentative();
        long id = pendingContract(residentId, body(residentId));

        for (String missing : new String[] {"signedDate", "signingRepresentativeId", "scanFileId"}) {
            ObjectNode sign = signedBody(residentId, today());
            sign.remove(missing);
            recordSignedRaw(id, sign).andExpect(status().isBadRequest());
        }
        assertThat(contractStatus(id)).isEqualTo("PENDING_SIGNATURE");
    }

    @Test
    void DBR_02_signerMustBeAnActiveRepresentativeOfThisResident() throws Exception {
        long residentId = residentWithRepresentative();
        long other = residentWithRepresentative();
        long id = pendingContract(residentId, body(residentId));

        ObjectNode strangers = signedBody(residentId, today());
        strangers.put("signingRepresentativeId", 123_456_789L);
        recordSignedRaw(id, strangers)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_SIGNING_REPRESENTATIVE"));

        ObjectNode otherResidents = signedBody(residentId, today());
        otherResidents.put("signingRepresentativeId", representativeOf(other));
        recordSignedRaw(id, otherResidents)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_SIGNING_REPRESENTATIVE"));

        assertThat(contractStatus(id)).isEqualTo("PENDING_SIGNATURE");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM contract_content_version WHERE contract_id = ?", Integer.class, id))
                .isZero();
    }

    @Test
    void FR_021_unknownScanFile_is404_andTheContractStaysPending() throws Exception {
        long residentId = residentWithRepresentative();
        long id = pendingContract(residentId, body(residentId));
        ObjectNode sign = signedBody(residentId, today());
        sign.put("scanFileId", 987_654_321L);

        recordSignedRaw(id, sign).andExpect(status().isNotFound());

        assertThat(contractStatus(id)).isEqualTo("PENDING_SIGNATURE");
    }

    @Test
    void FR_021_aScanFileAlreadyUsedByAnotherRecord_isRefused() throws Exception {
        long residentId = residentWithRepresentative();
        long first = pendingContract(residentId, body(residentId));
        ObjectNode sign = signedBody(residentId, today());
        recordSignedRaw(first, sign).andExpect(status().isOk());
        ObjectNode next = body(residentId);
        next.put("startDate", today().plusDays(400).toString());
        long second = pendingContract(residentId, next);

        recordSignedRaw(second, sign).andExpect(status().isConflict());

        assertThat(contractStatus(second)).isEqualTo("PENDING_SIGNATURE");
    }

    @Test
    void FR_036_aServiceWithoutAPriceAtTheStartDate_blocksRecordSigned() throws Exception {
        long residentId = residentWithRepresentative();
        long future = service("MEAL", "Dịch vụ giá tương lai", null, 900_000, today().plusDays(20));
        ObjectNode body = body(residentId);
        body.putArray("services").add(line(basicCare)).add(line(future));
        long id = pendingContract(residentId, body);

        MvcResult result = recordSignedRaw(id, signedBody(residentId, today()))
                .andExpect(status().isConflict())
                .andReturn();

        assertThat(unmetCodes(result)).contains("SERVICE_PRICE_MISSING");
        assertThat(contractStatus(id)).isEqualTo("PENDING_SIGNATURE");
    }

    @Test
    void FR_025_sendForSignature_warnsWhenAServicePriceIsNotEffectiveAtTheStartDate() throws Exception {
        long residentId = residentWithRepresentative();
        long future = service("MEAL", "Dịch vụ giá tương lai", null, 900_000, today().plusDays(20));
        ObjectNode body = body(residentId);
        body.putArray("services").add(line(basicCare)).add(line(future));
        long id = create(body).get("contract").get("id").asLong();

        JsonNode sent = JSON.readTree(command(id, "send-for-signature")
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

        assertThat(sent.get("warnings").toString()).contains("SERVICE_PRICE_NOT_EFFECTIVE");
        assertThat(sent.get("contract").get("status").asString()).isEqualTo("PENDING_SIGNATURE");
    }
}
