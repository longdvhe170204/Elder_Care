package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.nutrition.api.events.PortionsClosed;
import com.eldercare.backend.nutrition.api.events.SafeDishApplied;
import com.eldercare.backend.nutrition.internal.schedule.service.M08JobService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 011 T022 (US4, FR-036, FR-037a, SC-012; quickstart Q29, Q30; tên {@code FR_037a_MonAnToanIT} trong tasks): suất thiếu món
 * thay thế khi chốt ngoài giờ hành chính tự dùng món an toàn ngay, Bác sĩ được báo Nhẹ; trong giờ hành chính thì tới mốc
 * CFG-M08-05 trước giờ bữa mới áp, kèm phát sinh đổi suất; món an toàn cũng xung đột thì báo điều dưỡng phụ trách.
 */
class FR_037a_SafeDishIT extends ClosingTestBase {

    @Autowired
    M08JobService jobs;

    long shrimp;
    long shrimpSoup;

    /** Thực đơn tuần có món tôm không món thay thế ở bữa sáng và bữa trưa của ngày {@link #day}. */
    void shrimpMenu() throws Exception {
        shrimp = f.allergen();
        shrimpSoup = f.activeDish(List.of(shrimp), List.of(defaultDiet));
        LocalDate week = day.minusDays(2);
        long menu = createMenu(week);
        List<String> rows = new ArrayList<>(fullWeek(week, defaultDiet, plain));
        rows.removeIf(r -> r.contains("\"date\":\"" + day + "\"")
                && (r.contains("\"mealId\":" + breakfast + ",") || r.contains("\"mealId\":" + lunch + ",")));
        rows.add(row(day, breakfast, defaultDiet, shrimpSoup));
        rows.add(row(day, lunch, defaultDiet, shrimpSoup));
        putContent(menu, rows).andExpect(status().isOk());
        submit(menu, codes(menu)).andExpect(status().isOk());
        publish(menu, List.of()).andExpect(status().isOk());
    }

    @Test
    void FR_037a_closingOutsideOfficeHours_appliesSafeDishImmediately() throws Exception {
        shrimpMenu();
        long a = residentIn(room2);
        allergy(a, shrimp);
        clock.setTo(at(LocalTime.of(4, 30)));

        close(dailyMeal(breakfast))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.specialPortions").value(1));
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_CONTAINS(content->'$.flags', '\"SAFE_DISH\"') FROM simple_record"
                                + " WHERE kind = 'M08.SPECIAL_PORTION' AND resident_id = ?",
                        Integer.class,
                        a))
                .isEqualTo(1);
        assertThat(events.stream(PortionsClosed.class))
                .singleElement()
                .satisfies(e -> assertThat(e.safeDishApplied()).isEqualTo(1));
    }

    @Test
    void FR_037a_inOfficeHours_missingSubstitute_untilReminderLead_thenSafeDishByJob() throws Exception {
        shrimpMenu();
        long a = residentIn(room2);
        allergy(a, shrimp);
        clock.setTo(at(LocalTime.of(9, 30)));

        close(dailyMeal(lunch))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flags", hasItem("MISSING_SUBSTITUTE_DISH")));
        jobs.runMinuteTasks(at(LocalTime.of(10, 29)));
        adjustmentsOf(dailyMeal(lunch)).andExpect(jsonPath("$").isEmpty());

        clock.setTo(at(LocalTime.of(10, 30)));
        jobs.runMinuteTasks(at(LocalTime.of(10, 30)));
        jobs.runMinuteTasks(at(LocalTime.of(10, 31)));

        adjustmentsOf(dailyMeal(lunch))
                .andExpect(jsonPath("$[?(@.changeType == 'PORTION_CHANGED')].floorAreaId")
                        .value(hasItem((int) floor2)));
        assertThat(events.stream(SafeDishApplied.class))
                .singleElement()
                .satisfies(e -> assertThat(e.applied()).isEqualTo(1));
    }

    @Test
    void FR_037a_safeDishAlsoConflicts_notifiesAssignedNurse() throws Exception {
        shrimpMenu();
        long a = residentIn(room2);
        allergy(a, shrimp);
        long nut = f.allergen();
        allergy(a, nut);
        jdbc.update(
                "UPDATE catalog_item SET attributes = JSON_SET(attributes, '$.safeDishId', ?) WHERE id = ?",
                f.activeDish(List.of(nut), List.of(defaultDiet)),
                defaultDiet);
        clock.setTo(at(LocalTime.of(4, 30)));

        close(dailyMeal(breakfast))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flags", hasItem("MISSING_SUBSTITUTE_DISH")));
        assertThat(events.stream(PortionsClosed.class)).singleElement().satisfies(e -> {
            assertThat(e.safeDishConflicts()).containsExactly(a);
            assertThat(e.notificationRequests())
                    .flatExtracting(r -> r.recipientGroups())
                    .contains(new RecipientGroup.AssignedNurse(a, new Substitution.Default()));
        });
    }
}
