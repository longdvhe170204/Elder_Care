package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Q-462 (FR-019, FR-037): kỳ tính phí lưu trú theo loại lưu trú. Dài hạn chỉ giá tháng; dòng buổi bán trú có kỳ tính
 * trùng kỳ của hợp đồng; yêu cầu đổi loại lưu trú mang kỳ mới khi kỳ cũ không hợp loại mới.
 */
@AutoConfigureMockMvc
@WithMockUser
class Q_462_BillingPeriodIT extends ChangeTestBase {

    private long dayCareService(String period) {
        String name = "Buổi bán trú " + period + " " + System.nanoTime();
        jdbc.update(
                M02Sql.INSERT_SERVICE,
                name,
                M02Sql.attributes(
                        "DAY_CARE_SESSION", "SESSION".equals(period) ? "buổi" : "tháng", period, null, null, null));
        long id = jdbc.queryForObject(M02Sql.SERVICE_ID, Long.class, name);
        jdbc.update(
                "INSERT INTO price_version (service_id, unit_price, effective_from) VALUES (?, ?, '2020-01-01')",
                id,
                "SESSION".equals(period) ? 200_000 : 3_500_000);
        return id;
    }

    private ObjectNode dayCareBody(long residentId, String period, long dayCareService) {
        ObjectNode body = body(residentId);
        body.put("residenceType", "DAY_CARE");
        body.remove("roomTypeId");
        body.put("residencyFeeBillingPeriod", period);
        ((ArrayNode) body.get("services")).add(line(dayCareService));
        ObjectNode line = body.putArray("arrivalSchedule").addObject();
        line.put("dayOfWeek", 1);
        line.put("arrivalTime", "08:00");
        line.put("departureTime", "16:00");
        return body;
    }

    /** Phí lưu trú của loại phòng 101 (loại phòng của {@code body}) theo kỳ. */
    private long residencyFee(String period) {
        String name = "Phí phòng 101 " + period + " " + System.nanoTime();
        jdbc.update(
                M02Sql.INSERT_SERVICE,
                name,
                M02Sql.attributes(
                        "RESIDENCY_FEE", "MONTH".equals(period) ? "tháng" : "ngày", period, null, 101L, null));
        long id = jdbc.queryForObject(M02Sql.SERVICE_ID, Long.class, name);
        jdbc.update(
                "INSERT INTO price_version (service_id, unit_price, effective_from) VALUES (?, ?, '2020-01-01')",
                id,
                "MONTH".equals(period) ? 9_000_000 : 350_000);
        return id;
    }

    @Test
    void Q_462_shortTermDaily_needsDailyResidencyFee() throws Exception {
        long residentId = residentWithRepresentative();
        long monthly = residencyFee("MONTH");
        long daily = residencyFee("DAY");
        ObjectNode body = body(residentId);
        body.put("residenceType", "SHORT_TERM_RESIDENTIAL");
        body.put("endDate", today().plusDays(20).toString());
        body.put("residencyFeeBillingPeriod", "DAY");

        ObjectNode wrongBody = body.deepCopy();
        ((ArrayNode) wrongBody.get("services")).add(line(monthly));
        long wrong = create(wrongBody).get("contract").get("id").asLong();
        MvcResult blocked = command(wrong, "send-for-signature")
                .andExpect(status().isConflict())
                .andReturn();
        assertThat(unmetCodes(blocked)).contains("CONTRACT_RESIDENCY_FEE_MISMATCH");
        commandWithReason(wrong, "cancel", "Lập lại").andExpect(status().isOk());

        ((ArrayNode) body.get("services")).add(line(daily));
        long right = create(body).get("contract").get("id").asLong();
        send(right);
        assertThat(contractStatus(right)).isEqualTo("PENDING_SIGNATURE");
    }

    @Test
    void Q_462_longTermDaily_rejected422() throws Exception {
        ObjectNode body = body(residentWithRepresentative());
        body.put("residencyFeeBillingPeriod", "DAY");

        MvcResult result = createRaw(body).andExpect(status().is(422)).andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("BILLING_PERIOD_NOT_ALLOWED");
    }

    @Test
    void Q_462_dayCareMonthly_needsMonthlyDayCareLine() throws Exception {
        long residentId = residentWithRepresentative();
        long sessionPriced = dayCareService("SESSION");
        long monthPriced = dayCareService("MONTH");

        long wrong = create(dayCareBody(residentId, "MONTH", sessionPriced))
                .get("contract")
                .get("id")
                .asLong();
        MvcResult blocked = command(wrong, "send-for-signature")
                .andExpect(status().isConflict())
                .andReturn();
        assertThat(unmetCodes(blocked)).contains("CONTRACT_DAY_CARE_FEE_MISMATCH");
        commandWithReason(wrong, "cancel", "Lập lại").andExpect(status().isOk());

        long right = create(dayCareBody(residentId, "MONTH", monthPriced))
                .get("contract")
                .get("id")
                .asLong();
        send(right);
        assertThat(contractStatus(right)).isEqualTo("PENDING_SIGNATURE");
    }

    @Test
    void Q_462_dayCareToResidential_needsAllowedPeriod() throws Exception {
        long residentId = residentWithRepresentative();
        ObjectNode body = body(residentId);
        body.put("residenceType", "DAY_CARE");
        body.remove("roomTypeId");
        body.put("residencyFeeBillingPeriod", "SESSION");
        ObjectNode line = body.putArray("arrivalSchedule").addObject();
        line.put("dayOfWeek", 1);
        line.put("arrivalTime", "08:00");
        line.put("departureTime", "16:00");
        long contractId = activeContract(residentId, body);

        // Kỳ SESSION hiện hành không hợp nội trú: thiếu kỳ mới hoặc kỳ mới không hợp loại mới thì 422
        for (String newValue : new String[] {
            "{\"residenceType\": \"LONG_TERM_RESIDENTIAL\"}",
            "{\"residenceType\": \"LONG_TERM_RESIDENTIAL\", \"residencyFeeBillingPeriod\": \"DAY\"}"
        }) {
            MvcResult rejected = createRaw(
                            "/api/v1/residency-change-requests",
                            request(contractId, "[\"RESIDENCE_TYPE\"]", newValue, today().plusDays(2)))
                    .andExpect(status().is(422))
                    .andReturn();
            assertThat(rejected.getResponse().getContentAsString()).contains("BILLING_PERIOD_NOT_ALLOWED");
        }

        long requestId = createChange(request(
                        contractId,
                        "[\"RESIDENCE_TYPE\"]",
                        "{\"residenceType\": \"LONG_TERM_RESIDENTIAL\", \"residencyFeeBillingPeriod\": \"MONTH\"}",
                        today().plusDays(2)))
                .get("approvalRequestId")
                .asLong();
        assertThat(requestId).isPositive();
    }
}
