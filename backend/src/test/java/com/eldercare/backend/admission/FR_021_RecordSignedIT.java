package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.dto.ContractStatus;
import com.eldercare.backend.admission.api.events.ContractStatusChanged;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.node.ObjectNode;

/**
 * 004 T030 (US3; FR-021, FR-036, DBR-02, Q-297): "Ghi nhận đã ký" cần ngày ký, người đại diện hiệu lực (012) và bản scan;
 * thành công thì hợp đồng Hiệu lực, ghi phiên bản nội dung 0 với đơn giá của mọi khoản và đánh dấu phiên bản giá đã tham
 * chiếu. Nội trú cần phân bổ giường của 003 bắt đầu không muộn hơn ngày bắt đầu.
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
        bedForSigning(residentId);
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
                        "SELECT sequence_no, effective_from FROM contract_version WHERE kind = 'CONTENT' AND contract_id = ?",
                        id))
                .singleElement()
                .satisfies(row -> {
                    assertThat(((Number) row.get("sequence_no")).intValue()).isZero();
                    assertThat(row.get("effective_from").toString()).isEqualTo(today().toString());
                });
        assertThat(jdbc.queryForObject(
                        "SELECT CAST(JSON_EXTRACT(v.recorded_prices, CONCAT('$.\"', s.id, '\"')) AS DECIMAL(15, 0)) FROM contract_version v"
                                + " JOIN (SELECT ? AS cid, ? AS id) s ON v.contract_id = s.cid WHERE v.kind = 'DRAFT'",
                        BigDecimal.class,
                        id,
                        basicCare))
                .isEqualByComparingTo("5000000");
        assertThat(jdbc.queryForObject(
                        "SELECT CAST(JSON_EXTRACT(v.recorded_prices, CONCAT('$.\"', s.id, '\"')) AS DECIMAL(15, 0)) FROM contract_version v"
                                + " JOIN (SELECT ? AS cid, ? AS id) s ON v.contract_id = s.cid WHERE v.kind = 'DRAFT'",
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

    /** FR-021 (quickstart 004 Q6): hợp đồng nội trú chưa có phân bổ giường từ ngày bắt đầu thì không ghi nhận đã ký được. */
    @Test
    void FR_021_residentialWithoutBedAllocation_isBlocked() throws Exception {
        long residentId = residentWithRepresentative();
        ObjectNode body = body(residentId);
        body.putArray("services").add(line(basicCare));
        long id = pendingContract(residentId, body);

        recordSignedRaw(id, signedBody(residentId, today()))
                .andExpect(status().isConflict())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("BED_ALLOCATION_REQUIRED")));
        assertThat(contractStatus(id)).isEqualTo("PENDING_SIGNATURE");
    }
}
