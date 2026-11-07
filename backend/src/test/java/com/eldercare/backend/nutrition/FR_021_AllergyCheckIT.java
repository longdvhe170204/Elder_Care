package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.events.AllergyRecorded;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 011 T012 (US1, FR-021, BR-M01-12; team-plan mục 8 dòng "D (006, 011)"): dị ứng chọn từ danh mục mới ghi nhận thì 011
 * đối chiếu rồi gỡ cờ chờ kiểm tra dinh dưỡng của mục qua {@code HealthItemApi.markChecked(itemId, NUTRITION)}; phát lại
 * (job 15 phút của 001) không lỗi. Không {@code @Transactional}: sự kiện ngoài transaction chạy listener ngay
 * ({@code fallbackExecution}), như lần phát lại của job 001.
 */
@AutoConfigureMockMvc
@WithMockUser
@Import(FR_021_AllergyCheckIT.Users.class)
class FR_021_AllergyCheckIT extends MySqlIntegrationTestBase {

    @TestConfiguration
    static class Users {
        @Bean
        @Primary
        SwitchableUser nutritionAllergyUser() {
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
    ApplicationEventPublisher events;

    @Test
    void FR_021_newAllergy_nutritionCheckFlagCleared_replayIsHarmless() {
        NutritionCatalogFixture f = new NutritionCatalogFixture(mvc, jdbc, time, user);
        user.set(new TestUsers(jdbc, time).staff(Role.NURSING_STAFF).user());
        long resident = f.resident("IN_RESIDENCE");
        long allergen = f.allergen();
        jdbc.update(
                "INSERT INTO health_item (resident_id, type, allergen_id, severity_id, source_id, recorded_by,"
                        + " recorded_at, pending_nutrition_check) VALUES (?, 'ALLERGY', ?,"
                        + " (SELECT MIN(id) FROM catalog_item WHERE catalog_type = 'M01.ALLERGY_SEVERITY'), (SELECT MIN(id) FROM catalog_item WHERE catalog_type = 'M01.INFORMATION_SOURCE'), 1,"
                        + " NOW(6), TRUE)",
                resident,
                allergen);
        long item = jdbc.queryForObject(
                "SELECT id FROM health_item WHERE resident_id = ? AND allergen_id = ?", Long.class, resident, allergen);

        events.publishEvent(new AllergyRecorded(item, resident, allergen));
        events.publishEvent(new AllergyRecorded(item, resident, allergen));

        assertThat(jdbc.queryForObject(
                        "SELECT pending_nutrition_check FROM health_item WHERE id = ?", Boolean.class, item))
                .isFalse();
    }
}
