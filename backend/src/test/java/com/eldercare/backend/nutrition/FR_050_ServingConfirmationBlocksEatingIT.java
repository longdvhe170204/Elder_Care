package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 011 T032 (US6, FR-050, FR-036a; quickstart Q40): nguồn công việc {@code MEAL} cho 005 chỉ cho ghi kết quả ăn khi suất
 * đặc biệt, hoặc người trong danh sách cần đối chiếu (dị ứng loại "khác"), đã có xác nhận phục vụ thành công; người có suất
 * thường (kể cả suất chỉ có dấu) không cần.
 */
class FR_050_ServingConfirmationBlocksEatingIT extends ServingTestBase {

    /** Dị ứng loại "khác": chỉ có văn bản, không có dị nguyên (không kiểm tự động, 001 FR-018a). */
    void otherAllergy(long resident) {
        jdbc.update(
                "INSERT INTO health_item (resident_id, type, free_text, severity_id, source_id, recorded_by,"
                        + " recorded_at, pending_nutrition_check) VALUES (?, 'ALLERGY', 'một số loại nấm',"
                        + " (SELECT MIN(id) FROM catalog_item WHERE catalog_type = 'M01.ALLERGY_SEVERITY'), (SELECT MIN(id) FROM catalog_item WHERE catalog_type = 'M01.INFORMATION_SOURCE'), 1,"
                        + " NOW(6), FALSE)",
                resident);
    }

    @Test
    void FR_050_eatingResultBlockedUntilServingConfirmed_crossCheckPersonToo() throws Exception {
        publishedMenu(day.minusDays(2), plain);
        long special = softResident(room2);
        long crossCheck = residentIn(room2);
        f.activeAssignment(crossCheck, defaultDiet, "REGULAR", List.of(), time.now());
        otherAllergy(crossCheck);
        long ordinary = residentIn(room2);
        f.activeAssignment(ordinary, defaultDiet, "REGULAR", List.of(), time.now());
        long noDiet = residentIn(room2);
        closeLunchAndServe();
        long meal = dailyMeal(lunch);

        assertThat(mealSource.confirmServing(meal, special)).isFalse();
        assertThat(mealSource.confirmServing(meal, crossCheck)).isFalse();
        assertThat(mealSource.confirmServing(meal, ordinary)).isTrue();
        assertThat(mealSource.confirmServing(meal, noDiet)).isTrue();

        as(serverOf(floor2));
        confirm(specialOf(special), special, null).andExpect(status().isCreated());
        assertThat(mealSource.confirmServing(meal, special)).isTrue();
        assertThat(mealSource.confirmServing(meal, crossCheck)).isFalse();

        confirm(specialOf(crossCheck), crossCheck, null).andExpect(status().isCreated());
        assertThat(mealSource.confirmServing(meal, crossCheck)).isTrue();
    }

    @Test
    void FR_050_blockedConfirmationDoesNotCount() throws Exception {
        long tom = f.allergen();
        long shrimp = f.activeDish(List.of(tom), List.of(defaultDiet));
        publishedMenu(day.minusDays(2), shrimp);
        long a = softResident(room2);
        closeLunchAndServe();
        allergy(a, tom);

        as(serverOf(floor2));
        confirm(specialOf(a), a, "\"hasEaten\":false").andExpect(status().isCreated());

        assertThat(mealSource.confirmServing(dailyMeal(lunch), a)).isFalse();
    }
}
