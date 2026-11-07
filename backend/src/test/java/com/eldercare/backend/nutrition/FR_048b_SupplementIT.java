package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.events.AbsenceClosed;
import com.eldercare.backend.nutrition.api.events.SlipDiscrepancyNotice;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 011 T028 (US5, FR-048b; quickstart Q35; tên {@code FR_048b_PhanBoSungIT} trong tasks): phát sinh "+" bếp xác nhận sau khi
 * phiếu Đã nhận thành phần bổ sung, giao và nhận riêng, phiếu giữ Đã nhận; sai lệch trên phần bổ sung lưu như sai lệch phiếu,
 * bếp xử lý rồi tầng nhận. Yêu cầu suất bổ sung sau giờ bữa luôn là phần bổ sung, không cộng vào số suất của phiếu (chủ dự
 * án chọn 2026-11-07).
 */
class FR_048b_SupplementIT extends SlipTestBase {

    long supplementOf(long slip) {
        return jdbc.queryForObject(
                "SELECT id FROM record_event WHERE kind = 'M08.SUPPLEMENTARY_PORTION' AND entity_id = ?",
                Long.class,
                slip);
    }

    @Test
    void FR_048b_plusAfterReceipt_supplementDeliveredAndReceivedSeparately_slipStaysReceived() throws Exception {
        publishedWeek();
        residentIn(room2);
        long back = f.resident("TEMPORARILY_ABSENT");
        locations.stay(back, room2, at(LocalTime.MIDNIGHT).minusSeconds(86_400 * 30L), null);
        absences.open(back, new com.eldercare.backend.resident.api.dto.OpenAbsence(TestUsers.nextId(), null));
        clock.setTo(at(LocalTime.of(9, 30)));
        close(dailyMeal(lunch)).andExpect(status().isCreated());
        long slip = slipId(dailyMeal(lunch), floor2);
        clock.setTo(at(LocalTime.of(10, 0)));
        prepareAndDeliver(dailyMeal(lunch), slip, List.of());
        TestUser receiver = receiverOf(floor2);
        as(receiver);
        slipCommand(slip, "receive", "{}").andExpect(status().isOk());

        clock.setTo(at(LocalTime.of(10, 40)));
        setStatus(back, "IN_RESIDENCE");
        absences.close(back);
        handlers.onAbsenceClosed(new AbsenceClosed(TestUsers.nextId(), back, at(LocalTime.of(10, 40))));
        String body = adjustmentsOf(dailyMeal(lunch))
                .andExpect(jsonPath("$[?(@.portionCount == 1)]", hasSize(1)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<Integer> plus = JsonPath.read(body, "$[?(@.portionCount == 1)].id");
        int before = slipTotal(dailyMeal(lunch));
        send(post("/api/v1/adjustments/" + plus.getFirst() + "/confirm"), "{}").andExpect(status().isOk());

        long supplement = supplementOf(slip);
        assertThat(slipStatus(slip)).isEqualTo("RECEIVED");
        assertThat(slipTotal(dailyMeal(lunch))).isEqualTo(before + 1);
        slipsOf(dailyMeal(lunch))
                .andExpect(jsonPath("$[?(@.id == " + slip + ")].supplementaryPortions[*]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.id == " + slip + ")].supplementaryPortions[0].portionCount", hasItem(1)));

        as(receiver);
        supplementCommand(supplement, "receive", "{}").andExpect(status().isConflict());
        f.asKitchen();
        supplementCommand(supplement, "deliver", "{}").andExpect(status().isOk());
        supplementCommand(supplement, "deliver", "{}").andExpect(status().isConflict());

        as(receiver);
        supplementCommand(supplement, "report-discrepancy", "{\"content\":\"Sai kết cấu\"}")
                .andExpect(status().isOk());
        assertThat(slipStatus(slip)).isEqualTo("RECEIVED");
        assertThat(events.stream(SlipDiscrepancyNotice.class))
                .singleElement()
                .satisfies(n -> assertThat(n.onSupplement()).isTrue());
        supplementCommand(supplement, "receive", "{}").andExpect(status().isConflict());

        f.asKitchen();
        slipCommand(slip, "resolve-discrepancy", "{\"resolution\":\"REPLACE_PORTION\"}")
                .andExpect(status().isOk());
        assertThat(slipStatus(slip)).isEqualTo("RECEIVED");

        as(receiver);
        supplementCommand(supplement, "receive", "{}").andExpect(status().isOk());
        supplementCommand(supplement, "receive", "{}").andExpect(status().isConflict());
        f.asKitchen();
        slipsOf(dailyMeal(lunch))
                .andExpect(jsonPath(
                        "$[?(@.id == " + slip + ")].supplementaryPortions[0].receivedAt",
                        hasItem(org.hamcrest.Matchers.notNullValue())));
    }

    @Test
    void FR_048b_extraPortionRequest_isSupplement_notInSlipTotal() throws Exception {
        publishedWeek();
        long a = residentIn(room2);
        clock.setTo(at(LocalTime.of(9, 30)));
        close(dailyMeal(lunch)).andExpect(status().isCreated());
        long slip = slipId(dailyMeal(lunch), floor2);
        clock.setTo(at(LocalTime.of(10, 30)));
        prepareAndDeliver(dailyMeal(lunch), slip, List.of());
        int before = slipTotal(dailyMeal(lunch));

        clock.setTo(at(LocalTime.of(11, 10)));
        as(receiverOf(floor2));
        String location = send(
                        post(DAILY_MEALS + dailyMeal(lunch) + "/extra-portions"),
                        "{\"residentId\":" + a + ",\"floorAreaId\":" + floor2 + ",\"reason\":\"Ăn thêm\"}")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");
        f.asKitchen();
        send(post(location + "/confirm"), "{}").andExpect(status().isOk());

        assertThat(slipTotal(dailyMeal(lunch))).isEqualTo(before);
        slipsOf(dailyMeal(lunch))
                .andExpect(jsonPath("$[?(@.id == " + slip + ")].supplementaryPortions[*]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.id == " + slip + ")].supplementaryPortions[0].portionCount", hasItem(1)));
    }
}
