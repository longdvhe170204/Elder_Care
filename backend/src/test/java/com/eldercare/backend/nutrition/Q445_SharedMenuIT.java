package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.file.FakeFileConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;

/**
 * 011 T048, T051, T052 (Q-445; quickstart mục 7 Q1, Q2, Q5): món có mô tả, ảnh, Bếp đưa vào dùng; Bếp xem số suất dự kiến
 * và số người cao tuổi theo loại dị ứng không tên; mọi nhân viên xem thực đơn chung đã công bố.
 */
@org.springframework.context.annotation.Import(FakeFileConfig.class)
class Q445_SharedMenuIT extends ClosingTestBase {

    static final String DISHES = NutritionCatalogFixture.BASE + "/dishes";
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 0};

    @Test
    void FR_004_kitchenCreatesActivatesEditsAndDeactivatesDish_withImage() throws Exception {
        f.asKitchen();
        send(post(DISHES), "{\"description\":\"Không tên\"}").andExpect(status().is4xxClientError());
        long dish = f.draftDish(List.of(), List.of());
        assertThat(f.dishStatus(dish)).isEqualTo("DRAFT");

        f.asDoctor();
        send(post(DISHES + "/" + dish + "/activate"), "{}").andExpect(status().isForbidden());
        f.asKitchen();
        send(post(DISHES + "/" + dish + "/activate"), "{}").andExpect(status().isOk());
        assertThat(f.dishStatus(dish)).isEqualTo("ACTIVE");

        int version = jdbc.queryForObject("SELECT version FROM catalog_item WHERE id = ?", Integer.class, dish);
        mvc.perform(put(DISHES + "/" + dish)
                        .header("Idempotency-Key", "m08-" + TestUsers.nextId())
                        .header("If-Match", "\"" + version + "\"")
                        .contentType("application/json")
                        .content("{\"name\":\"Cá kho tộ " + dish + "\",\"description\":\"Cá basa kho tộ\"}"))
                .andExpect(status().isOk());

        mvc.perform(multipart(HttpMethod.PUT, DISHES + "/" + dish + "/image")
                        .file(new MockMultipartFile("file", "ca.png", "image/png", PNG))
                        .header("Idempotency-Key", "m08-" + TestUsers.nextId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").isNotEmpty())
                .andExpect(jsonPath("$.description").value("Cá basa kho tộ"));
        mvc.perform(multipart(HttpMethod.PUT, DISHES + "/" + dish + "/image")
                        .file(new MockMultipartFile("file", "ca.txt", "text/plain", "abc".getBytes()))
                        .header("Idempotency-Key", "m08-" + TestUsers.nextId()))
                .andExpect(status().isUnprocessableContent());

        send(post(DISHES + "/" + dish + "/deactivate"), "{}").andExpect(status().is4xxClientError());
        send(post(DISHES + "/" + dish + "/deactivate"), "{\"reason\":\"Không còn nấu\"}")
                .andExpect(status().isOk());
        assertThat(f.dishStatus(dish)).isEqualTo("INACTIVE");
    }

    @Test
    void FR_025a_kitchenSeesExpectedPortionsAndAllergyCounts_withoutNames() throws Exception {
        long shrimp = f.allergen();
        long withAllergy = residentIn(room2);
        allergy(withAllergy, shrimp);
        residentIn(room2);
        long menu = createMenu(day.minusDays(2));

        f.asKitchen();
        String lunchRow = "$[?(@.date == '" + day + "' && @.mealId == " + lunch + ")]";
        send(get(MENUS + "/" + menu + "/pre-submit-info"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath(lunchRow + ".allergyCounts[?(@.allergenId == " + shrimp + ")].residentCount")
                        .value(hasItem(1)))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString(residentName(withAllergy)))));
    }

    @Test
    void FR_028_everyStaffSeesPublishedSharedMenu_draftHidden() throws Exception {
        publishedWeek();
        long draftWeekDish = f.activeDish(List.of(), List.of());
        long draft = createMenu(day.plusWeeks(1).minusDays(2));
        putContent(draft, List.of(row(day.plusWeeks(1), lunch, defaultDiet, draftWeekDish)))
                .andExpect(status().isOk());

        for (Role role : List.of(Role.CAREGIVER, Role.OFFICE_STAFF, Role.NURSING_HOME_MANAGER)) {
            f.as(role);
            send(get(MENUS + "/shared").param("from", day.toString()).param("to", day.toString()), null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].date").value(day.toString()))
                    .andExpect(jsonPath("$[0].meals[*].dishes[*].name", hasItem(dishName(plain))));
        }
        f.as(Role.CAREGIVER);
        send(
                        get(MENUS + "/shared")
                                .param("from", day.plusWeeks(1).toString())
                                .param("to", day.plusWeeks(1).toString()),
                        null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].meals[*].dishes[*].name", not(hasItem(dishName(draftWeekDish)))));
    }
}
