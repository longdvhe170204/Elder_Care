package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.nutrition.api.events.MealSlipDelivered;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

/**
 * 011 T028 (US5, FR-044 → FR-047, BR-M08-09, 10, 15; quickstart Q32, Q37; tên {@code BR_M08_10_GiaoPhieuIT} trong tasks):
 * phiếu mỗi tầng/khu có các món của bữa và tổng suất (Q-445, không suất đặc biệt); giao bị chặn khi chưa có lưu mẫu, còn
 * phát sinh chưa xác nhận; giao ghi người, thời điểm giao. Phát sinh mới đưa phiếu Đã chuẩn bị về Đã chốt. Bếp không thấy
 * tên, dị ứng (SC-008).
 */
class BR_36_SlipDeliveryIT extends SlipTestBase {

    @Test
    void BR_36_deliverBlockedUntilSampleRetained_thenRecordsDelivery() throws Exception {
        publishedWeek();
        residentIn(room2);
        residentIn(room2);
        softResident(room2);
        clock.setTo(at(LocalTime.of(9, 30)));
        close(dailyMeal(lunch)).andExpect(status().isCreated());
        long slip = slipId(dailyMeal(lunch), floor2);

        f.asKitchen();
        slipsOf(dailyMeal(lunch))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + slip + ")].portionCount", hasItem(3)))
                .andExpect(jsonPath("$[?(@.id == " + slip + ")].group", hasItem("FLOOR")))
                .andExpect(jsonPath("$[?(@.id == " + slip + ")].dishes[*]", hasItem(dishName(plain))))
                .andExpect(jsonPath("$[*].labels").doesNotExist())
                .andExpect(jsonPath("$[?(@.id == " + slip + ")].status", hasItem("CLOSED")));

        clock.setTo(at(LocalTime.of(10, 0)));
        slipCommand(slip, "deliver", "{}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[*].code", containsInAnyOrder("SAMPLE_NOT_RETAINED")));
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
}
