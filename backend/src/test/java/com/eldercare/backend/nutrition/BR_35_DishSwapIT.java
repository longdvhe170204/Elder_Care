package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
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

/**
 * 011 T017 (US3, BR-M08-08, FR-031, FR-032; quickstart Q19 phần thực đơn): Đổi món trên thực đơn Công bố hoặc Đã áp dụng,
 * lý do bắt buộc, chỉ món Hiệu lực phù hợp, không cho bữa đã qua; lưu trước/sau; báo Bác sĩ (Trung bình nếu bữa trong
 * ngày); chạy lại cảnh báo. Nội dung ngoài Nháp chỉ đổi qua lệnh này (trigger). Phát sinh sau chốt: Phase 6.
 */
class BR_35_DishSwapIT extends MenuTestBase {

    String swap(LocalDate date, long meal, long oldDish, long newDish, String reason, List<String> warnings) {
        String r = reason == null ? "" : ",\"reason\":\"" + reason + "\"";
        return "{\"date\":\"" + date + "\",\"mealId\":" + meal + ",\"dietId\":" + defaultDiet + ",\"oldDishId\":"
                + oldDish + ",\"newDishId\":" + newDish + r + ",\"confirmedWarnings\":" + strings(warnings) + "}";
    }

    @Test
    void BR_35_swapOnPublishedMenu_appliesImmediately_keepsHistory_notifiesDoctor() throws Exception {
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

    /** Thân {@code DishSwap} có món trước, món sau, món thay thế tùy chọn (hợp đồng sửa 2026-11-06, chủ dự án chọn). */
    String change(LocalDate date, long meal, long diet, Long oldDish, Long newDish, List<Long> substitutes) {
        String subs = substitutes == null ? "" : ",\"substituteDishIds\":" + NutritionCatalogFixture.ids(substitutes);
        return "{\"date\":\"" + date + "\",\"mealId\":" + meal + ",\"dietId\":" + diet + ",\"oldDishId\":" + oldDish
                + ",\"newDishId\":" + newDish + subs + ",\"reason\":\"Điều chỉnh\",\"confirmedWarnings\":[]}";
    }
}
