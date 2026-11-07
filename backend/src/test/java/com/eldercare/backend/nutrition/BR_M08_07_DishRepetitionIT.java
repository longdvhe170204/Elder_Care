package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 011 T017 (US3, BR-M08-07, FR-025 b, c, FR-026; quickstart Q13, Q14): lặp món qua ranh giới tuần, món thay thế không
 * tính; cảnh báo dị ứng cho bếp chỉ có số người, Bác sĩ thấy tên; cảnh báo không chặn nhưng phải được xác nhận khi gửi.
 */
class BR_M08_07_DishRepetitionIT extends MenuTestBase {

    @Test
    void FR_026_repetitionAcrossPublishedPreviousWeek_mustBeConfirmed() throws Exception {
        LocalDate previous = futureWeek();
        LocalDate week = previous.plusWeeks(1);
        long porridge = f.activeDish(List.of(), List.of(defaultDiet));
        long prev = createMenu(previous);
        List<String> prevRows = new ArrayList<>(fullWeek(previous, defaultDiet, plain));
        prevRows.add(row(previous.plusDays(5), breakfast, defaultDiet, porridge));
        prevRows.add(row(previous.plusDays(6), breakfast, defaultDiet, porridge));
        putContent(prev, prevRows).andExpect(status().isOk());
        submit(prev, codes(prev)).andExpect(status().isOk());
        publish(prev, List.of()).andExpect(status().isOk());

        long menu = createMenu(week);
        List<String> rows = new ArrayList<>(fullWeek(week, defaultDiet, plain));
        rows.add(row(week, lunch, defaultDiet, porridge));
        rows.add(row(week.plusDays(3), breakfast, defaultDiet, plain, porridge));
        putContent(menu, rows).andExpect(status().isOk());

        f.asKitchen();
        String body = send(post(MENUS + "/" + menu + "/check"), "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverageGaps", hasSize(0)))
                .andExpect(jsonPath("$.dishRepetitions[?(@.dishId == " + porridge + ")].from")
                        .value(previous.plusDays(5).toString()))
                .andExpect(jsonPath("$.dishRepetitions[?(@.dishId == " + porridge + ")].to")
                        .value(week.toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> codes = JsonPath.read(body, "$.warningCodes");
        assertThat(codes)
                .contains("REPETITION:" + defaultDiet + ":" + porridge + ":" + previous.plusDays(5) + ":" + week);

        submit(menu, List.of())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[*].code", hasItem("WARNINGS_NOT_CONFIRMED")));
        submit(menu, codes).andExpect(status().isOk());
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_LENGTH(confirmed_warnings) FROM menu WHERE id = ?", Integer.class, menu))
                .isEqualTo(codes.size());
    }

    @Test
    void BR_M08_07_allergyWarning_kitchenSeesCount_doctorSeesNames() throws Exception {
        LocalDate week = futureWeek();
        long shrimp = f.allergen();
        long shrimpSoup = f.activeDish(List.of(shrimp), List.of(defaultDiet));
        long resident = f.resident("IN_RESIDENCE");
        String name = "Nguyễn Thị Tôm " + resident;
        jdbc.update("UPDATE resident SET full_name = ? WHERE id = ?", name, resident);
        allergy(resident, shrimp);
        long menu = createMenu(week);
        List<String> rows = new ArrayList<>(fullWeek(week, defaultDiet, plain));
        rows.add(row(week, breakfast, defaultDiet, shrimpSoup));
        rows.add(row(week.plusDays(1), breakfast, defaultDiet, shrimpSoup, plain));
        putContent(menu, rows).andExpect(status().isOk());

        f.asKitchen();
        send(post(MENUS + "/" + menu + "/check"), "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allergies", hasSize(1)))
                .andExpect(jsonPath("$.allergies[0].dishId").value(shrimpSoup))
                .andExpect(jsonPath("$.allergies[0].date").value(week.toString()))
                .andExpect(jsonPath("$.allergies[0].residentCount").value(1))
                .andExpect(jsonPath("$.allergies[0].residents").doesNotExist())
                .andExpect(content().string(not(containsString(name))));

        f.asDoctor();
        send(post(MENUS + "/" + menu + "/check"), "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allergies[0].residentCount").value(1))
                .andExpect(jsonPath("$.allergies[0].residents[0].name").value(name));
    }
}
