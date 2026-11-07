package com.eldercare.backend.support.fake.nutrition;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.nutrition.api.NutritionStatisticsApi;
import com.eldercare.backend.nutrition.api.dto.FamilyFoodReportView;
import com.eldercare.backend.nutrition.api.dto.MealSlipReportView;
import com.eldercare.backend.nutrition.api.dto.PortionReportView;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

/** Bản giả {@link NutritionStatisticsApi} cho test của 016; trả dòng do test đặt, không lọc phạm vi hay ngày. */
public class FakeNutritionStatisticsApi implements NutritionStatisticsApi {

    private List<MealSlipReportView> slips = List.of();
    private List<PortionReportView> portions = List.of();
    private List<FamilyFoodReportView> familyFoods = List.of();

    public FakeNutritionStatisticsApi slips(List<MealSlipReportView> values) {
        slips = List.copyOf(values);
        return this;
    }

    public FakeNutritionStatisticsApi portions(List<PortionReportView> values) {
        portions = List.copyOf(values);
        return this;
    }

    public FakeNutritionStatisticsApi familyFoods(List<FamilyFoodReportView> values) {
        familyFoods = List.copyOf(values);
        return this;
    }

    @Override
    public Stream<MealSlipReportView> slips(ScopeFilter scope, LocalDate from, LocalDate to) {
        return slips.stream();
    }

    @Override
    public Stream<PortionReportView> portions(ScopeFilter scope, LocalDate from, LocalDate to) {
        return portions.stream();
    }

    @Override
    public Stream<FamilyFoodReportView> familyFoods(ScopeFilter scope, LocalDate from, LocalDate to) {
        return familyFoods.stream();
    }
}
