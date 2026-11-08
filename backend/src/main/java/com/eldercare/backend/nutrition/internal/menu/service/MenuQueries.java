package com.eldercare.backend.nutrition.internal.menu.service;

import com.eldercare.backend.nutrition.api.DietApi;
import com.eldercare.backend.nutrition.api.ExpectedMealApi;
import com.eldercare.backend.nutrition.api.GeneralMenuApi;
import com.eldercare.backend.nutrition.api.dto.DishByMealView;
import com.eldercare.backend.nutrition.api.dto.ExpectedPortionView;
import com.eldercare.backend.nutrition.internal.catalog.domain.Diet;
import com.eldercare.backend.nutrition.internal.catalog.persistence.NutritionCatalogStore;
import com.eldercare.backend.nutrition.internal.menu.domain.MenuDish;
import com.eldercare.backend.nutrition.internal.menu.persistence.MenuStore;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bản cài API đọc thực đơn cho 012; không chốt suất và không ghi dữ liệu (FR-028, FR-063). */
@Service
class MenuQueries implements GeneralMenuApi, ExpectedMealApi {

    private final MenuStore menus;
    private final NutritionCatalogStore catalog;
    private final DietApi diets;

    MenuQueries(MenuStore menus, NutritionCatalogStore catalog, DietApi diets) {
        this.menus = menus;
        this.catalog = catalog;
        this.diets = diets;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DishByMealView> generalMenu(LocalDate from, LocalDate to) {
        return defaultDiet()
                .map(diet -> ofDiet(diet.id(), from, to).stream()
                        .map(dish -> new DishByMealView(
                                dish.date(), dish.mealId(), mealName(dish), dish.dishId(), dishName(dish.dishId())))
                        .toList())
                .orElseGet(List::of);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> dietNames() {
        return catalog.diets().stream().filter(Diet::active).map(Diet::name).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpectedPortionView> ofResident(long residentId, LocalDate from, LocalDate to) {
        return diets.current(residentId)
                .map(diet -> ofDiet(diet.dietId(), from, to).stream()
                        .flatMap(dish -> portions(dish).stream())
                        .toList())
                .orElseGet(List::of);
    }

    private List<MenuDish> ofDiet(long dietId, LocalDate from, LocalDate to) {
        return menus.publishedDishes(from, to).stream()
                .filter(dish -> dish.dietId() == dietId)
                .toList();
    }

    private List<ExpectedPortionView> portions(MenuDish dish) {
        String meal = mealName(dish);
        List<ExpectedPortionView> result = new ArrayList<>();
        result.add(new ExpectedPortionView(dish.date(), dish.mealId(), meal, dishName(dish.dishId()), false));
        dish.substituteDishIds()
                .forEach(substitute -> result.add(
                        new ExpectedPortionView(dish.date(), dish.mealId(), meal, dishName(substitute), true)));
        return result;
    }

    private String mealName(MenuDish dish) {
        return catalog.meal(dish.mealId()).name();
    }

    private String dishName(long dishId) {
        return catalog.dish(dishId).name();
    }

    private Optional<Diet> defaultDiet() {
        return catalog.diets().stream()
                .filter(diet -> diet.active() && diet.attributes().isDefault())
                .findFirst();
    }
}
