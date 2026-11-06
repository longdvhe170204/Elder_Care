package com.eldercare.backend.support.fake.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.nutrition.api.dto.CurrentDietView;
import com.eldercare.backend.nutrition.api.dto.DailyMealView;
import com.eldercare.backend.nutrition.api.dto.DishByMealView;
import com.eldercare.backend.nutrition.api.dto.ExpectedPortionView;
import com.eldercare.backend.nutrition.api.dto.FamilyFoodReportView;
import com.eldercare.backend.nutrition.api.dto.FamilyFoodStatus;
import com.eldercare.backend.nutrition.api.dto.MealSlipReportView;
import com.eldercare.backend.nutrition.api.dto.MealSlipStatus;
import com.eldercare.backend.nutrition.api.dto.PortionReportView;
import com.eldercare.backend.nutrition.api.dto.Texture;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 011 T003: bản giả dùng chung của các API đọc 011 cung cấp cho 006, 007, 012, 016 trả dữ liệu do test đặt. */
class FakeNutritionApiTest {

    private static final LocalDate D1 = LocalDate.parse("2026-11-10");

    @Test
    void fakeDietApi_returnsConfiguredCurrentDiet() {
        FakeDietApi fake = new FakeDietApi();
        CurrentDietView diet = new CurrentDietView(100, 5, "Tiểu đường", Texture.SOFT, Set.of(9L), false);
        fake.current(diet);

        assertThat(fake.current(100)).contains(diet);
        assertThat(fake.current(101)).isEqualTo(Optional.empty());
        assertThat(fake.history(100)).isEmpty();
    }

    @Test
    void fakeGeneralMenuApi_filtersByDateRange() {
        FakeGeneralMenuApi fake = new FakeGeneralMenuApi();
        fake.dish(new DishByMealView(D1, 1, "Sáng", 7, "Cháo"))
                .dish(new DishByMealView(D1.plusDays(5), 1, "Sáng", 8, "Phở"))
                .dietName("Thường");

        assertThat(fake.generalMenu(D1, D1.plusDays(1)))
                .extracting(DishByMealView::dishName)
                .containsExactly("Cháo");
        assertThat(fake.dietNames()).containsExactly("Thường");
    }

    @Test
    void fakeExpectedMealApi_returnsOnlyResidentAndRange() {
        FakeExpectedMealApi fake = new FakeExpectedMealApi();
        fake.portion(100, new ExpectedPortionView(D1, 1, "Sáng", "Cháo", false))
                .portion(101, new ExpectedPortionView(D1, 1, "Sáng", "Phở", false));

        assertThat(fake.ofResident(100, D1, D1))
                .extracting(ExpectedPortionView::dishName)
                .containsExactly("Cháo");
        assertThat(fake.ofResident(100, D1.plusDays(1), D1.plusDays(2))).isEmpty();
    }

    @Test
    void fakeMealScheduleApi_knowsDailyMealsAndMembership() {
        FakeMealScheduleApi fake = new FakeMealScheduleApi();
        fake.meal(new DailyMealView(10, 1, D1, Instant.parse("2026-11-10T00:00:00Z"), false))
                .member(10, 100);

        assertThat(fake.mealsOnDate(D1)).extracting(DailyMealView::dailyMealId).containsExactly(10L);
        assertThat(fake.isInMealSchedule(10, 100)).isTrue();
        assertThat(fake.isInMealSchedule(10, 101)).isFalse();
    }

    @Test
    void fakeNutritionStatisticsApi_returnsConfiguredRows() {
        FakeNutritionStatisticsApi fake = new FakeNutritionStatisticsApi();
        MealSlipReportView slip = new MealSlipReportView(1, D1, 1, 3, MealSlipStatus.DELIVERED, null, true, false);
        PortionReportView portion = new PortionReportView(D1, 1, 3, 5, 12, false);
        FamilyFoodReportView food =
                new FamilyFoodReportView(1, 100, FamilyFoodStatus.ALLOWED, Instant.parse("2026-11-10T01:00:00Z"));
        fake.slips(List.of(slip)).portions(List.of(portion)).familyFoods(List.of(food));

        assertThat(fake.slips(ScopeFilter.FACILITY, D1, D1)).containsExactly(slip);
        assertThat(fake.portions(ScopeFilter.FACILITY, D1, D1)).containsExactly(portion);
        assertThat(fake.familyFoods(ScopeFilter.FACILITY, D1, D1)).containsExactly(food);
    }
}
