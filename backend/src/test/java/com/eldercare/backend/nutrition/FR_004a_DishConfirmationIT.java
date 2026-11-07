package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.nutrition.api.events.DishConfirmationRequested;
import com.eldercare.backend.nutrition.api.events.DishConfirmationResult;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 011 T008 (US2, FR-004, FR-004a, Q-308): bảng trạng thái món, bản sửa là phiên bản mới, quyền Nhân viên bếp lập / Bác
 * sĩ xác nhận, món an toàn của chế độ ăn Hiệu lực không ngừng được. Quyền thật theo Phụ lục 27 dòng "Món ăn, thực đơn".
 */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
@RecordApplicationEvents
@Import(FR_004a_DishConfirmationIT.Users.class)
class FR_004a_DishConfirmationIT extends MySqlIntegrationTestBase {

    private static final String DISHES = NutritionCatalogFixture.BASE + "/dishes";

    @TestConfiguration
    static class Users {
        @Bean
        @Primary
        SwitchableUser nutritionDishUser() {
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

    @Autowired
    ApplicationEvents events;

    NutritionCatalogFixture f;
    long diet;
    long fish;

    @BeforeEach
    void setUp() throws Exception {
        f = new NutritionCatalogFixture(mvc, jdbc, time, user);
        diet = f.baseDiet();
        fish = f.allergen();
    }

    @Test
    void FR_004_submitWithoutIngredientsOrFlag_isBlocked() throws Exception {
        f.asKitchen();
        String name = "Cá kho " + TestUsers.nextId();
        f.send(post(DISHES), NutritionCatalogFixture.dishBody(name, List.of(), false, List.of(diet)))
                .andExpect(status().isCreated());
        long id = f.dishId(name);

        f.send(post(DISHES + "/" + id + "/submit"), "{}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONDITION_NOT_MET"));
        assertThat(f.dishStatus(id)).isEqualTo("DRAFT");
    }

    @Test
    void FR_004a_returnNeedsReason_thenResubmitAndConfirmMakesDishActive() throws Exception {
        long id = f.draftDish(List.of(fish), List.of(diet));
        f.send(post(DISHES + "/" + id + "/submit"), "{}").andExpect(status().isOk());
        assertThat(f.dishStatus(id)).isEqualTo("PENDING_CONFIRMATION");
        assertThat(events.stream(DishConfirmationRequested.class)).hasSize(1);

        f.asDoctor();
        f.send(post(DISHES + "/" + id + "/return"), "{}").andExpect(status().is(422));
        f.send(post(DISHES + "/" + id + "/return"), "{\"reason\":\"Thiếu thành phần tôm\"}")
                .andExpect(status().isOk());
        assertThat(f.dishStatus(id)).isEqualTo("DRAFT");
        assertThat(events.stream(DishConfirmationResult.class)).anyMatch(e -> !e.confirmed());

        f.asKitchen();
        f.send(post(DISHES + "/" + id + "/submit"), "{}").andExpect(status().isOk());
        f.asDoctor();
        f.send(post(DISHES + "/" + id + "/confirm"), "{}").andExpect(status().isOk());

        assertThat(f.dishStatus(id)).isEqualTo("ACTIVE");
        assertThat(events.stream(DishConfirmationResult.class)).anyMatch(DishConfirmationResult::confirmed);
        mvc.perform(get(DISHES).param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].effective.ingredients[0]")
                        .value((int) fish));
    }

    @Test
    void FR_004a_kitchenCannotConfirm_doctorCannotCreate() throws Exception {
        long id = f.draftDish(List.of(fish), List.of(diet));
        f.send(post(DISHES + "/" + id + "/submit"), "{}").andExpect(status().isOk());

        f.send(post(DISHES + "/" + id + "/confirm"), "{}").andExpect(status().isForbidden());

        f.asDoctor();
        f.send(
                        post(DISHES),
                        NutritionCatalogFixture.dishBody(
                                "Món " + TestUsers.nextId(), List.of(fish), false, List.of(diet)))
                .andExpect(status().isForbidden());
    }

    @Test
    void FR_004a_revisionIsNewVersion_effectiveContentKeptUntilConfirmed() throws Exception {
        long id = f.activeDish(List.of(fish), List.of(diet));
        long mushroom = f.allergen();
        String revision = NutritionCatalogFixture.dishBody("bỏ qua", List.of(fish, mushroom), false, List.of(diet));

        f.asKitchen();
        f.send(post(DISHES + "/" + id + "/submit-revision"), "{\"content\":" + revision + "}")
                .andExpect(status().isOk());
        f.send(post(DISHES + "/" + id + "/submit-revision"), "{\"content\":" + revision + "}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("REVISION_EXISTS"));

        mvc.perform(get(DISHES).param("status", "ACTIVE"))
                .andExpect(jsonPath("$[?(@.id == " + id + ")].effective.ingredients.length()")
                        .value(1))
                .andExpect(jsonPath("$[?(@.id == " + id + ")].pendingRevision.ingredients.length()")
                        .value(2));

        f.asDoctor();
        f.send(post(DISHES + "/" + id + "/confirm-revision"), "{}").andExpect(status().isOk());
        assertThat(f.dishStatus(id)).isEqualTo("ACTIVE");
        mvc.perform(get(DISHES).param("status", "ACTIVE"))
                .andExpect(jsonPath("$[?(@.id == " + id + ")].effective.ingredients.length()")
                        .value(2));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM simple_record WHERE kind='M08.DISH_VERSION' AND entity_id=? AND locked_at IS NOT NULL",
                        Integer.class,
                        id))
                .isEqualTo(2);
    }

    @Test
    void FR_004a_returnedRevisionKeepsOldContent() throws Exception {
        long id = f.activeDish(List.of(fish), List.of(diet));
        String revision = NutritionCatalogFixture.dishBody("bỏ qua", List.of(), true, List.of(diet));
        f.asKitchen();
        f.send(post(DISHES + "/" + id + "/submit-revision"), "{\"content\":" + revision + "}")
                .andExpect(status().isOk());

        f.asDoctor();
        f.send(post(DISHES + "/" + id + "/return-revision"), "{\"reason\":\"Vẫn có cá\"}")
                .andExpect(status().isOk());

        mvc.perform(get(DISHES).param("status", "ACTIVE"))
                .andExpect(jsonPath("$[?(@.id == " + id + ")].effective.ingredients[0]")
                        .value((int) fish))
                .andExpect(jsonPath("$[?(@.id == " + id + ")].pendingRevision")
                        .value(org.hamcrest.Matchers.contains(org.hamcrest.Matchers.nullValue())));
    }

    @Test
    void FR_004a_activeDishEditChangesOnlyNameAndNote() throws Exception {
        long id = f.activeDish(List.of(fish), List.of(diet));
        f.asKitchen();
        String renamed = "Cá kho tộ " + TestUsers.nextId();

        f.send(
                        put(DISHES + "/" + id).header("If-Match", etag(id)),
                        NutritionCatalogFixture.dishBody(renamed, List.of(), true, List.of(diet)))
                .andExpect(status().isConflict());
        f.send(
                        put(DISHES + "/" + id).header("If-Match", etag(id)),
                        NutritionCatalogFixture.dishBody(renamed, List.of(fish), false, List.of(diet)))
                .andExpect(status().isOk());

        assertThat(f.dishId(renamed)).isEqualTo(id);
    }

    @Test
    void FR_004_cancelOnlyNeverActive_deactivateNeedsReasonAndBlocksSafeDish() throws Exception {
        long draft = f.draftDish(List.of(fish), List.of(diet));
        f.send(post(DISHES + "/" + draft + "/cancel"), "{}").andExpect(status().isOk());
        assertThat(f.dishStatus(draft)).isEqualTo("CANCELLED");

        long active = f.activeDish(List.of(fish), List.of(diet));
        f.asKitchen();
        f.send(post(DISHES + "/" + active + "/cancel"), "{}").andExpect(status().isConflict());
        f.send(post(DISHES + "/" + active + "/deactivate"), "{}").andExpect(status().is(422));

        long safe = f.activeDish(List.of(), List.of(diet));
        f.diet(false, List.of(), safe);
        f.asKitchen();
        f.send(post(DISHES + "/" + safe + "/deactivate"), "{\"reason\":\"Hết nguyên liệu\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("IS_SAFE_DISH"));

        f.send(post(DISHES + "/" + active + "/deactivate"), "{\"reason\":\"Ngừng phục vụ\"}")
                .andExpect(status().isOk());
        assertThat(f.dishStatus(active)).isEqualTo("INACTIVE");
    }

    private String etag(long id) {
        return "\"" + jdbc.queryForObject("SELECT version FROM catalog_item WHERE id=?", Integer.class, id) + "\"";
    }
}
