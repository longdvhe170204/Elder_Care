package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.nutrition.api.events.MealSlipDelivered;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.events.ResidentStatusChanged;
import com.eldercare.backend.support.TestUsers;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

/**
 * 011 T028 (US5, FR-044 → FR-047, BR-M08-09, 10, 15; quickstart Q32, Q37; tên {@code BR_M08_10_GiaoPhieuIT} trong tasks):
 * phiếu mỗi tầng/khu có số suất theo chế độ ăn và nhãn suất đặc biệt; giao bị chặn khi còn suất đặc biệt chưa chuẩn bị,
 * chưa có lưu mẫu, còn phát sinh chưa xác nhận; giao ghi người, thời điểm giao. Phát sinh mới đưa phiếu Đã chuẩn bị về Đã
 * chốt. Bếp không thấy dấu "cần đối chiếu", dị ứng; tầng thấy (SC-008).
 */
class BR_M08_10_SlipDeliveryIT extends SlipTestBase {

    @Test
    void BR_M08_10_deliverBlockedUntilSpecialsPreparedAndSampleRetained_thenRecordsDelivery() throws Exception {
        publishedWeek();
        residentIn(room2);
        residentIn(room2);
        long soft = softResident(room2);
        clock.setTo(at(LocalTime.of(9, 30)));
        close(dailyMeal(lunch)).andExpect(status().isCreated());
        long slip = slipId(dailyMeal(lunch), floor2);

        f.asKitchen();
        slipsOf(dailyMeal(lunch))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + slip + ")].portionsByDiet[*].portionCount", hasItem(3)))
                .andExpect(jsonPath("$[?(@.id == " + slip + ")].labels[*]", hasSize(3)))
                .andExpect(jsonPath(
                        "$[?(@.id == " + slip + ")].labels[?(@.texture == 'SOFT')].id", hasItem(num(specialOf(soft)))))
                .andExpect(
                        jsonPath("$[?(@.id == " + slip + ")].labels[?(@.texture == 'SOFT')].prepared", hasItem(false)))
                .andExpect(jsonPath("$[?(@.id == " + slip + ")].status", hasItem("CLOSED")));

        clock.setTo(at(LocalTime.of(10, 0)));
        slipCommand(slip, "deliver", "{}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath(
                        "$.unmetConditions[*].code", containsInAnyOrder("NOT_PREPARED", "SAMPLE_NOT_RETAINED")));
        slipCommand(slip, "mark-prepared", "{}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[0].code").value("NOT_PREPARED"));

        f.asDoctor();
        send(post("/api/v1/special-portions/" + specialOf(soft) + "/mark-prepared"), "{}")
                .andExpect(status().isForbidden());
        f.asKitchen();
        send(post("/api/v1/special-portions/" + specialOf(soft) + "/mark-prepared"), "{}")
                .andExpect(status().isOk());
        send(post("/api/v1/special-portions/" + specialOf(soft) + "/mark-prepared"), "{}")
                .andExpect(status().isConflict());
        slipCommand(slip, "mark-prepared", "{}").andExpect(status().isOk());
        slipCommand(slip, "deliver", "{}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[*].code", containsInAnyOrder("SAMPLE_NOT_RETAINED")));

        sample(dailyMeal(lunch));
        slipCommand(slip, "deliver", "{}").andExpect(status().isOk());
        slipCommand(slip, "deliver", "{}").andExpect(status().isConflict());

        assertThat(slipStatus(slip)).isEqualTo("DELIVERED");
        assertThat(jdbc.queryForObject(
                        "SELECT content->>'$.deliveredAt' IS NOT NULL AND content->>'$.deliveredBy' IS NOT NULL"
                                + " FROM simple_record WHERE id = ?",
                        Integer.class,
                        slip))
                .isEqualTo(1);
        assertThat(events.stream(MealSlipDelivered.class))
                .singleElement()
                .satisfies(e -> assertThat(e.slipId()).isEqualTo(slip));
    }

    @Test
    void BR_M08_10_newAdjustmentReopensPreparedSlip_unconfirmedAdjustmentBlocksDelivery() throws Exception {
        publishedWeek();
        residentIn(room2);
        long b = residentIn(room2);
        clock.setTo(at(LocalTime.of(9, 30)));
        close(dailyMeal(lunch)).andExpect(status().isCreated());
        long slip = slipId(dailyMeal(lunch), floor2);
        f.asKitchen();
        slipCommand(slip, "mark-prepared", "{}").andExpect(status().isOk());
        sample(dailyMeal(lunch));

        clock.setTo(at(LocalTime.of(10, 0)));
        setStatus(b, "HOSPITALIZED");
        handlers.onResidentStatusChanged(new ResidentStatusChanged(
                b,
                ResidentStatus.IN_RESIDENCE,
                ResidentStatus.HOSPITALIZED,
                null,
                TestUsers.nextId(),
                at(LocalTime.of(10, 0))));

        assertThat(slipStatus(slip)).isEqualTo("CLOSED");
        f.asKitchen();
        slipCommand(slip, "deliver", "{}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[*].code", containsInAnyOrder("ADJUSTMENTS_NOT_CONFIRMED")));
    }

    @Test
    void SC_008_kitchenSlipHasNoCrossCheckOrAllergy_floorSlipHasCrossCheck() throws Exception {
        publishedWeek();
        long c = residentIn(room2);
        jdbc.update(
                "INSERT INTO health_item (resident_id, type, free_text, severity_id, source_id, recorded_by, recorded_at,"
                        + " pending_nutrition_check) VALUES (?, 'ALLERGY', 'Dị ứng khác', (SELECT MIN(id) FROM"
                        + " allergy_severity), (SELECT MIN(id) FROM information_source), 1, NOW(6), FALSE)",
                c);
        clock.setTo(at(LocalTime.of(9, 30)));
        close(dailyMeal(lunch)).andExpect(status().isCreated());
        long slip = slipId(dailyMeal(lunch), floor2);

        f.asKitchen();
        String kitchen = slipsOf(dailyMeal(lunch))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + slip + ")].labels[*]", hasSize(1)))
                .andExpect(jsonPath("$[*].labels[*].flags[*]", not(hasItem("NEEDS_CROSS_CHECK"))))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(kitchen).doesNotContain("needsCrossCheck", "allergies", "Dị ứng khác");

        as(receiverOf(floor2));
        slipsOf(dailyMeal(lunch))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].needsCrossCheck", hasItem(num(c))));
    }
}
