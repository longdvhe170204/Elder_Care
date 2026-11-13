package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 011 T017 (US3, BR-M08-06, FR-023 → FR-025a, FR-027 → FR-030; quickstart Q11, Q12, Q15, Q16): một thực đơn chưa hủy mỗi
 * tuần; mọi lỗi độ phủ hiện một lần; xác nhận kiểm tra chế biến bắt buộc; bếp không công bố; công bố chạy lại độ phủ,
 * tuần đã bắt đầu thì Đã áp dụng.
 */
class BR_M08_06_CoverageIT extends MenuTestBase {

    @Test
    void BR_M08_06_missingDinner_isBlockedAndEveryGapListedOnce() throws Exception {
        LocalDate week = futureWeek();
        long diabetes = f.diet(false, List.of(), safe);
        long diabeticDish = f.activeDish(List.of(), List.of(diabetes));
        long resident = f.resident("IN_RESIDENCE");
        f.activeAssignment(resident, diabetes, "REGULAR", List.of(), time.now());
        long menu = createMenu(week);
        List<String> rows = new ArrayList<>(fullWeek(week, defaultDiet, plain));
        LocalDate wednesday = week.plusDays(2);
        for (String row : fullWeek(week, diabetes, diabeticDish)) {
            if (!(row.contains(wednesday.toString()) && row.contains("\"mealId\":" + dinner + ","))) {
                rows.add(row);
            }
        }
        putContent(menu, rows).andExpect(status().isOk());

        f.asKitchen();
        send(post(MENUS + "/" + menu + "/check"), "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverageGaps", hasSize(1)))
                .andExpect(jsonPath("$.coverageGaps[0].dietId").value(diabetes))
                .andExpect(jsonPath("$.coverageGaps[0].date").value(wednesday.toString()))
                .andExpect(jsonPath("$.coverageGaps[0].mealId").value(dinner));
        submit(menu, codes(menu))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions", hasSize(1)))
                .andExpect(jsonPath("$.unmetConditions[0].code").value("INSUFFICIENT_COVERAGE"));
        assertThat(menuStatus(menu)).isEqualTo("DRAFT");
    }
}
