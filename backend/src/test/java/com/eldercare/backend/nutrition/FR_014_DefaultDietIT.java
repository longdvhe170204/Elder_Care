package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.time.BusinessTime;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 011 T008 (US2, FR-001, FR-002, FR-003, FR-005, FR-014): đúng một chế độ ăn mặc định (cột sinh {@code
 * m08_default_diet} UNIQUE), chuyển mặc định trong cùng lệnh, món an toàn bắt buộc và không chứa hạn chế.
 */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
@Import(SwitchableUserConfig.class)
class FR_014_DefaultDietIT extends MySqlIntegrationTestBase {

    private static final String DIETS = NutritionCatalogFixture.BASE + "/diets";

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
