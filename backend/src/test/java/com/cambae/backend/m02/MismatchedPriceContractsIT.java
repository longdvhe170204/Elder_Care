package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.m01.api.dto.CareLevel;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;

/**
 * 004 T066 (US9; FR-036, kịch bản 4): tạo phiên bản đơn giá mới không đổi hợp đồng đang hiệu lực; Quản lý viện xem được danh
 * sách hợp đồng Hiệu lực có đơn giá đã ghi khác phiên bản mới nhất.
 */
@AutoConfigureMockMvc
@WithMockUser
class MismatchedPriceContractsIT extends ContractTestBase {

    /** Hợp đồng với một dòng dịch vụ ghi đơn giá {@code recorded}; chuyển sang {@code status} sau khi thêm dòng. */
    private long contractWithService(long residentId, long serviceId, String status, Integer recorded) {
        long id = contracts.contract(
                residentId,
                com.cambae.backend.m02.api.dto.ContractStatus.DRAFT,
                com.cambae.backend.m01.api.dto.ResidenceType.LONG_TERM_RESIDENTIAL,
                CareLevel.BASIC,
                today());
        jdbc.update(
                "INSERT INTO contract_service (contract_id, service_id, in_package, quantity, recorded_unit_price)"
                        + " VALUES (?, ?, TRUE, 1, ?)",
                id,
                serviceId,
                recorded);
        if (!"DRAFT".equals(status)) {
            jdbc.update(
                    "UPDATE contract SET status = ?, signed_date = ?, signing_representative_id = 1, scan_file_id = 1"
                            + " WHERE id = ?",
                    status,
                    today(),
                    id);
        }
        return id;
    }

    private JsonNode mismatched(long serviceId) throws Exception {
        return JSON.readTree(mvc.perform(get("/api/v1/services/" + serviceId + "/mismatched-price-contracts"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    }

    @Test
    void FR_036_listsActiveContractsWhoseRecordedPriceDiffersFromTheLatestVersion() throws Exception {
        long service = service("REHABILITATION", "Phục hồi", null, 200_000, today().minusDays(90));
        long same = contractWithService(newResident(), service, "ACTIVE", 250_000);
        long lagging = contractWithService(newResident(), service, "ACTIVE", 200_000);
        long draft = contractWithService(newResident(), service, "DRAFT", null);
        long ended = contractWithService(newResident(), service, "ENDED", 200_000);

        user.set(manager.user());

        // Phiên bản mới từ ngày mai: hợp đồng ghi 200.000 lệch, hợp đồng ghi 250.000 thì chưa
        mvc.perform(post("/api/v1/services/" + service + "/price-versions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unitPrice\":250000,\"effectiveFrom\":\"%s\"}".formatted(today().plusDays(1))))
                .andExpect(status().isCreated());

        JsonNode list = mismatched(service);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).get("contractId").asLong()).isEqualTo(lagging);
        assertThat(list.get(0).get("contractNumber").asString()).startsWith("HD-");
        assertThat(list.get(0).get("recordedUnitPrice").asInt()).isEqualTo(200_000);
        assertThat(list.get(0).get("newUnitPrice").asInt()).isEqualTo(250_000);
        assertThat(list.findValuesAsString("contractId")).doesNotContain(String.valueOf(same));

        // Hệ thống không đổi gì trên hợp đồng (FR-036)
        assertThat(jdbc.queryForObject(
                        "SELECT recorded_unit_price FROM contract_service WHERE contract_id = ?",
                        Integer.class,
                        lagging))
                .isEqualTo(200_000);
        assertThat(jdbc.queryForObject("SELECT status FROM contract WHERE id = ?", String.class, lagging))
                .isEqualTo("ACTIVE");
        assertThat(draft).isNotEqualTo(ended);
    }

    @Test
    void FR_036_aContractWithoutARecordedPriceIsNotListed_andOfficeIsRefused() throws Exception {
        long service = service("REHABILITATION", "Phục hồi 2", null, 200_000, LocalDate.of(2020, 1, 1));
        contractWithService(newResident(), service, "ACTIVE", null);

        user.set(manager.user());
        assertThat(mismatched(service)).isEmpty();
        user.set(office.user());
        mvc.perform(get("/api/v1/services/" + service + "/mismatched-price-contracts"))
                .andExpect(status().isForbidden());
        user.set(manager.user());
        mvc.perform(get("/api/v1/services/99999999/mismatched-price-contracts")).andExpect(status().isNotFound());
    }
}
