package com.eldercare.backend.nutrition.internal.menu.service;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.nutrition.internal.catalog.domain.Diet;
import com.eldercare.backend.nutrition.internal.catalog.domain.Dish;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishVersionContent;
import com.eldercare.backend.nutrition.internal.catalog.persistence.NutritionCatalogStore;
import com.eldercare.backend.nutrition.internal.conflict.domain.ConflictCheck;
import com.eldercare.backend.nutrition.internal.conflict.domain.ConflictCheck.AllergyItem;
import com.eldercare.backend.nutrition.internal.conflict.domain.ConflictType;
import com.eldercare.backend.nutrition.internal.conflict.domain.SubstituteDishSelector;
import com.eldercare.backend.nutrition.internal.menu.domain.CoverageCheck;
import com.eldercare.backend.nutrition.internal.menu.domain.CoverageCheck.DietInUse;
import com.eldercare.backend.nutrition.internal.menu.domain.CoverageCheck.Gap;
import com.eldercare.backend.nutrition.internal.menu.domain.Menu;
import com.eldercare.backend.nutrition.internal.menu.domain.MenuDish;
import com.eldercare.backend.nutrition.internal.menu.persistence.MenuStore;
import com.eldercare.backend.resident.api.HealthItemApi;
import com.eldercare.backend.resident.api.dto.HealthItemType;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.eldercare.backend.resident.api.spi.ContractApi;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * Đối chiếu một người với thực đơn chưa hết tuần theo FR-020 (FR-016, FR-022, FR-038; research M-06): thực đơn của tuần
 * hiện tại (mọi trạng thái chưa hủy) và các tuần đã Công bố, chỉ các ngày từ hôm nay. Một dòng mỗi món, mỗi loại xung
 * đột, kèm thành phần trùng và đã có món thay thế phù hợp chưa; bữa không có món của chế độ ăn trong thực đơn đã công bố.
 */
@Component
public class ResidentMenuConflicts {

    private final MenuStore menus;
    private final NutritionCatalogStore catalog;
    private final HealthItemApi healthItems;
    private final ContractApi contracts;
    private final MenuChecker checker;
    private final BusinessTime time;

    ResidentMenuConflicts(
            MenuStore menus,
            NutritionCatalogStore catalog,
            HealthItemApi healthItems,
            ContractApi contracts,
            MenuChecker checker,
            BusinessTime time) {
        this.menus = menus;
        this.catalog = catalog;
        this.healthItems = healthItems;
        this.contracts = contracts;
        this.checker = checker;
        this.time = time;
    }

    /** Xung đột của một món với một người (api-011 {@code Conflict}). */
    public record Conflict(
            LocalDate date,
            long mealId,
            long dishId,
            List<Long> ingredients,
            ConflictType type,
            boolean hasSuitableSubstitute) {

        /** Mã cảnh báo cần Bác sĩ xác nhận khi chưa có món thay thế (FR-016). */
        public String code() {
            return "CONFLICT:" + date + ":" + mealId + ":" + dishId;
        }
    }

    /** Kết quả đối chiếu FR-016: xung đột và bữa thiếu món; {@code warningCodes} phải được xác nhận khi Áp dụng. */
    public record Result(List<Conflict> conflicts, List<Gap> missingMeals) {

        public List<String> warningCodes() {
            return Stream.concat(
                            conflicts.stream()
                                    .filter(c -> !c.hasSuitableSubstitute())
                                    .map(Conflict::code),
                            missingMeals.stream().map(g -> "NO_DISH:" + g.dietId() + ":" + g.date() + ":" + g.mealId()))
                    .distinct()
                    .toList();
        }
    }

    public Result check(long residentId, long dietId, Set<Long> personalRestrictions) {
        LocalDate today = time.today();
        Diet diet = catalog.diet(dietId);
        Set<Long> dietRestrictions = new HashSet<>(diet.attributes().restrictions());
        List<AllergyItem> allergies = healthItems.active(residentId, HealthItemType.ALLERGY).stream()
                .map(i -> new AllergyItem(i.allergenId().orElse(null), i.notAutoChecked()))
                .toList();
        Map<Long, Set<Long>> ingredients = new HashMap<>();
        List<Conflict> conflicts = new ArrayList<>();
        List<Gap> missing = new ArrayList<>();
        for (Menu menu : menus.notEnded(today)) {
            boolean current = !menu.weekStart().isAfter(today);
            if (!current && !menu.status().published()) {
                continue;
            }
            List<MenuDish> rows = menu.content().dishes().stream()
                    .filter(d -> d.dietId() == dietId && !d.date().isBefore(today))
                    .toList();
            for (MenuDish row : rows) {
                Set<Long> dishIngredients = ingredients.computeIfAbsent(row.dishId(), this::ingredients);
                Set<ConflictType> types =
                        ConflictCheck.conflicts(dishIngredients, allergies, personalRestrictions, dietRestrictions);
                if (types.isEmpty()) {
                    continue;
                }
                boolean substitute = SubstituteDishSelector.choose(
                                row.substituteDishIds(),
                                id -> ConflictCheck.hasConflict(
                                        ingredients.computeIfAbsent(id, this::ingredients),
                                        allergies,
                                        personalRestrictions,
                                        dietRestrictions))
                        .isPresent();
                for (ConflictType type : types) {
                    conflicts.add(new Conflict(
                            row.date(),
                            row.mealId(),
                            row.dishId(),
                            matching(type, dishIngredients, allergies, personalRestrictions, dietRestrictions),
                            type,
                            substitute));
                }
            }
            if (menu.status().published()) {
                missing.addAll(missingMeals(menu, dietId, residentId, today));
            }
        }
        return new Result(conflicts, missing);
    }

    private List<Gap> missingMeals(Menu menu, long dietId, long residentId, LocalDate today) {
        boolean dayCare = contracts
                .currentResidenceType(residentId, today)
                .filter(t -> t == ResidenceType.DAY_CARE)
                .isPresent();
        return CoverageCheck.gaps(
                        menu.weekStart(),
                        menu.content().dishes(),
                        List.of(new DietInUse(dietId, false, !dayCare, dayCare)),
                        checker.meals())
                .stream()
                .filter(g -> !g.date().isBefore(today))
                .toList();
    }

    private static List<Long> matching(
            ConflictType type,
            Set<Long> dishIngredients,
            List<AllergyItem> allergies,
            Set<Long> personalRestrictions,
            Set<Long> dietRestrictions) {
        Set<Long> against = switch (type) {
            case ALLERGY ->
                allergies.stream()
                        .filter(a -> !a.notAutoChecked() && a.allergenId() != null)
                        .map(AllergyItem::allergenId)
                        .collect(Collectors.toSet());
            case PERSONAL_RESTRICTION -> personalRestrictions;
            case DIET_RESTRICTION -> dietRestrictions;
        };
        return dishIngredients.stream().filter(against::contains).sorted().toList();
    }

    private Set<Long> ingredients(long dishId) {
        Dish dish = catalog.dish(dishId);
        Long version = dish.attributes().effectiveVersionId();
        return Optional.ofNullable(version)
                .map(v -> catalog.version(v).content())
                .map(DishVersionContent::ingredients)
                .<Set<Long>>map(HashSet::new)
                .orElse(Set.of());
    }
}
