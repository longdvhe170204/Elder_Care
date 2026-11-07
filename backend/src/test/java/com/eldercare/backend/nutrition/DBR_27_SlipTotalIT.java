package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.events.ResidentStatusChanged;
import com.eldercare.backend.roombedallocation.api.events.BedTransferred;
import com.eldercare.backend.support.TestUsers;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 011 T022 (US4, DBR-27, FR-044; tên {@code DBR_27_TongPhieuIT} trong tasks): tổng suất trên các phiếu của bữa luôn bằng
 * số suất đã chốt cộng các phát sinh cần giao đã được bếp xác nhận; phát sinh chưa xác nhận và phát sinh chỉ ghi nhận
 * (suất giữ) không làm đổi tổng; mỗi (bữa, tầng/khu) có đúng một phiếu.
 */
class DBR_27_SlipTotalIT extends SlipTestBase {

    @Test
    void DBR_27_slipTotal_equalsClosedPlusConfirmedDeliveredAdjustments() throws Exception {
        publishedWeek();
        residentIn(room2);
        long moved = residentIn(room2);
        long sick = residentIn(room2);
        residentIn(room3);
        absentResident(room3, at(LocalTime.of(10, 50)));
        clock.setTo(at(LocalTime.of(9, 30)));
        close(dailyMeal(lunch)).andExpect(status().isCreated());
        long closed = jdbc.queryForObject(
                "SELECT total_portions FROM portion_closing WHERE daily_meal_id = ?", Long.class, dailyMeal(lunch));
        assertThat(slipTotal(dailyMeal(lunch))).isEqualTo(closed);

        clock.setTo(at(LocalTime.of(10, 0)));
        locations.closeStays(moved, at(LocalTime.of(10, 0)));
        locations.stay(moved, room3, at(LocalTime.of(10, 0)), null);
        handlers.onBedTransferred(new BedTransferred(moved, 1, 2, floor2, floor3, at(LocalTime.of(10, 0))));
        setStatus(sick, "HOSPITALIZED");
        handlers.onResidentStatusChanged(new ResidentStatusChanged(
                sick,
                ResidentStatus.IN_RESIDENCE,
                ResidentStatus.HOSPITALIZED,
                null,
                TestUsers.nextId(),
                at(LocalTime.of(10, 0))));
        assertThat(slipTotal(dailyMeal(lunch))).as("phát sinh chưa xác nhận").isEqualTo(closed);

        String body = adjustmentsOf(dailyMeal(lunch)).andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> rows = JsonPath.read(body, "$[?(@.needsDelivery == true)]");
        int confirmedSum = 0;
        for (Map<String, Object> row : rows) {
            if (((Number) row.get("floorAreaId")).longValue() == floor2 && (int) row.get("portionCount") < 0) {
                send(post("/api/v1/adjustments/" + row.get("id") + "/confirm"), "{}")
                        .andExpect(status().isOk());
                confirmedSum += (int) row.get("portionCount");
            }
        }
        assertThat(confirmedSum).isEqualTo(-2);
        assertThat(slipTotal(dailyMeal(lunch))).isEqualTo(closed + confirmedSum);

        jobs.runMinuteTasks(at(LocalTime.of(11, 0)));
        assertThat(slipTotal(dailyMeal(lunch))).as("suất giữ chỉ ghi nhận").isEqualTo(closed + confirmedSum);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) - COUNT(DISTINCT unique_key) FROM simple_record WHERE kind = 'M08.MEAL_SLIP'"
                                + " AND entity_id = ?",
                        Integer.class,
                        dailyMeal(lunch)))
                .isZero();
    }
}
