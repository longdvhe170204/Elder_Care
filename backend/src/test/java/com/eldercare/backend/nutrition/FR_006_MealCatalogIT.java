package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.SwitchableUserConfig;
import com.eldercare.backend.support.TestUsers;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 011 T010, T011 (FR-006, FR-007, Q-401): Quản lý viện quản lý danh mục bữa ăn và loại đồ ăn cấm (Phụ lục 27 dòng
 * "Danh mục bữa ăn, loại đồ ăn cấm": Quản lý viện C; Bác sĩ, Điều dưỡng, Nhân viên bếp X). Bữa không xóa, chỉ Ngừng hiệu
 * lực; giờ dự kiến, ngưỡng giao trễ chỉ đọc từ CFG-M08-04.
 */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
@Import(SwitchableUserConfig.class)
class FR_006_MealCatalogIT extends MySqlIntegrationTestBase {

    private static final String MEALS = NutritionCatalogFixture.BASE + "/meals";
    private static final String FORBIDDEN = NutritionCatalogFixture.BASE + "/forbidden-food-types";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser user;

    NutritionCatalogFixture f;

    @BeforeEach
    void setUp() {
        f = new NutritionCatalogFixture(mvc, jdbc, time, user);
    }

    private static String mealBody(String code, String name, boolean dayCare, String status) {
        return "{\"code\":\"" + code + "\",\"name\":\"" + name + "\",\"servesResidential\":true,\"servesDayCare\":"
                + dayCare + ",\"sortOrder\":7" + (status == null ? "" : ",\"status\":\"" + status + "\"") + "}";
    }

    private long mealId(String code) {
        return jdbc.queryForObject(
                "SELECT id FROM catalog_item WHERE catalog_type='M08.MEAL' AND code=?", Long.class, code);
    }

    @Test
    void FR_006_managerCreatesMeal_othersOnlyView_withThresholdFromParameter() throws Exception {
        String code = "SNACK_" + TestUsers.nextId();
        f.asKitchen();
        f.send(post(MEALS), mealBody(code, "Bữa phụ", false, null)).andExpect(status().isForbidden());

        f.asManager();
        f.send(post(MEALS), mealBody(code, "Bữa phụ", false, null)).andExpect(status().isCreated());
        long id = mealId(code);

        f.asKitchen();
        String row = "$[?(@.id == " + id + ")]";
        mvc.perform(get(MEALS))
                .andExpect(status().isOk())
                .andExpect(jsonPath(row + ".code").value(code))
                .andExpect(jsonPath(row + ".name").value("Bữa phụ"))
                .andExpect(jsonPath(row + ".servesResidential").value(true))
                .andExpect(jsonPath(row + ".servesDayCare").value(false))
                .andExpect(jsonPath(row + ".sortOrder").value(7))
                .andExpect(jsonPath(row + ".status").value("ACTIVE"))
                .andExpect(jsonPath(row + ".lateDeliveryThresholdMinutes").isNotEmpty());
    }

    @Test
    void FR_001_dietListCarriesVersion_usedAsIfMatchForUpdate() throws Exception {
        long diet = f.baseDiet();
        String list = mvc.perform(get(NutritionCatalogFixture.BASE + "/diets"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<Integer> versions = JsonPath.read(list, "$[?(@.id == " + diet + ")].version");
        assertThat(versions).singleElement().isNotNull();
        List<Object> safe = JsonPath.read(list, "$[?(@.id == " + diet + ")].safeDishId");

        f.asDoctor();
        f.send(
                        put(NutritionCatalogFixture.BASE + "/diets/" + diet)
                                .header("If-Match", "\"" + versions.getFirst() + "\""),
                        NutritionCatalogFixture.dietBody("Chế độ sửa " + TestUsers.nextId(), false, false, List.of(), 0)
                                .replace("\"safeDishId\":0", "\"safeDishId\":" + safe.getFirst()))
                .andExpect(status().isOk());
    }
}
