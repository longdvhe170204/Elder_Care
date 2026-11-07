package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.UnitPriceApi;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;

/**
 * 004 T066 (US9; FR-035, BR-M11-02, DBR-08): phiên bản đơn giá. Phiên bản mới còn mở đóng phiên bản đang mở ở ngày liền
 * trước; hiệu lực lùi chỉ cho khoảng chưa có phiên bản và bị cắt trước phiên bản kế tiếp; chồng khoảng bị chặn; phiên bản
 * đã tham chiếu không sửa được (chỉ đóng khoảng).
 */
@AutoConfigureMockMvc
@WithMockUser
class DBR_08_PriceVersionIT extends ContractTestBase {

    @Autowired
    UnitPriceApi prices;

    private long serviceWithoutVersions() {
        String name = "Đưa đi khám " + UUID.randomUUID();
        jdbc.update(M02Sql.INSERT_SERVICE, name, M02Sql.attributes("MEDICAL_ESCORT", "lần", "OCCURRENCE", null));
        return jdbc.queryForObject(M02Sql.SERVICE_ID, Long.class, name);
    }

    private void version(long serviceId, int price, String from, String to, boolean referenced) {
        jdbc.update(
                "INSERT INTO price_version (service_id, unit_price, effective_from, effective_to, referenced)"
                        + " VALUES (?, ?, ?, ?, ?)",
                serviceId,
                price,
                from,
                to,
                referenced);
    }

    private ResultActions add(long serviceId, int price, LocalDate from, LocalDate to) throws Exception {
        String body = "{\"unitPrice\":%d,\"effectiveFrom\":\"%s\"%s}"
                .formatted(price, from, to == null ? "" : ",\"effectiveTo\":\"" + to + "\"");
        return mvc.perform(post("/api/v1/services/" + serviceId + "/price-versions")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private JsonNode versions(long serviceId) throws Exception {
        return JSON.readTree(mvc.perform(get("/api/v1/services/" + serviceId + "/price-versions"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    }

    @Test
    void DBR_08_overlappingRange_isBlocked() throws Exception {
        long id = serviceWithoutVersions();
        version(id, 300_000, "2024-01-01", "2024-03-31", false);
        version(id, 310_000, "2024-07-01", "2024-09-30", false);
        user.set(manager.user());

        add(id, 1, LocalDate.of(2024, 3, 31), LocalDate.of(2024, 4, 30)).andExpect(status().isConflict());
        add(id, 1, LocalDate.of(2024, 4, 1), LocalDate.of(2024, 7, 1)).andExpect(status().isConflict());
        add(id, 1, LocalDate.of(2023, 12, 1), LocalDate.of(2024, 1, 1)).andExpect(status().isConflict());
        add(id, 1, LocalDate.of(2024, 8, 1), LocalDate.of(2024, 8, 5)).andExpect(status().isConflict());
        assertThat(versions(id)).hasSize(2);
    }
}
