package com.eldercare.backend.nutrition.internal.closing.service;

import com.eldercare.backend.nutrition.internal.closing.domain.CountedPerson;
import com.eldercare.backend.nutrition.internal.closing.domain.DailyMeal;
import com.eldercare.backend.resident.api.AllergenApi;
import com.eldercare.backend.resident.api.HealthItemApi;
import com.eldercare.backend.resident.api.dto.AllergenView;
import com.eldercare.backend.resident.api.dto.HealthItemType;
import com.eldercare.backend.resident.api.dto.HealthItemView;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thông tin cho Bếp khi lập thực đơn tuần (FR-025a, Q-445): mỗi ngày, bữa có giờ dự kiến, số suất dự kiến (người cao
 * tuổi, người thân ở lại, Quản lý viện và nhân viên có ca) tính như lệnh Chốt suất với dữ liệu hiện có, và số người cao
 * tuổi theo từng loại dị ứng Hiệu lực (001), không kèm tên. Chỉ đọc, không lưu.
 */
@Component
public class MealPlanning {

    /** Tên nhóm dị ứng nhập tay không kiểm tra tự động (FR-025a). */
    static final String OTHER_ALLERGY = "Dị ứng khác";

    private final DailyMeals dailyMeals;
    private final PortionCounter counter;
    private final HealthItemApi healthItems;
    private final AllergenApi allergens;

    MealPlanning(DailyMeals dailyMeals, PortionCounter counter, HealthItemApi healthItems, AllergenApi allergens) {
        this.dailyMeals = dailyMeals;
        this.counter = counter;
        this.healthItems = healthItems;
        this.allergens = allergens;
    }

    public record AllergyCount(Long allergenId, String name, int residentCount) {}

    public record PlanningInfo(
            LocalDate date,
            long mealId,
            int residentPortions,
            int familyPortions,
            int staffPortions,
            List<AllergyCount> allergyCounts) {}

    @Transactional(readOnly = true)
    public List<PlanningInfo> week(LocalDate weekStart) {
        Map<Long, List<HealthItemView>> allergiesOf = new HashMap<>();
        Map<Long, String> names = new HashMap<>();
        List<PlanningInfo> out = new ArrayList<>();
        for (int day = 0; day < 7; day++) {
            LocalDate date = weekStart.plusDays(day);
            for (DailyMeal meal : dailyMeals.on(date)) {
                if (meal.mealTime().isEmpty()) {
                    continue;
                }
                List<CountedPerson> persons =
                        counter.count(meal, Optional.empty()).persons();
                int residents = 0;
                int family = 0;
                int staff = 0;
                Map<Long, Set<Long>> byAllergen = new LinkedHashMap<>();
                Set<Long> other = new LinkedHashSet<>();
                for (CountedPerson p : persons) {
                    switch (p.kind()) {
                        case FAMILY -> family++;
                        case STAFF -> staff++;
                        default -> {
                            residents++;
                            for (HealthItemView item : allergiesOf.computeIfAbsent(
                                    p.residentId(), id -> healthItems.active(id, HealthItemType.ALLERGY))) {
                                if (item.notAutoChecked() || item.allergenId().isEmpty()) {
                                    other.add(p.residentId());
                                } else {
                                    byAllergen
                                            .computeIfAbsent(item.allergenId().get(), a -> new LinkedHashSet<>())
                                            .add(p.residentId());
                                }
                            }
                        }
                    }
                }
                List<AllergyCount> counts = new ArrayList<>();
                byAllergen.forEach((allergen, people) -> counts.add(new AllergyCount(
                        allergen,
                        names.computeIfAbsent(
                                allergen,
                                a -> allergens
                                        .allergen(a)
                                        .map(AllergenView::name)
                                        .orElse("?")),
                        people.size())));
                counts.sort(Comparator.comparing(AllergyCount::residentCount)
                        .reversed()
                        .thenComparing(AllergyCount::name));
                if (!other.isEmpty()) {
                    counts.add(new AllergyCount(null, OTHER_ALLERGY, other.size()));
                }
                out.add(new PlanningInfo(date, meal.mealId(), residents, family, staff, counts));
            }
        }
        return out;
    }
}
