package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.support.TestUsers;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

/**
 * 011 T022 (US4, FR-033 → FR-038, FR-043, BR-M08-01, 09; quickstart Q20 → Q24, Q31; tên {@code BR_M08_01_ChotSuatIT}
 * trong tasks): tính sẵn xem được trước mốc, chốt bị chặn trước mốc và khi bữa chưa đặt giờ; chốt một lần; người được tính
 * theo trạng thái, lượt vắng, chuyến đi, lịch bán trú, lượt ở lại; phiếu mỗi tầng/khu lập cùng lệnh; suất ăn người thân là
 * nguồn chi phí cho 010; chưa có thực đơn thì không chặn mà gắn dấu và báo.
 */
class BR_M08_01_PortionClosingIT extends ClosingTestBase {

    @Test
    void FR_034_closing_countsByStatusAbsenceDayCare_slipsPerFloor_closedOnce() throws Exception {
        publishedWeek();
        long a = residentIn(room2);
        long hospital = f.resident("HOSPITALIZED");
        locations.stay(hospital, room2, at(LocalTime.MIDNIGHT).minusSeconds(86_400), null);
        long expected = absentResident(room2, at(LocalTime.of(10, 45)));
        long late = absentResident(room3, at(LocalTime.of(12, 0)));
        long dayCareArea = TestUsers.nextId();
        long dayCare = dayCareResident(dayCareArea);
        clock.setTo(at(LocalTime.of(9, 30)));

        close(dailyMeal(lunch))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.portionsByFloorArea[?(@.floorAreaId == " + floor2 + ")].portionCount")
                        .value(hasItem(2)))
                .andExpect(jsonPath("$.portionsByFloorArea[?(@.floorAreaId == " + floor3 + ")]", hasSize(0)))
                .andExpect(jsonPath("$.portionsByFloorArea[?(@.floorAreaId == " + dayCareArea + ")].portionCount")
                        .value(hasItem(1)))
                .andExpect(jsonPath("$.flags", hasItem("NO_DIET_ASSIGNED")));

        assertThat(jdbc.queryForList(
                        "SELECT JSON_EXTRACT(content, '$.persons[*].residentId') FROM record_event WHERE kind ="
                                + " 'M08.PORTION_CLOSING' AND entity_type = 'M08.DAILY_MEAL' AND entity_id = ?",
                        String.class,
                        dailyMeal(lunch)))
                .singleElement()
                .satisfies(ids -> assertThat(ids)
                        .contains(String.valueOf(a), String.valueOf(expected), String.valueOf(dayCare))
                        .doesNotContain(String.valueOf(hospital), String.valueOf(late)));
        assertThat(slipCount(dailyMeal(lunch))).isGreaterThanOrEqualTo(2);
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_CONTAINS(content->'$.flags', '\"EXPECTED_RETURN\"') FROM simple_record"
                                + " WHERE kind = 'M08.SPECIAL_PORTION' AND resident_id = ?",
                        Integer.class,
                        expected))
                .isEqualTo(1);
        close(dailyMeal(lunch))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[0].code").value("ALREADY_CLOSED"));
        f.asDoctor();
        send(post(DAILY_MEALS + dailyMeal(dinner) + "/close-portions"), "{}").andExpect(status().isForbidden());
    }
}
