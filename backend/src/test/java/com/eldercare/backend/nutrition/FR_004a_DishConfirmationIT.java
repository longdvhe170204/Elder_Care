package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.nutrition.api.events.DishConfirmationRequested;
import com.eldercare.backend.nutrition.api.events.DishConfirmationResult;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.SwitchableUserConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
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
@Import(SwitchableUserConfig.class)
class FR_004a_DishConfirmationIT extends MySqlIntegrationTestBase {

    private static final String DISHES = NutritionCatalogFixture.BASE + "/dishes";

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

    private String etag(long id) {
        return "\"" + jdbc.queryForObject("SELECT version FROM catalog_item WHERE id=?", Integer.class, id) + "\"";
    }
}
