package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.nutrition.api.events.DishSwapped;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

/**
 * 011 T017 (US3, BR-M08-08, FR-031, FR-032; quickstart Q19 phần thực đơn): Đổi món trên thực đơn Công bố hoặc Đã áp dụng,
 * lý do bắt buộc, chỉ món Hiệu lực phù hợp, không cho bữa đã qua; lưu trước/sau; báo Bác sĩ (Trung bình nếu bữa trong
 * ngày); chạy lại cảnh báo. Nội dung ngoài Nháp chỉ đổi qua lệnh này (trigger). Phát sinh sau chốt: Phase 6.
 */
class BR_M08_08_DishSwapIT extends MenuTestBase {

    String swap(LocalDate date, long meal, long oldDish, long newDish, String reason, List<String> warnings) {
        String r = reason == null ? "" : ",\"reason\":\"" + reason + "\"";
        return "{\"date\":\"" + date + "\",\"mealId\":" + meal + ",\"dietId\":" + defaultDiet + ",\"oldDishId\":"
                + oldDish + ",\"newDishId\":" + newDish + r + ",\"confirmedWarnings\":" + strings(warnings) + "}";
    }

    @Test
    void BR_M08_08_swapOnPublishedMenu_appliesImmediately_keepsHistory_notifiesDoctor() throws Exception {
        LocalDate week = futureWeek();
        long menu = publishedMenu(week, plain);
        long fish = f.activeDish(List.of(), List.of(defaultDiet));

        f.asKitchen();
        send(
                        post(MENUS + "/" + menu + "/dish-swaps"),
                        swap(week.plusDays(2), lunch, plain, fish, "Hết thịt", List.of()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverageGaps", hasSize(0)));

        send(get(MENUS + "/" + menu), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.dishes[?(@.date == '" + week.plusDays(2) + "' && @.mealId == " + lunch
                                + ")].dishId")
                        .value(hasItem((int) fish)))
                .andExpect(jsonPath("$.dishSwaps", hasSize(1)))
                .andExpect(jsonPath("$.dishSwaps[0].oldDishId").value(plain))
                .andExpect(jsonPath("$.dishSwaps[0].newDishId").value(fish))
                .andExpect(jsonPath("$.dishSwaps[0].reason").value("Hết thịt"));
        assertThat(menuStatus(menu)).isEqualTo("PUBLISHED");
        assertThat(events.stream(DishSwapped.class))
                .singleElement()
                .satisfies(e ->
                        assertThat(e.notificationRequests().getFirst().level()).contains(NotificationLevel.LOW));
    }

    @Test
    void FR_031_swap_needsReason_activeSuitableDish_publishedMenu_futureMeal() throws Exception {
        LocalDate week = futureWeek();
        long sugar = f.allergen();
        long otherDiet = f.diet(false, List.of(), safe);
        long unsuitable = f.activeDish(List.of(), List.of(otherDiet));
        long draft = f.draftDish(List.of(), List.of(defaultDiet));
        long draftMenu = createMenu(futureWeek());
        long menu = publishedMenu(week, plain);
        long fish = f.activeDish(List.of(sugar), List.of(defaultDiet));

        f.asKitchen();
        String url = MENUS + "/" + menu + "/dish-swaps";
        send(post(url), swap(week, lunch, plain, fish, null, List.of())).andExpect(status().isUnprocessableContent());
        send(post(url), swap(week, lunch, plain, draft, "Đổi", List.of()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[*].code", hasItem("DISH_NOT_ACTIVE")));
        send(post(url), swap(week, lunch, plain, unsuitable, "Đổi", List.of()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[*].code", hasItem("DISH_NOT_SUITABLE")));
        send(post(url), swap(week, lunch, fish, plain, "Đổi", List.of()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[*].code", hasItem("DISH_NOT_IN_MENU")));
        send(post(MENUS + "/" + draftMenu + "/dish-swaps"), swap(week, lunch, plain, fish, "Đổi", List.of()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));

        jdbc.update(
                "UPDATE menu SET status = 'CANCELLED', cancel_reason = 'test' WHERE week_start = ?"
                        + " AND status IN ('DRAFT', 'PENDING_PUBLICATION')",
                currentWeek());
        LocalDate today = time.today();
        if (!today.equals(currentWeek())) {
            long current = publishedMenu(currentWeek(), plain);
            f.asKitchen();
            send(
                            post(MENUS + "/" + current + "/dish-swaps"),
                            swap(today.minusDays(1), lunch, plain, fish, "Đổi", List.of()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.unmetConditions[*].code", hasItem("PAST_MEAL_TIME")));
            send(post(MENUS + "/" + current + "/dish-swaps"), swap(today, lunch, plain, fish, "Đổi", List.of()))
                    .andExpect(status().isOk());
            assertThat(events.stream(DishSwapped.class))
                    .singleElement()
                    .satisfies(
                            e -> assertThat(e.notificationRequests().getFirst().level())
                                    .contains(NotificationLevel.MEDIUM));
        }
    }

    @Test
    void FR_032_swapIntroducingAllergyWarning_mustBeConfirmed() throws Exception {
        LocalDate week = futureWeek();
        long shrimp = f.allergen();
        long shrimpSoup = f.activeDish(List.of(shrimp), List.of(defaultDiet));
        long resident = f.resident("IN_RESIDENCE");
        allergy(resident, shrimp);
        long menu = publishedMenu(week, plain);

        f.asKitchen();
        String url = MENUS + "/" + menu + "/dish-swaps";
        String code = "ALLERGY:" + week.plusDays(1) + ":" + dinner + ":" + defaultDiet + ":" + shrimpSoup;
        send(post(url), swap(week.plusDays(1), dinner, plain, shrimpSoup, "Có tôm tươi", List.of()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[*].code", hasItem("WARNINGS_NOT_CONFIRMED")));
        send(post(url), swap(week.plusDays(1), dinner, plain, shrimpSoup, "Có tôm tươi", List.of(code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allergies[0].residentCount").value(1))
                .andExpect(jsonPath("$.warningCodes", hasItem(code)));
    }

    /** Thân {@code DishSwap} có món trước, món sau, món thay thế tùy chọn (hợp đồng sửa 2026-11-06, chủ dự án chọn). */
    String change(LocalDate date, long meal, long diet, Long oldDish, Long newDish, List<Long> substitutes) {
        String subs = substitutes == null ? "" : ",\"substituteDishIds\":" + NutritionCatalogFixture.ids(substitutes);
        return "{\"date\":\"" + date + "\",\"mealId\":" + meal + ",\"dietId\":" + diet + ",\"oldDishId\":" + oldDish
                + ",\"newDishId\":" + newDish + subs + ",\"reason\":\"Điều chỉnh\",\"confirmedWarnings\":[]}";
    }

    @Test
    void FR_032_addDish_fillsMealOfDietWithoutDish() throws Exception {
        LocalDate week = futureWeek();
        long menu = publishedMenu(week, plain);
        long soft = f.diet(false, List.of(), safe);
        long softDish = f.activeDish(List.of(), List.of(soft));
        jdbc.update(
                "INSERT INTO diet_assignment(resident_id,diet_id,texture,personal_restrictions,basis,"
                        + " requested_effective_from,actual_effective_at,status,created_by,created_at)"
                        + " VALUES(?,?,'REGULAR',JSON_ARRAY(),'ASSESSMENT',NOW(6),NOW(6),'ACTIVE',1,NOW(6))",
                f.resident("IN_RESIDENCE"),
                soft);

        f.asKitchen();
        send(post(MENUS + "/" + menu + "/dish-swaps"), change(week.plusDays(1), lunch, soft, null, softDish, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverageGaps", hasSize(0)));
        send(get(MENUS + "/" + menu), null)
                .andExpect(jsonPath("$.content.dishes[?(@.dietId == " + soft + ")].dishId")
                        .value(hasItem((int) softDish)))
                .andExpect(jsonPath("$.dishSwaps[0].oldDishId").doesNotExist());
    }

    @Test
    void FR_032_removeDish_blockedWhenMealLosesLastDish_allowedWhenAnotherRemains() throws Exception {
        LocalDate week = futureWeek();
        long fish = f.activeDish(List.of(), List.of(defaultDiet));
        long menu = createMenu(week);
        List<String> rows = new java.util.ArrayList<>(fullWeek(week, defaultDiet, plain));
        rows.add(row(week.plusDays(2), dinner, defaultDiet, fish));
        putContent(menu, rows).andExpect(status().isOk());
        submit(menu, codes(menu)).andExpect(status().isOk());
        publish(menu, List.of()).andExpect(status().isOk());

        f.asKitchen();
        String url = MENUS + "/" + menu + "/dish-swaps";
        send(post(url), change(week.plusDays(3), dinner, defaultDiet, plain, null, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[*].code", hasItem("INSUFFICIENT_COVERAGE")));
        send(post(url), change(week.plusDays(2), dinner, defaultDiet, fish, null, null))
                .andExpect(status().isOk());
        send(post(url), change(week, lunch, defaultDiet, null, null, null))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void FR_031_substitutesOnly_sameOldAndNewDish() throws Exception {
        LocalDate week = futureWeek();
        long menu = publishedMenu(week, plain);
        long fish = f.activeDish(List.of(), List.of(defaultDiet));

        f.asKitchen();
        send(
                        post(MENUS + "/" + menu + "/dish-swaps"),
                        change(week.plusDays(1), breakfast, defaultDiet, plain, plain, List.of(fish)))
                .andExpect(status().isOk());
        send(get(MENUS + "/" + menu), null)
                .andExpect(jsonPath("$.content.dishes[?(@.date == '" + week.plusDays(1) + "' && @.mealId == "
                                + breakfast + ")].substituteDishIds[0]")
                        .value(hasItem((int) fish)))
                .andExpect(jsonPath("$.dishSwaps[0].substituteDishIds[0]").value(fish));
    }

    @Test
    void FR_031_trigger_blocksContentChangeOutsideDraftWithoutSwapRecord() throws Exception {
        long menu = publishedMenu(futureWeek(), plain);

        assertThatThrownBy(() ->
                        jdbc.update("UPDATE menu SET content = JSON_OBJECT('dishes', JSON_ARRAY()) WHERE id = ?", menu))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("immutable outside DRAFT");
        assertThatThrownBy(() ->
                        jdbc.update("UPDATE menu SET status = 'CANCELLED', cancel_reason = 'x' WHERE id = ?", menu))
                .isInstanceOf(DataAccessException.class);
    }
}
