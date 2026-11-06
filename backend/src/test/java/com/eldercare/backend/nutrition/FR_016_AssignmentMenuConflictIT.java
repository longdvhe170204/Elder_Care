package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 011 T014, T017 (FR-016, FR-020, FR-022, FR-038; Phase 4 chuyển sang): lập bản gán đối chiếu thực đơn của chế độ ăn mới
 * (tuần hiện tại và tuần đã công bố, từ hôm nay); xung đột chưa có món thay thế và bữa không có món phải được Bác sĩ xác
 * nhận khi Áp dụng, không chặn. Kết quả đối chiếu của một người cho Bác sĩ nêu bữa, món, thành phần; bếp không xem được.
 */
class FR_016_AssignmentMenuConflictIT extends MenuTestBase {

    long shrimp;
    long shrimpDiet;
    long shrimpSoup;
    long resident;
    LocalDate week;

    void publishedMenuWithShrimpBreakfast() throws Exception {
        shrimp = f.allergen();
        shrimpDiet = f.diet(false, List.of(), safe);
        shrimpSoup = f.activeDish(List.of(shrimp), List.of(shrimpDiet));
        resident = f.resident("IN_RESIDENCE");
        allergy(resident, shrimp);
        week = futureWeek();
        long menu = createMenu(week);
        List<String> rows = new ArrayList<>(fullWeek(week, defaultDiet, plain));
        rows.add(row(week, breakfast, shrimpDiet, shrimpSoup));
        putContent(menu, rows).andExpect(status().isOk());
        submit(menu, codes(menu)).andExpect(status().isOk());
        publish(menu, List.of()).andExpect(status().isOk());
    }

    @Test
    void FR_016_conflictWithoutSubstitute_andMissingMeals_mustBeConfirmedOnApply() throws Exception {
        publishedMenuWithShrimpBreakfast();

        String body = f.propose(
                        resident, NutritionCatalogFixture.assignmentBody(shrimpDiet, "REGULAR", List.of(), time.now()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.allergies", hasSize(1)))
                .andExpect(jsonPath("$.allergies[0].dishId").value(shrimpSoup))
                .andExpect(jsonPath("$.allergies[0].type").value("ALLERGY"))
                .andExpect(jsonPath("$.allergies[0].hasSuitableSubstitute").value(false))
                .andExpect(jsonPath("$.allergies[0].ingredients", hasItem((int) shrimp)))
                .andExpect(jsonPath("$.coverageGaps[?(@.date == '" + week + "' && @.mealId == " + lunch + ")]")
                        .exists())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> codes = JsonPath.read(body, "$.warningCodes");
        assertThat(codes).contains("CONFLICT:" + week + ":" + breakfast + ":" + shrimpSoup);
        long id = f.latestAssignment(resident);

        f.apply(id, "Theo đánh giá").andExpect(status().isConflict());
        f.asDoctor();
        f.send(
                        post("/api/v1/diet-assignments/" + id + "/apply"),
                        "{\"professionalReason\":\"Theo đánh giá\",\"confirmedWarnings\":" + strings(codes) + "}")
                .andExpect(status().isOk());
        assertThat(f.assignmentStatus(id)).isEqualTo("ACTIVE");
    }

    @org.springframework.beans.factory.annotation.Autowired
    com.eldercare.backend.nutrition.internal.conflict.service.AllergyMenuCheck allergyCheck;

    @Test
    void FR_021_newAllergyConflictingWithMenuWithoutSubstitute_requestsMediumAlertOncePerDish() throws Exception {
        publishedMenuWithShrimpBreakfast();
        jdbc.update(
                "INSERT INTO diet_assignment(resident_id,diet_id,texture,personal_restrictions,basis,"
                        + " requested_effective_from,actual_effective_at,status,created_by,created_at)"
                        + " VALUES(?,?,'REGULAR',JSON_ARRAY(),'ASSESSMENT',NOW(6),NOW(6),'ACTIVE',1,NOW(6))",
                resident,
                shrimpDiet);
        long item = jdbc.queryForObject(
                "SELECT MAX(id) FROM health_item WHERE resident_id = ? AND allergen_id = ?",
                Long.class,
                resident,
                shrimp);

        allergyCheck.onAllergyRecorded(item, resident);

        assertThat(events.stream(com.eldercare.backend.incident.api.events.AlertRequested.class))
                .singleElement()
                .satisfies(alert -> {
                    assertThat(alert.residentId()).isEqualTo(resident);
                    assertThat(alert.type()).isEqualTo("DIET_ALLERGY_CONFLICT");
                    assertThat(alert.sourceType()).isEqualTo("MENU_DISH");
                    assertThat(alert.level()).isEqualTo("MEDIUM");
                    assertThat(alert.sourceId()).isEqualTo(shrimpSoup);
                });
    }

    @Test
    void FR_022_dishConflictCheck_forDoctor_hiddenFromKitchen() throws Exception {
        publishedMenuWithShrimpBreakfast();
        jdbc.update(
                "INSERT INTO diet_assignment(resident_id,diet_id,texture,personal_restrictions,basis,"
                        + " requested_effective_from,actual_effective_at,status,created_by,created_at)"
                        + " VALUES(?,?,'REGULAR',JSON_ARRAY(),'ASSESSMENT',NOW(6),NOW(6),'ACTIVE',1,NOW(6))",
                resident,
                shrimpDiet);

        f.asDoctor();
        f.send(get("/api/v1/residents/" + resident + "/dish-conflict-check"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].date").value(week.toString()))
                .andExpect(jsonPath("$[0].mealId").value(breakfast))
                .andExpect(jsonPath("$[0].dishId").value(shrimpSoup))
                .andExpect(jsonPath("$[0].ingredients[0]").value(shrimp))
                .andExpect(jsonPath("$[0].type").value("ALLERGY"))
                .andExpect(jsonPath("$[0].hasSuitableSubstitute").value(false));
        f.asKitchen();
        f.send(get("/api/v1/residents/" + resident + "/dish-conflict-check"), null)
                .andExpect(status().isForbidden());
    }
}
