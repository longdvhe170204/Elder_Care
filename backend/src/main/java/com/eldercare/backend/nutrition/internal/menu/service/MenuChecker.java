package com.eldercare.backend.nutrition.internal.menu.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.nutrition.internal.assignment.domain.DietAssignment;
import com.eldercare.backend.nutrition.internal.assignment.persistence.DietAssignmentStore;
import com.eldercare.backend.nutrition.internal.catalog.domain.Diet;
import com.eldercare.backend.nutrition.internal.catalog.domain.Dish;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishStatus;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishVersionContent;
import com.eldercare.backend.nutrition.internal.catalog.domain.Meal;
import com.eldercare.backend.nutrition.internal.catalog.persistence.NutritionCatalogStore;
import com.eldercare.backend.nutrition.internal.conflict.domain.ConflictCheck.AllergyItem;
import com.eldercare.backend.nutrition.internal.menu.domain.AllergyWarningRule;
import com.eldercare.backend.nutrition.internal.menu.domain.AllergyWarningRule.AllergyWarning;
import com.eldercare.backend.nutrition.internal.menu.domain.AllergyWarningRule.DietUser;
import com.eldercare.backend.nutrition.internal.menu.domain.CoverageCheck;
import com.eldercare.backend.nutrition.internal.menu.domain.CoverageCheck.DietInUse;
import com.eldercare.backend.nutrition.internal.menu.domain.CoverageCheck.Gap;
import com.eldercare.backend.nutrition.internal.menu.domain.CoverageCheck.MealServing;
import com.eldercare.backend.nutrition.internal.menu.domain.DishRepetitionCheck;
import com.eldercare.backend.nutrition.internal.menu.domain.DishRepetitionCheck.Repetition;
import com.eldercare.backend.nutrition.internal.menu.domain.MenuDish;
import com.eldercare.backend.nutrition.internal.menu.persistence.MenuStore;
import com.eldercare.backend.resident.api.HealthItemApi;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.HealthItemType;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.spi.ContractApi;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * Dựng dữ liệu cho ba hàm thuần kiểm tra thực đơn (research M-07) với dữ liệu tại thời điểm gọi: bữa Hiệu lực, chế độ ăn
 * đang có người dùng và người dùng của từng chế độ (FR-027: bản gán Hiệu lực, Chờ hiệu lực giao với tuần của người chưa ở
 * trạng thái cuối; người chưa gán dùng chế độ mặc định, chủ dự án chốt 2026-11-06 theo chữ spec), dị ứng Hiệu lực (001),
 * loại lưu trú trong tuần (004, không có hợp đồng thì coi là nội trú như 005), thực đơn Công bố liền kề, CFG-M08-03.
 */
@Component
class MenuChecker {

    private static final Set<ResidentStatus> NOT_FINAL = EnumSet.of(
            ResidentStatus.ADMISSION_IN_PROGRESS,
            ResidentStatus.IN_RESIDENCE,
            ResidentStatus.TEMPORARILY_ABSENT,
            ResidentStatus.OFF_SITE_ACTIVITY,
            ResidentStatus.HOSPITALIZED);

    private final NutritionCatalogStore catalog;
    private final DietAssignmentStore assignments;
    private final MenuStore menus;
    private final ResidentApi residents;
    private final HealthItemApi healthItems;
    private final ContractApi contracts;
    private final ParameterApi parameters;
    private final BusinessTime time;

    MenuChecker(
            NutritionCatalogStore catalog,
            DietAssignmentStore assignments,
            MenuStore menus,
            ResidentApi residents,
            HealthItemApi healthItems,
            ContractApi contracts,
            ParameterApi parameters,
            BusinessTime time) {
        this.catalog = catalog;
        this.assignments = assignments;
        this.menus = menus;
        this.residents = residents;
        this.healthItems = healthItems;
        this.contracts = contracts;
        this.parameters = parameters;
        this.time = time;
    }

    /** Kết quả một lần kiểm tra (FR-025 a, b, c). */
    record Outcome(List<Gap> gaps, List<Repetition> repetitions, List<AllergyWarning> allergies) {

        List<String> warningCodes() {
            return Stream.concat(
                            repetitions.stream().map(Repetition::code),
                            allergies.stream().map(AllergyWarning::code))
                    .toList();
        }
    }

    /** Người dùng của mỗi chế độ ăn trong tuần (FR-027). */
    record WeekUsers(Map<Long, List<DietUser>> byDiet, List<DietInUse> dietsInUse) {

        int count(long dietId) {
            return byDiet.getOrDefault(dietId, List.of()).size();
        }
    }

    Outcome check(LocalDate weekStart, List<MenuDish> dishes) {
        WeekUsers users = users(weekStart);
        return new Outcome(
                CoverageCheck.gaps(weekStart, dishes, users.dietsInUse(), meals()),
                DishRepetitionCheck.find(weekStart, dishes, neighbours(weekStart), repetitionDays()),
                allergies(dishes, users));
    }

    /** Đổi món (FR-032): độ phủ chỉ của bữa bị đổi, cảnh báo như lần gửi. */
    Outcome checkSlot(LocalDate weekStart, List<MenuDish> dishes, LocalDate date, long mealId) {
        WeekUsers users = users(weekStart);
        return new Outcome(
                CoverageCheck.gapsAt(date, mealId, dishes, users.dietsInUse(), meals()),
                DishRepetitionCheck.find(weekStart, dishes, neighbours(weekStart), repetitionDays()),
                allergies(dishes, users));
    }

    WeekUsers users(LocalDate weekStart) {
        Optional<Diet> defaultDiet = catalog.diets().stream()
                .filter(d -> d.active() && d.attributes().isDefault())
                .findFirst();
        List<DietAssignment> effective = assignments.effectiveBefore(time.startOfDay(weekStart.plusDays(7))).stream()
                .filter(a -> !residents.isFinalStatus(a.residentId()))
                .toList();
        Map<Long, List<AllergyItem>> allergyCache = new HashMap<>();
        Map<Long, List<DietUser>> byDiet = new LinkedHashMap<>();
        Map<Long, boolean[]> types = new LinkedHashMap<>();
        Set<Long> assigned = new HashSet<>();
        for (DietAssignment a : effective) {
            assigned.add(a.residentId());
            byDiet.computeIfAbsent(a.dietId(), k -> new ArrayList<>())
                    .add(new DietUser(
                            a.residentId(),
                            allergies(a.residentId(), allergyCache),
                            new HashSet<>(a.personalRestrictions())));
            boolean[] flags = types.computeIfAbsent(a.dietId(), k -> new boolean[2]);
            for (int day = 0; day < 7; day++) {
                boolean dayCare = contracts
                        .currentResidenceType(a.residentId(), weekStart.plusDays(day))
                        .filter(t -> t == ResidenceType.DAY_CARE)
                        .isPresent();
                flags[dayCare ? 1 : 0] = true;
            }
        }
        List<DietInUse> inUse = new ArrayList<>();
        defaultDiet.ifPresent(d -> {
            List<DietUser> users = byDiet.computeIfAbsent(d.id(), k -> new ArrayList<>());
            for (long resident : residents.byStatus(NOT_FINAL)) {
                if (!assigned.contains(resident)) {
                    users.add(new DietUser(resident, allergies(resident, allergyCache), Set.of()));
                }
            }
            inUse.add(new DietInUse(d.id(), true, false, false));
        });
        types.forEach((dietId, flags) -> {
            if (defaultDiet.map(d -> d.id() != dietId).orElse(true)) {
                inUse.add(new DietInUse(dietId, false, flags[0], flags[1]));
            }
        });
        return new WeekUsers(byDiet, inUse);
    }

    /**
     * Món và món thay thế phải là món Hiệu lực (FR-024, FR-031) và thỏa FR-005 với chế độ ăn; ô ngoài tuần, bữa hoặc
     * chế độ ăn không có trong danh mục là giá trị sai (422).
     */
    List<UnmetCondition> dishProblems(LocalDate weekStart, List<MenuDish> dishes) {
        Set<String> notActive = new LinkedHashSet<>();
        Set<String> notSuitable = new LinkedHashSet<>();
        Map<Long, Diet> diets = new HashMap<>();
        Set<Long> meals = new HashSet<>();
        meals().forEach(m -> meals.add(m.mealId()));
        for (MenuDish row : dishes) {
            if (row.date() == null
                    || row.date().isBefore(weekStart)
                    || row.date().isAfter(weekStart.plusDays(6))) {
                throw new InvalidValueException("Ngày " + row.date() + " không thuộc tuần của thực đơn");
            }
            if (!meals.contains(row.mealId())) {
                throw new InvalidValueException("Bữa " + row.mealId() + " không có trong danh mục bữa Hiệu lực");
            }
            Diet diet = diets.computeIfAbsent(row.dietId(), this::diet);
            for (long dishId : dishIds(row)) {
                Dish dish = dish(dishId);
                if (dish.status() != DishStatus.ACTIVE) {
                    notActive.add(dish.name());
                } else if (!suitable(dish, diet)) {
                    notSuitable.add(dish.name() + " – " + diet.name());
                }
            }
        }
        List<UnmetCondition> out = new ArrayList<>();
        if (!notActive.isEmpty()) {
            out.add(new UnmetCondition(
                    "DISH_NOT_ACTIVE", "Món chưa Hiệu lực hoặc đã Ngừng, cần thay: " + String.join(", ", notActive)));
        }
        if (!notSuitable.isEmpty()) {
            out.add(new UnmetCondition(
                    "DISH_NOT_SUITABLE",
                    "Món không phù hợp hoặc chứa hạn chế của chế độ ăn (FR-005): " + String.join(", ", notSuitable)));
        }
        return out;
    }

    /** Ô có món hoặc món thay thế không còn Hiệu lực (bản sao từ thực đơn trước, FR-024). */
    List<MenuDish> toReplace(List<MenuDish> dishes) {
        return dishes.stream()
                .filter(row -> dishIds(row).stream().anyMatch(id -> dish(id).status() != DishStatus.ACTIVE))
                .toList();
    }

    List<MealServing> meals() {
        return catalog.activeMeals().stream()
                .map(m -> new MealServing(
                        m.id(),
                        m.attributes().servesResidential(),
                        m.attributes().servesDayCare()))
                .toList();
    }

    Optional<Meal> meal(long mealId) {
        return catalog.activeMeals().stream().filter(m -> m.id() == mealId).findFirst();
    }

    private List<AllergyWarning> allergies(List<MenuDish> dishes, WeekUsers users) {
        Map<Long, Set<Long>> ingredients = new HashMap<>();
        Map<Long, Set<Long>> restrictions = new HashMap<>();
        for (MenuDish row : dishes) {
            dishIds(row).forEach(id -> ingredients.computeIfAbsent(id, this::ingredients));
            restrictions.computeIfAbsent(
                    row.dietId(), id -> new HashSet<>(diet(id).attributes().restrictions()));
        }
        return AllergyWarningRule.find(dishes, users.byDiet(), restrictions, ingredients);
    }

    private List<MenuDish> neighbours(LocalDate weekStart) {
        List<MenuDish> out = new ArrayList<>(menus.publishedDishes(weekStart.minusDays(7), weekStart.minusDays(1)));
        out.addAll(menus.publishedDishes(weekStart.plusDays(7), weekStart.plusDays(13)));
        return out;
    }

    private int repetitionDays() {
        return (int) parameters.read(ParameterCode.CFG_M08_03).asDuration().toDays();
    }

    private List<AllergyItem> allergies(long residentId, Map<Long, List<AllergyItem>> cache) {
        return cache.computeIfAbsent(
                residentId,
                id -> healthItems.active(id, HealthItemType.ALLERGY).stream()
                        .map(item -> new AllergyItem(item.allergenId().orElse(null), item.notAutoChecked()))
                        .toList());
    }

    private Set<Long> ingredients(long dishId) {
        return effective(dish(dishId))
                .map(c -> (Set<Long>) new HashSet<>(c.ingredients()))
                .orElse(Set.of());
    }

    private boolean suitable(Dish dish, Diet diet) {
        return effective(dish)
                .filter(c -> c.suitableDietIds().contains(diet.id()))
                .filter(c ->
                        c.ingredients().stream().noneMatch(diet.attributes().restrictions()::contains))
                .isPresent();
    }

    private Optional<DishVersionContent> effective(Dish dish) {
        Long version = dish.attributes().effectiveVersionId();
        return version == null
                ? Optional.empty()
                : Optional.of(catalog.version(version).content());
    }

    private Dish dish(long id) {
        try {
            return catalog.dish(id);
        } catch (NotFoundException notFound) {
            throw new InvalidValueException("Món " + id + " không có trong danh mục");
        }
    }

    private Diet diet(long id) {
        try {
            return catalog.diet(id);
        } catch (NotFoundException notFound) {
            throw new InvalidValueException("Chế độ ăn " + id + " không có trong danh mục");
        }
    }

    private static List<Long> dishIds(MenuDish row) {
        List<Long> ids = new ArrayList<>();
        ids.add(row.dishId());
        ids.addAll(row.substituteDishIds());
        return ids;
    }
}
