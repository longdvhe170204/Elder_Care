package com.eldercare.backend.nutrition.api;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.nutrition.api.dto.FamilyFoodReportView;
import com.eldercare.backend.nutrition.api.dto.MealSlipReportView;
import com.eldercare.backend.nutrition.api.dto.PortionReportView;
import java.time.LocalDate;
import java.util.stream.Stream;

/** Số liệu dinh dưỡng cho báo cáo 016 (module-interfaces mục 1.5); lọc theo phạm vi của người xem. */
public interface NutritionStatisticsApi {

    Stream<MealSlipReportView> slips(ScopeFilter scope, LocalDate from, LocalDate to);

    Stream<PortionReportView> portions(ScopeFilter scope, LocalDate from, LocalDate to);

    Stream<FamilyFoodReportView> familyFoods(ScopeFilter scope, LocalDate from, LocalDate to);
}
