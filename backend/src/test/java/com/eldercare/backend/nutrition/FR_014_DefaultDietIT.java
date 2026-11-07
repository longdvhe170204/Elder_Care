package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.TestUsers;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 011 T008 (US2, FR-001, FR-002, FR-003, FR-005, FR-014): đúng một chế độ ăn mặc định (cột sinh {@code
 * m08_default_diet} UNIQUE), chuyển mặc định trong cùng lệnh, món an toàn bắt buộc và không chứa hạn chế.
 */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
@Import(FR_014_DefaultDietIT.Users.class)
class FR_014_DefaultDietIT extends MySqlIntegrationTestBase {

    private static final String DIETS = NutritionCatalogFixture.BASE + "/diets";

    @TestConfiguration
    static class Users {
        @Bean
        @Primary
        SwitchableUser nutritionDietUser() {
            return new SwitchableUser();
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser user;

    NutritionCatalogFixture f;
    long base;
    long sugar;
    long safe;

    @BeforeEach
    void setUp() throws Exception {
        f = new NutritionCatalogFixture(mvc, jdbc, time, user);
        base = f.baseDiet();
        sugar = f.allergen();
        safe = f.activeDish(List.of(), List.of(base));
    }

    @Test
    void FR_014_settingDefaultMovesItInSameCommand_exactlyOneDefaultRemains() throws Exception {
        long first = f.diet(true, List.of(), safe);
        assertThat(f.activeDefaultDiets()).isEqualTo(1);

        long second = f.diet(true, List.of(), safe);

        assertThat(f.activeDefaultDiets()).isEqualTo(1);
        assertThat(isDefault(second)).isTrue();
        assertThat(isDefault(first)).isFalse();
    }

    @Test
    void FR_014_databaseKeepsAtMostOneActiveDefault() throws Exception {
        f.diet(true, List.of(), safe);

        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) VALUES('M08.DIET','M08',?,'ACTIVE',"
                                + "JSON_OBJECT('isDefault',TRUE,'treatmentRelated',FALSE,'restrictions',JSON_ARRAY()))",
                        "Mặc định trùng " + TestUsers.nextId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void FR_001_defaultDietCannotBeTreatmentRelated_norDeactivated_norUnset() throws Exception {
        f.asDoctor();
        f.send(
                        post(DIETS),
                        NutritionCatalogFixture.dietBody("Mặc định " + TestUsers.nextId(), true, true, List.of(), safe))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONDITION_NOT_MET"));

        long def = f.diet(true, List.of(), safe);
        f.send(post(DIETS + "/" + def + "/deactivate"), "{\"reason\":\"Thử\"}").andExpect(status().isConflict());
        f.send(
                        put(DIETS + "/" + def).header("If-Match", etag(def)),
                        NutritionCatalogFixture.dietBody(name(def), false, false, List.of(), safe))
                .andExpect(status().isConflict());
        assertThat(isDefault(def)).isTrue();
    }

    @Test
    void FR_001_FR_005_safeDishMustBeActiveAndFreeOfRestrictions() throws Exception {
        long sugary = f.activeDish(List.of(sugar), List.of(base));
        long draft = f.draftDish(List.of(), List.of(base));
        f.asDoctor();

        f.send(
                        post(DIETS),
                        NutritionCatalogFixture.dietBody(
                                "Tiểu đường " + TestUsers.nextId(), true, false, List.of(sugar), sugary))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONDITION_NOT_MET"));
        f.send(
                        post(DIETS),
                        NutritionCatalogFixture.dietBody("Mềm " + TestUsers.nextId(), false, false, List.of(), draft))
                .andExpect(status().isConflict());
        f.send(
                        post(DIETS),
                        "{\"name\":\"Không món " + TestUsers.nextId()
                                + "\",\"treatmentRelated\":false,\"isDefault\":false,\"restrictions\":[],\"safeDishId\":null}")
                .andExpect(status().isConflict());

        long diabetic = f.diet(false, List.of(sugar), safe);
        f.send(
                        put(DIETS + "/" + diabetic).header("If-Match", etag(diabetic)),
                        NutritionCatalogFixture.dietBody(name(diabetic), true, false, List.of(sugar), sugary))
                .andExpect(status().isConflict());
    }

    @Test
    void FR_002_nonDefaultDietDeactivatesWithReason() throws Exception {
        long diet = f.diet(false, List.of(), safe);
        f.send(post(DIETS + "/" + diet + "/deactivate"), "{}").andExpect(status().is(422));
        f.send(post(DIETS + "/" + diet + "/deactivate"), "{\"reason\":\"Không còn dùng\"}")
                .andExpect(status().isOk());

        mvc.perform(get(DIETS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + diet + ")].status").value("INACTIVE"));
    }

    @Test
    void FR_003_doctorAddsFoodIngredientToSharedAllergenCatalog() throws Exception {
        String name = "Nấm hương " + TestUsers.nextId();
        f.asKitchen();
        f.send(post(NutritionCatalogFixture.BASE + "/food-ingredients"), "{\"name\":\"" + name + "\"}")
                .andExpect(status().isForbidden());

        f.asDoctor();
        f.send(
                        post(NutritionCatalogFixture.BASE + "/food-ingredients"),
                        "{\"name\":\"" + name + "\",\"description\":\"\"}")
                .andExpect(status().isCreated());

        assertThat(jdbc.queryForObject(
                        "SELECT attributes ->> '$.allergenGroup' FROM catalog_item WHERE catalog_type = 'M01.ALLERGEN'"
                                + " AND name=? AND status='ACTIVE'",
                        String.class,
                        name))
                .isEqualTo("FOOD");
    }

    private boolean isDefault(long id) {
        return "true"
                .equals(jdbc.queryForObject(
                        "SELECT attributes->>'$.isDefault' FROM catalog_item WHERE id=?", String.class, id));
    }

    private String name(long id) {
        return jdbc.queryForObject("SELECT name FROM catalog_item WHERE id=?", String.class, id);
    }

    private String etag(long id) {
        return "\"" + jdbc.queryForObject("SELECT version FROM catalog_item WHERE id=?", Integer.class, id) + "\"";
    }
}
