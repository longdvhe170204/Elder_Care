package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.nutrition.api.NutritionStatisticsApi;
import com.eldercare.backend.nutrition.api.dto.FamilyFoodReportView;
import com.eldercare.backend.nutrition.api.dto.FamilyFoodStatus;
import com.eldercare.backend.nutrition.api.dto.MealSlipReportView;
import com.eldercare.backend.nutrition.api.dto.MealSlipStatus;
import com.eldercare.backend.nutrition.api.dto.PortionReportView;
import com.eldercare.backend.nutrition.internal.familyfood.domain.FamilyFood;
import com.eldercare.backend.nutrition.internal.familyfood.persistence.FamilyFoodStore;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.events.ResidentStatusChanged;
import com.eldercare.backend.support.TestUsers;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 011 T040 (module-interfaces 1.5; FR-035, FR-047, FR-048, FR-052): bản cài thật {@link NutritionStatisticsApi} cho 016
 * đọc từ lần chốt, phiếu, phát sinh, đồ ăn gia đình của chính 011, lọc theo phạm vi và ngày.
 */
class NutritionStatisticsApiIT extends SlipTestBase {

    @Autowired
    NutritionStatisticsApi statistics;

    @Autowired
    FamilyFoodStore foods;

    private ScopeFilter floors(long... ids) {
        Set<Long> set = new HashSet<>();
        for (long id : ids) {
            set.add(id);
        }
        return new ScopeFilter(false, set, Set.of(), Set.of());
    }

    @Test
    void FR_035_portions_countByFloorAndDiet_thenAdjustmentRowAfterClosing_scopeFiltersFloors() throws Exception {
        publishedWeek();
        residentIn(room2);
        long leaving = residentIn(room2);
        residentIn(room3);
        clock.setTo(at(LocalTime.of(9, 30)));
        close(dailyMeal(lunch)).andExpect(status().isCreated());

        List<PortionReportView> closed =
                statistics.portions(ScopeFilter.FACILITY, day, day).toList();
        assertThat(closed).allMatch(p -> p.mealId() == lunch && p.date().equals(day) && !p.adjustment());
        assertThat(sum(closed, floor2, false)).isEqualTo(2);
        assertThat(sum(closed, floor3, false)).isEqualTo(1);

        clock.setTo(at(LocalTime.of(10, 0)));
        setStatus(leaving, "HOSPITALIZED");
        handlers.onResidentStatusChanged(new ResidentStatusChanged(
                leaving,
                ResidentStatus.IN_RESIDENCE,
                ResidentStatus.HOSPITALIZED,
                null,
                TestUsers.nextId(),
                at(LocalTime.of(10, 0))));

        List<PortionReportView> after =
                statistics.portions(floors(floor2), day, day).toList();
        assertThat(after).allMatch(p -> p.floorAreaId() == floor2);
        assertThat(sum(after, floor2, false)).isEqualTo(2);
        assertThat(sum(after, floor2, true)).isEqualTo(-1);
        assertThat(statistics.portions(ScopeFilter.FACILITY, day.plusDays(1), day.plusDays(1)))
                .isEmpty();
    }

    @Test
    void FR_047_slips_statusLateAndDiscrepancy_scopeFiltersFloors() throws Exception {
        publishedWeek();
        residentIn(room2);
        residentIn(room3);
        clock.setTo(at(LocalTime.of(9, 30)));
        close(dailyMeal(lunch)).andExpect(status().isCreated());
        long slip2 = slipId(dailyMeal(lunch), floor2);
        long slip3 = slipId(dailyMeal(lunch), floor3);

        clock.setTo(at(LocalTime.of(10, 30)));
        f.asKitchen();
        slipCommand(slip3, "mark-prepared", "{}").andExpect(status().isOk());
        sample(dailyMeal(lunch));
        slipCommand(slip3, "deliver", "{}").andExpect(status().isOk());
        as(receiverOf(floor3));
        slipCommand(slip3, "report-discrepancy", "{\"content\":\"Thiếu 1 suất\"}")
                .andExpect(status().isOk());

        clock.setTo(at(LocalTime.of(11, 45)));
        prepareAndDeliver(dailyMeal(lunch), slip2, List.of());

        List<MealSlipReportView> all =
                statistics.slips(ScopeFilter.FACILITY, day, day).toList();
        MealSlipReportView late = one(all, slip2);
        assertThat(late.status()).isEqualTo(MealSlipStatus.DELIVERED);
        assertThat(late.late()).isTrue();
        assertThat(late.hasDiscrepancy()).isFalse();
        assertThat(late.deliveredAt())
                .isBetween(at(LocalTime.of(11, 45)), at(LocalTime.of(11, 45)).plusSeconds(5));
        assertThat(late.floorAreaId()).isEqualTo(floor2);
        assertThat(late.mealId()).isEqualTo(lunch);
        MealSlipReportView flagged = one(all, slip3);
        assertThat(flagged.status()).isEqualTo(MealSlipStatus.HAS_DISCREPANCY);
        assertThat(flagged.late()).isFalse();
        assertThat(flagged.hasDiscrepancy()).isTrue();

        assertThat(statistics.slips(floors(floor3), day, day).toList())
                .extracting(MealSlipReportView::slipId)
                .containsExactly(slip3);
    }

    @Test
    void FR_052_familyFoods_filteredByDateAndScope() throws Exception {
        long a = residentIn(room2);
        long b = residentIn(room3);
        long foodA = food(a, FamilyFoodStatus.ALLOWED);
        long foodB = food(b, FamilyFoodStatus.NOT_ALLOWED);

        List<FamilyFoodReportView> all =
                statistics.familyFoods(ScopeFilter.FACILITY, day, day).toList();
        assertThat(all).extracting(FamilyFoodReportView::id).contains(foodA, foodB);
        assertThat(all.stream().filter(v -> v.id() == foodB).findFirst().orElseThrow())
                .satisfies(v -> {
                    assertThat(v.residentId()).isEqualTo(b);
                    assertThat(v.status()).isEqualTo(FamilyFoodStatus.NOT_ALLOWED);
                    assertThat(v.receivedAt()).isEqualTo(at(LocalTime.of(10, 0)));
                });
        assertThat(statistics.familyFoods(floors(floor2), day, day).toList())
                .extracting(FamilyFoodReportView::id)
                .containsExactly(foodA);
        assertThat(statistics
                        .familyFoods(new ScopeFilter(false, Set.of(), Set.of(), Set.of(b)), day, day)
                        .toList())
                .extracting(FamilyFoodReportView::id)
                .containsExactly(foodB);
        assertThat(statistics.familyFoods(ScopeFilter.FACILITY, day.plusDays(1), day.plusDays(1)))
                .isEmpty();
    }

    private long food(long resident, FamilyFoodStatus status) {
        return foods.create(
                        resident,
                        status,
                        FamilyFood.recorded(
                                "Người thân",
                                null,
                                "Bánh",
                                "Bánh quy",
                                List.of(),
                                true,
                                at(LocalTime.of(20, 0)),
                                "Tủ lạnh",
                                at(LocalTime.of(10, 0)),
                                List.of()),
                        1)
                .id();
    }

    private static int sum(List<PortionReportView> rows, long floor, boolean adjustment) {
        return rows.stream()
                .filter(p -> p.floorAreaId() == floor && p.adjustment() == adjustment)
                .mapToInt(PortionReportView::portionCount)
                .sum();
    }

    private static MealSlipReportView one(List<MealSlipReportView> rows, long slipId) {
        return rows.stream().filter(v -> v.slipId() == slipId).findFirst().orElseThrow();
    }
}
