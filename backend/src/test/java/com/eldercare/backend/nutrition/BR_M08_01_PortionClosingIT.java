package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.billing.api.events.CostSourceBilled;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.nutrition.api.dto.StayMealView;
import com.eldercare.backend.nutrition.api.dto.TripOverlapView;
import com.eldercare.backend.nutrition.api.events.PortionsClosed;
import com.eldercare.backend.support.TestUsers;
import java.time.LocalTime;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 011 T022 (US4, FR-033 → FR-038, FR-043, BR-M08-01, 09; quickstart Q20 → Q24, Q31; tên {@code BR_M08_01_ChotSuatIT}
 * trong tasks): tính sẵn xem được trước mốc, chốt bị chặn trước mốc và khi bữa chưa đặt giờ; chốt một lần; người được tính
 * theo trạng thái, lượt vắng, chuyến đi, lịch bán trú, lượt ở lại; phiếu mỗi tầng/khu lập cùng lệnh; suất ăn người thân là
 * nguồn chi phí cho 010; chưa có thực đơn thì không chặn mà gắn dấu và báo.
 */
class BR_M08_01_PortionClosingIT extends ClosingTestBase {

    @Test
    void FR_033_beforeClosingTime_precomputedVisible_closeBlocked_noMealTimeBlocked() throws Exception {
        publishedWeek();
        residentIn(room2);
        clock.setTo(at(LocalTime.of(8, 0)));

        precomputed(dailyMeal(lunch))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyMealId").value(dailyMeal(lunch)))
                .andExpect(jsonPath("$.portionsByFloorArea[?(@.floorAreaId == " + floor2 + ")].portionCount")
                        .value(hasItem(1)));
        close(dailyMeal(lunch))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[0].code").value("BEFORE_CLOSING_TIME"));
        long snack = meal("Phụ", true, false);
        close(dailyMeal(snack))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[0].code").value("MEAL_TIME_NOT_SET"));
        f.asKitchen();
        send(get("/api/v1/daily-meals?date=" + day), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.dailyMealId == " + dailyMeal(lunch) + ")].closingOpensAt")
                        .value(hasItem(at(LocalTime.of(9, 0)).toString())))
                .andExpect(jsonPath("$[?(@.dailyMealId == " + dailyMeal(lunch) + ")].closed")
                        .value(hasItem(false)));
    }

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
                        "SELECT JSON_EXTRACT(counted_residents, '$[*].residentId') FROM portion_closing"
                                + " WHERE daily_meal_id = ?",
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

    @Test
    void FR_034_tripOverlappingMeal_excludesResident() throws Exception {
        publishedWeek();
        long a = residentIn(room2);
        long b = residentIn(room2);
        trips.trip(new TripOverlapView(TestUsers.nextId(), Set.of(a), at(LocalTime.of(9, 0)), at(LocalTime.of(14, 0))));
        clock.setTo(at(LocalTime.of(9, 30)));

        close(dailyMeal(lunch))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.portionsByFloorArea[?(@.floorAreaId == " + floor2 + ")].portionCount")
                        .value(hasItem(1)));
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_EXTRACT(counted_residents, '$[*].residentId') FROM portion_closing"
                                + " WHERE daily_meal_id = ?",
                        String.class,
                        dailyMeal(lunch)))
                .contains(String.valueOf(b))
                .doesNotContain(String.valueOf(a) + ",", "[" + a + "]");
    }

    @Test
    void FR_043_familyStayWithMeal_countedOnResidentFloor_recordedAsCostSource() throws Exception {
        publishedWeek();
        long a = residentIn(room2);
        long familyMember = TestUsers.nextId();
        stays.stay(new StayMealView(
                TestUsers.nextId(), a, familyMember, at(LocalTime.MIDNIGHT), at(LocalTime.of(23, 0)), true));
        clock.setTo(at(LocalTime.of(9, 30)));

        close(dailyMeal(lunch))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.familyMealPortions").value(1))
                .andExpect(jsonPath("$.portionsByFloorArea[?(@.floorAreaId == " + floor2 + ")].portionCount")
                        .value(hasItem(2)));
        assertThat(events.stream(CostSourceBilled.class)).singleElement().satisfies(e -> {
            assertThat(e.sourceType()).isEqualTo("FAMILY_MEAL");
            assertThat(e.residentId()).isEqualTo(a);
            assertThat(e.incurredOn()).isEqualTo(day);
        });
        assertThat(jdbc.queryForObject(
                        "SELECT status FROM simple_record WHERE kind = 'M08.FAMILY_MEAL' AND resident_id = ?",
                        String.class,
                        a))
                .isEqualTo("RECORDED");
    }

    @Test
    void FR_038_noPublishedMenu_doesNotBlock_flagsAndNotifiesManager() throws Exception {
        residentIn(room2);
        clock.setTo(at(LocalTime.of(9, 30)));

        close(dailyMeal(lunch)).andExpect(status().isCreated()).andExpect(jsonPath("$.flags", hasItem("NO_MENU")));
        assertThat(events.stream(PortionsClosed.class)).singleElement().satisfies(e -> {
            assertThat(e.flags()).contains("NO_MENU");
            assertThat(e.notificationRequests())
                    .flatExtracting(r -> r.recipientGroups())
                    .contains(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
        });
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_CONTAINS(content->'$.flags', '\"NO_MENU\"') FROM simple_record"
                                + " WHERE kind = 'M08.MEAL_SLIP' AND entity_id = ? AND unique_key LIKE ?",
                        Integer.class,
                        dailyMeal(lunch),
                        "%:" + floor2))
                .isEqualTo(1);
    }
}
