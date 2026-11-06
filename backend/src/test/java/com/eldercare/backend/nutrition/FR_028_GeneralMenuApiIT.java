package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.nutrition.api.ExpectedMealApi;
import com.eldercare.backend.nutrition.api.GeneralMenuApi;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** 011 T019, T045 (FR-028, FR-063): API thực đơn cho cổng người thân và người cao tuổi. */
@Transactional
@Import(FR_028_GeneralMenuApiIT.Users.class)
class FR_028_GeneralMenuApiIT extends MySqlIntegrationTestBase {

    @TestConfiguration
    static class Users {
        @Bean
        @Primary
        SwitchableUser menuApiUser() {
            return new SwitchableUser();
        }
    }

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser user;

    @Autowired
    GeneralMenuApi generalMenu;

    @Autowired
    ExpectedMealApi expectedMeals;

    NutritionCatalogFixture fixture;
    long defaultDiet;
    long personalDiet;
    long defaultDish;
    long personalDish;
    long substituteDish;
    long meal;
    long resident;
    LocalDate monday;

    @BeforeEach
    void setUp() throws Exception {
        fixture = new NutritionCatalogFixture(null, jdbc, time, user);
        defaultDiet = catalog("M08.DIET", "Chế độ mặc định", true);
        personalDiet = catalog("M08.DIET", "Chế độ riêng", false);
        defaultDish = catalog("M08.DISH", "Cháo mặc định", false);
        personalDish = catalog("M08.DISH", "Cháo riêng", false);
        substituteDish = catalog("M08.DISH", "Món thay thế", false);
        meal = catalog("M08.MEAL", "Bữa sáng", false);
        resident = fixture.resident("IN_RESIDENCE");
        monday = LocalDate.of(2040, 1, 2);

        jdbc.update(
                "INSERT INTO menu(week_start,status,content,processing_check_confirmed,version,created_by,created_at,"
                        + "published_by,published_at)"
                        + " VALUES(?, 'PUBLISHED', CAST(? AS JSON), TRUE, 0, 1, NOW(6), 1, NOW(6))",
                monday,
                content());
    }

    @Test
    void FR_028_generalMenu_returnsOnlyDefaultDietFromPublishedMenu() {
        assertThat(generalMenu.generalMenu(monday, monday.plusDays(6)))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.date()).isEqualTo(monday);
                    assertThat(item.mealId()).isEqualTo(meal);
                    assertThat(item.mealName()).isEqualTo(name(meal));
                    assertThat(item.dishId()).isEqualTo(defaultDish);
                    assertThat(item.dishName()).isEqualTo(name(defaultDish));
                });
        assertThat(generalMenu.dietNames()).contains(name(defaultDiet), name(personalDiet));
    }

    @Test
    void FR_063_expectedMeals_useResidentsCurrentDietWithoutExposingDietName() {
        jdbc.update(
                "INSERT INTO diet_assignment(resident_id,diet_id,texture,personal_restrictions,basis,"
                        + " requested_effective_from,actual_effective_at,status,created_by,created_at)"
                        + " VALUES(?,?,'REGULAR',JSON_ARRAY(),'ASSESSMENT',NOW(6),NOW(6),'ACTIVE',1,NOW(6))",
                resident,
                personalDiet);

        assertThat(expectedMeals.ofResident(resident, monday, monday.plusDays(6)))
                .extracting(item -> item.dishName() + ":" + item.isSubstitute())
                .containsExactly(name(personalDish) + ":false", name(substituteDish) + ":true");
    }

    private long catalog(String type, String name, boolean isDefault) {
        long id = com.eldercare.backend.support.TestUsers.nextId();
        String attributes = switch (type) {
            case "M08.DIET" ->
                "{\"description\":\"\",\"treatmentRelated\":false,\"isDefault\":" + isDefault
                        + ",\"restrictions\":[],\"safeDishId\":null}";
            case "M08.DISH" ->
                "{\"status\":\"ACTIVE\",\"note\":\"\",\"effectiveVersionId\":null,"
                        + "\"everActive\":true,\"deactivationReason\":null,\"deactivatedBy\":null,"
                        + "\"deactivatedAt\":null}";
            case "M08.MEAL" -> "{\"servesResidential\":true,\"servesDayCare\":false}";
            default -> "{}";
        };
        jdbc.update(
                "INSERT INTO catalog_item(id,module,catalog_type,code,name,status,sort_order,attributes,version)"
                        + " VALUES(?,'M08',?,?,?,'ACTIVE',0,CAST(? AS JSON),0)",
                id,
                type,
                type + "." + id,
                name + " " + id,
                attributes);
        return id;
    }

    private String name(long id) {
        return jdbc.queryForObject("SELECT name FROM catalog_item WHERE id = ?", String.class, id);
    }

    private String content() {
        return "{\"dishes\":[" + entry(defaultDiet, defaultDish) + "," + entry(personalDiet, personalDish) + "]}";
    }

    private String entry(long diet, long dish) {
        String substitutes = diet == personalDiet ? String.valueOf(substituteDish) : "";
        return "{\"date\":\"" + monday + "\",\"mealId\":" + meal + ",\"dietId\":" + diet + ",\"dishId\":" + dish
                + ",\"substituteDishIds\":[" + substitutes + "]}";
    }
}
