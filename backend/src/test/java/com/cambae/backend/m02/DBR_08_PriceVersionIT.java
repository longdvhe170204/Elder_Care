package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.m02.api.UnitPriceApi;
import com.cambae.backend.m02.api.events.PriceVersionCreated;
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
        jdbc.update(
                "INSERT INTO service (type, name, unit, billing_period) VALUES ('MEDICAL_ESCORT', ?, 'lần', 'OCCURRENCE')",
                name);
        return jdbc.queryForObject("SELECT id FROM service WHERE name = ?", Long.class, name);
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
    void FR_035_newOpenVersion_closesTheOpenOneTheDayBefore() throws Exception {
        long id = serviceWithoutVersions();
        LocalDate start = today().minusDays(60);
        LocalDate next = today().plusDays(30);
        version(id, 300_000, start.toString(), null, false);
        user.set(manager.user());

        add(id, 350_000, next, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.unitPrice").value(350_000))
                .andExpect(jsonPath("$.effectiveFrom").value(next.toString()))
                .andExpect(jsonPath("$.referenced").value(false));

        JsonNode list = versions(id);
        assertThat(list).hasSize(2);
        assertThat(list.get(0).get("effectiveTo").asString())
                .isEqualTo(next.minusDays(1).toString());
        assertThat(list.get(1).has("effectiveTo")).isFalse();
        assertThat(prices.unitPriceAt(id, next.minusDays(1), false)
                        .orElseThrow()
                        .unitPrice())
                .isEqualByComparingTo("300000");
        assertThat(prices.unitPriceAt(id, next, false).orElseThrow().unitPrice())
                .isEqualByComparingTo("350000");
        assertThat(events.stream(PriceVersionCreated.class).filter(e -> e.serviceId() == id))
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.effectiveFrom()).isEqualTo(next);
                    assertThat(e.effectiveTo()).isNull();
                });
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type = 'M02.PRICE_VERSION'"
                                + " AND action = 'M02_SERVICE_PRICE_VERSION_CREATED'",
                        Integer.class))
                .isGreaterThan(0);
    }

    @Test
    void FR_035_aReferencedOpenVersion_canStillBeClosedByANewOne() throws Exception {
        long id = serviceWithoutVersions();
        version(id, 300_000, today().minusDays(60).toString(), null, true);
        user.set(manager.user());

        add(id, 320_000, today().plusDays(5), null).andExpect(status().isCreated());

        JsonNode list = versions(id);
        assertThat(list.get(0).get("referenced").asBoolean()).isTrue();
        assertThat(list.get(0).get("unitPrice").asInt()).isEqualTo(300_000);
        assertThat(list.get(0).get("effectiveTo").asString())
                .isEqualTo(today().plusDays(4).toString());
    }

    @Test
    void DBR_08_newOpenVersionCannotShortenAPeriodAlreadyApplied() throws Exception {
        long id = serviceWithoutVersions();
        version(id, 300_000, today().minusDays(60).toString(), null, false);
        user.set(manager.user());

        add(id, 350_000, today().minusDays(10), null).andExpect(status().isConflict());
        add(id, 350_000, today(), null).andExpect(status().isConflict());
        assertThat(versions(id)).hasSize(1);
        assertThat(versions(id).get(0).has("effectiveTo")).isFalse();
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

    @Test
    void FR_035_backdatedVersion_fillsOnlyAGap_andIsCutBeforeTheNextVersion() throws Exception {
        long id = serviceWithoutVersions();
        version(id, 300_000, "2024-01-01", "2024-03-31", false);
        version(id, 310_000, "2024-07-01", null, false);
        user.set(manager.user());

        add(id, 305_000, LocalDate.of(2024, 4, 1), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.effectiveTo").value("2024-06-30"));
        // Khoảng đã kín: không còn chỗ cho phiên bản nào nữa
        add(id, 1, LocalDate.of(2024, 5, 1), null).andExpect(status().isConflict());
        // Trước phiên bản đầu tiên: cắt ở ngày liền trước phiên bản kế tiếp
        add(id, 290_000, LocalDate.of(2023, 6, 1), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.effectiveTo").value("2023-12-31"));

        JsonNode list = versions(id);
        assertThat(list).hasSize(4);
        assertThat(list.get(0).get("effectiveFrom").asString()).isEqualTo("2023-06-01");
        assertThat(prices.unitPriceAt(id, LocalDate.of(2024, 5, 15), false)
                        .orElseThrow()
                        .unitPrice())
                .isEqualByComparingTo("305000");
        assertThat(prices.unitPriceAt(id, LocalDate.of(2024, 1, 1), false)
                        .orElseThrow()
                        .unitPrice())
                .isEqualByComparingTo("300000");
    }

    @Test
    void FR_035_backdatedVersionStartingWhereTheNextOneStarts_isBlocked() throws Exception {
        long id = serviceWithoutVersions();
        version(id, 310_000, "2024-07-01", null, false);
        user.set(manager.user());

        add(id, 1, LocalDate.of(2024, 7, 1), null).andExpect(status().isConflict());
    }

    @Test
    void FR_035_aReferencedVersion_hasNoEditEndpoint_andTheDatabaseRefusesEdits() throws Exception {
        long id = serviceWithoutVersions();
        version(id, 300_000, "2024-01-01", null, true);
        long versionId = jdbc.queryForObject("SELECT id FROM price_version WHERE service_id = ?", Long.class, id);
        user.set(manager.user());

        mvc.perform(put("/api/v1/services/" + id + "/price-versions/" + versionId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unitPrice\":1,\"effectiveFrom\":\"2024-01-01\"}"))
                .andExpect(status().is4xxClientError());
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> jdbc.update("UPDATE price_version SET unit_price = 1 WHERE id = ?", versionId))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test
    void FR_035_onlyTheManagerCreates_officeCanRead_inputIsValidated() throws Exception {
        long id = serviceWithoutVersions();
        user.set(office.user());
        add(id, 100_000, today().plusDays(3), null).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/services/" + id + "/price-versions")).andExpect(status().isOk());

        user.set(manager.user());
        add(id, -1, today().plusDays(3), null).andExpect(status().isBadRequest());
        add(id, 100_000, today().plusDays(3), today().plusDays(2)).andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/v1/services/99999999/price-versions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unitPrice\":1,\"effectiveFrom\":\"2030-01-01\"}"))
                .andExpect(status().isNotFound());
        assertThat(versions(id)).isEmpty();
    }
}
