package com.eldercare.backend.nutrition;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.support.TestUsers.TestUser;
import com.eldercare.backend.support.fake.incident.FakeIncidentCommands;
import com.eldercare.backend.support.fake.incident.FakeIncidentTypes;
import java.time.LocalTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Nền chung cho IT xác nhận phục vụ của 011 Phase 8 (trên {@link SlipTestBase}, một context): 007 chưa có bản cài
 * {@code IncidentCommandApi} nên dùng bản giả dùng chung ({@code support/fake/incident}); người phục vụ có quyền "Xác nhận
 * phục vụ suất đặc biệt" chỉ ở một tầng/khu (spy {@code PermissionApi}).
 */
@Import(ServingTestBase.IncidentFakes.class)
abstract class ServingTestBase extends SlipTestBase {

    @TestConfiguration
    static class IncidentFakes {
        @Bean
        @Primary
        FakeIncidentCommands servingIncidentCommands() {
            return new FakeIncidentCommands();
        }

        @Bean
        @Primary
        FakeIncidentTypes servingIncidentTypes() {
            return new FakeIncidentTypes();
        }
    }

    @Autowired
    FakeIncidentCommands incidentCommands;

    @Autowired
    com.eldercare.backend.nutrition.internal.serving.service.MealTaskSource mealSource;

    /** Chia suất đặc biệt: người phục vụ ở {@code floor}. */
    TestUser serverOf(long floor) {
        return staffOf(floor, Function.SPECIAL_MEAL_SERVICE);
    }

    ResultActions confirm(long special, long servedResident, String extra) throws Exception {
        return send(
                post("/api/v1/special-portions/" + special + "/confirm-serving"),
                "{\"residentId\":" + servedResident + (extra == null ? "" : "," + extra) + "}");
    }

    /** Chốt bữa trưa lúc 09:30 và đặt đồng hồ 11:15 (sau giờ bữa, đang phục vụ). */
    void closeLunchAndServe() throws Exception {
        clock.setTo(at(LocalTime.of(9, 30)));
        close(dailyMeal(lunch)).andExpect(status().isCreated());
        clock.setTo(at(LocalTime.of(11, 15)));
    }

    String result(long special) {
        List<String> rows = jdbc.queryForList(
                "SELECT content->>'$.result' FROM record_event WHERE kind = 'M08.SERVING_CONFIRMATION'"
                        + " AND entity_id = ? ORDER BY id",
                String.class,
                special);
        return rows.isEmpty() ? null : rows.getLast();
    }

    int confirmations(long special) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM record_event WHERE kind = 'M08.SERVING_CONFIRMATION' AND entity_id = ?",
                Integer.class,
                special);
    }
}
