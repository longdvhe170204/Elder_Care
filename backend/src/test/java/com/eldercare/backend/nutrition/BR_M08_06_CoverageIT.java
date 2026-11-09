package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.nutrition.api.events.MenuPublished;
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

    @Test
    void FR_002_dietInMenuNotEnded_cannotBeDeactivated() throws Exception {
        LocalDate week = futureWeek();
        long soft = f.diet(false, List.of(), safe);
        long softDish = f.activeDish(List.of(), List.of(soft));
        long menu = createMenu(week);
        putContent(menu, List.of(row(week, lunch, soft, softDish))).andExpect(status().isOk());

        f.asDoctor();
        send(post(NutritionCatalogFixture.BASE + "/diets/" + soft + "/deactivate"), "{\"reason\":\"Không dùng nữa\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STILL_IN_MENU"));
    }

    @Test
    void FR_029_publish_rerunsCoverage_thenPublishesOrAppliesWhenWeekStarted() throws Exception {
        LocalDate week = futureWeek();
        long menu = createMenu(week);
        putContent(menu, fullWeek(week, defaultDiet, plain)).andExpect(status().isOk());
        submit(menu, codes(menu)).andExpect(status().isOk());
        long diabetes = f.diet(false, List.of(), safe);
        f.activeAssignment(f.resident("IN_RESIDENCE"), diabetes, "REGULAR", List.of(), time.now());

        publish(menu, List.of())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[0].code").value("INSUFFICIENT_COVERAGE"));
        assertThat(menuStatus(menu)).isEqualTo("PENDING_PUBLICATION");

        LocalDate other = futureWeek();
        long ok = createMenu(other);
        List<String> rows = new ArrayList<>(fullWeek(other, defaultDiet, plain));
        long diabeticDish = f.activeDish(List.of(), List.of(diabetes));
        rows.addAll(fullWeek(other, diabetes, diabeticDish));
        putContent(ok, rows).andExpect(status().isOk());
        submit(ok, codes(ok)).andExpect(status().isOk());
        publish(ok, List.of()).andExpect(status().isOk());
        assertThat(menuStatus(ok)).isEqualTo("PUBLISHED");
        assertThat(events.stream(MenuPublished.class)).hasSize(1);

        jdbc.update(
                "UPDATE menu SET status = 'CANCELLED', cancel_reason = 'test' WHERE week_start = ?"
                        + " AND status IN ('DRAFT', 'PENDING_PUBLICATION')",
                currentWeek());
        long started = createMenu(currentWeek());
        List<String> current = new ArrayList<>(fullWeek(currentWeek(), defaultDiet, plain));
        current.addAll(fullWeek(currentWeek(), diabetes, diabeticDish));
        putContent(started, current).andExpect(status().isOk());
        submit(started, codes(started)).andExpect(status().isOk());
        publish(started, List.of()).andExpect(status().isOk());
        assertThat(menuStatus(started)).isEqualTo("APPLIED");
    }
}
