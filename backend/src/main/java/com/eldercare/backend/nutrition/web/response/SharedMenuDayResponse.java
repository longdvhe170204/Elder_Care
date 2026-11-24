package com.eldercare.backend.nutrition.web.response;

import com.eldercare.backend.nutrition.api.dto.DishByMealView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Một ngày của thực đơn chung đã công bố (api-011 {@code SharedMenuDay}; FR-028): các bữa và món theo thứ tự. */
@Schema(name = "SharedMenuDay")
public record SharedMenuDayResponse(LocalDate date, List<Meal> meals) {

    @Schema(name = "SharedMenuMeal")
    public record Meal(long mealId, String name, String expectedTime, List<Dish> dishes) {}

    @Schema(name = "SharedMenuDish")
    public record Dish(long id, String name, String description, String imageUrl) {}

    /** Gom các dòng (đã theo thứ tự ngày, bữa, món) thành ngày → bữa → món. */
    public static List<SharedMenuDayResponse> of(List<DishByMealView> rows) {
        Map<LocalDate, Map<Long, List<DishByMealView>>> byDay = new LinkedHashMap<>();
        for (DishByMealView row : rows) {
            byDay.computeIfAbsent(row.date(), d -> new LinkedHashMap<>())
                    .computeIfAbsent(row.mealId(), m -> new ArrayList<>())
                    .add(row);
        }
        List<SharedMenuDayResponse> out = new ArrayList<>();
        byDay.forEach((date, meals) -> out.add(new SharedMenuDayResponse(
                date,
                meals.values().stream()
                        .map(list -> new Meal(
                                list.getFirst().mealId(),
                                list.getFirst().mealName(),
                                list.getFirst().expectedTime(),
                                list.stream()
                                        .map(d -> new Dish(d.dishId(), d.dishName(), d.description(), d.imageUrl()))
                                        .toList()))
                        .toList())));
        return out;
    }
}
