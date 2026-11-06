package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.nutrition.api.events.MenuPublished;
import com.eldercare.backend.nutrition.api.events.MenuReturned;
import com.eldercare.backend.nutrition.api.events.MenuSubmitted;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

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
    void FR_025a_submitWithoutProcessingCheck_isBlocked_preSubmitInfoHasCountsWithoutNames() throws Exception {
        LocalDate week = futureWeek();
        long diabetes = f.diet(false, List.of(), safe);
        long resident = f.resident("IN_RESIDENCE");
        f.activeAssignment(resident, diabetes, "REGULAR", List.of(), time.now());
        long menu = createMenu(week);
        putContent(menu, fullWeek(week, defaultDiet, plain)).andExpect(status().isOk());

        f.asKitchen();
        send(get(MENUS + "/" + menu + "/pre-submit-info"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diets[?(@.dietId == " + diabetes + ")].residentCount")
                        .value(1))
                .andExpect(content().string(not(containsString(residentName(resident)))));
        command(menu, "submit", "{\"processingCheckConfirmed\":false,\"confirmedWarnings\":[]}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[*].code", hasItem("PROCESSING_CHECK_REQUIRED")))
                .andExpect(jsonPath("$.unmetConditions[*].code", hasItem("INSUFFICIENT_COVERAGE")));
    }

    @Test
    void FR_023_oneMenuPerWeek_cancelReleasesWeek_endedWeekBlocked() throws Exception {
        LocalDate week = futureWeek();
        long menu = createMenu(week);

        send(post(MENUS), "{\"weekStart\":\"" + week + "\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[0].code").value("MENU_EXISTS_FOR_WEEK"));
        command(menu, "cancel", "{}").andExpect(status().isUnprocessableContent());
        command(menu, "cancel", "{\"reason\":\"Lập nhầm tuần\"}").andExpect(status().isOk());
        assertThat(menuStatus(menu)).isEqualTo("CANCELLED");
        createMenu(week);

        send(post(MENUS), "{\"weekStart\":\"" + currentWeek().minusWeeks(1) + "\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[0].code").value("WEEK_ENDED"));
        send(post(MENUS), "{\"weekStart\":\"" + week.plusDays(1) + "\"}").andExpect(status().isUnprocessableContent());
    }

    @Test
    void FR_024_content_editableOnlyInDraft_withIfMatch_andSuitableActiveDishes() throws Exception {
        LocalDate week = futureWeek();
        long sugar = f.allergen();
        long diabetes = f.diet(false, List.of(sugar), safe);
        long sweet = f.activeDish(List.of(sugar), List.of(defaultDiet));
        long draftDish = f.draftDish(List.of(), List.of(defaultDiet));
        long menu = createMenu(week);

        putContent(menu, List.of(row(week, breakfast, diabetes, sweet)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[*].code", hasItem("DISH_NOT_SUITABLE")));
        putContent(menu, List.of(row(week, breakfast, defaultDiet, draftDish)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[*].code", hasItem("DISH_NOT_ACTIVE")));
        f.asKitchen();
        mvc.perform(put(MENUS + "/" + menu)
                        .header("Idempotency-Key", "m08-stale-" + menu)
                        .header("If-Match", "\"999\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dishes\":[]}"))
                .andExpect(status().isPreconditionFailed());

        putContent(menu, fullWeek(week, defaultDiet, plain)).andExpect(status().isOk());
        submit(menu, codes(menu)).andExpect(status().isOk());
        assertThat(menuStatus(menu)).isEqualTo("PENDING_PUBLICATION");
        assertThat(events.stream(MenuSubmitted.class)).hasSize(1);
        putContent(menu, fullWeek(week, defaultDiet, safe))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));
    }

    @Test
    void FR_024_copyFromPreviousMenu_shiftsDates_andMarksInactiveDishes() throws Exception {
        LocalDate week = futureWeek();
        long soonInactive = f.activeDish(List.of(), List.of(defaultDiet));
        long source = createMenu(week);
        putContent(source, fullWeek(week, defaultDiet, soonInactive)).andExpect(status().isOk());
        jdbc.update(
                "UPDATE catalog_item SET status = 'INACTIVE', attributes = JSON_SET(attributes, '$.status', 'INACTIVE')"
                        + " WHERE id = ?",
                soonInactive);

        f.asKitchen();
        var created = send(post(MENUS), "{\"weekStart\":\"" + week.plusWeeks(1) + "\",\"copyFromId\":" + source + "}")
                .andExpect(status().isCreated())
                .andReturn();
        String location = created.getResponse().getHeader("Location");
        long copy = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));

        send(get(MENUS + "/" + copy), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.copiedFromId").value(source))
                .andExpect(jsonPath("$.content.dishes", hasSize(21)))
                .andExpect(jsonPath("$.content.dishes[0].date")
                        .value(week.plusWeeks(1).toString()))
                .andExpect(jsonPath("$.dishesToReplace", hasSize(21)));
        submit(copy, List.of())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[*].code", hasItem("DISH_NOT_ACTIVE")));
    }

    @Test
    void FR_002_dietInUnendedMenu_cannotBeDeactivated() throws Exception {
        LocalDate week = futureWeek();
        long soft = f.diet(false, List.of(), safe);
        long softDish = f.activeDish(List.of(), List.of(soft));
        long menu = createMenu(week);
        putContent(menu, List.of(row(week, breakfast, soft, softDish))).andExpect(status().isOk());

        f.asDoctor();
        send(post(NutritionCatalogFixture.BASE + "/diets/" + soft + "/deactivate"), "{\"reason\":\"Bỏ\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STILL_IN_MENU"));
        f.asKitchen();
        command(menu, "cancel", "{\"reason\":\"Lập lại\"}").andExpect(status().isOk());
        f.asDoctor();
        send(post(NutritionCatalogFixture.BASE + "/diets/" + soft + "/deactivate"), "{\"reason\":\"Bỏ\"}")
                .andExpect(status().isOk());
    }

    @Test
    void Q_308_kitchenCannotPublish_returnNeedsReason_withdrawGoesBackToDraft() throws Exception {
        LocalDate week = futureWeek();
        long menu = createMenu(week);
        putContent(menu, fullWeek(week, defaultDiet, plain)).andExpect(status().isOk());
        submit(menu, codes(menu)).andExpect(status().isOk());

        f.asKitchen();
        command(menu, "publish", "{\"confirmedWarnings\":[]}").andExpect(status().isForbidden());
        f.asDoctor();
        command(menu, "return", "{}").andExpect(status().isUnprocessableContent());
        command(menu, "return", "{\"reason\":\"Bữa tối nhiều tinh bột\"}").andExpect(status().isOk());
        assertThat(menuStatus(menu)).isEqualTo("DRAFT");
        assertThat(events.stream(MenuReturned.class))
                .singleElement()
                .satisfies(e -> assertThat(e.reason()).isEqualTo("Bữa tối nhiều tinh bột"));

        submit(menu, codes(menu)).andExpect(status().isOk());
        f.asKitchen();
        command(menu, "withdraw", "{}").andExpect(status().isOk());
        assertThat(menuStatus(menu)).isEqualTo("DRAFT");
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
