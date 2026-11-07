package com.eldercare.backend.support.fake.nutrition;

import com.eldercare.backend.nutrition.api.GeneralMenuApi;
import com.eldercare.backend.nutrition.api.dto.DishByMealView;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Bản giả {@link GeneralMenuApi} cho 012: trả món do test đặt trong khoảng ngày. */
public class FakeGeneralMenuApi implements GeneralMenuApi {

    private final List<DishByMealView> dishes = new ArrayList<>();
    private final List<String> dietNames = new ArrayList<>();

    public FakeGeneralMenuApi dish(DishByMealView dish) {
        dishes.add(dish);
        return this;
    }

    public FakeGeneralMenuApi dietName(String name) {
        dietNames.add(name);
        return this;
    }

    @Override
    public List<DishByMealView> generalMenu(LocalDate from, LocalDate to) {
        return dishes.stream()
                .filter(d -> !d.date().isBefore(from) && !d.date().isAfter(to))
                .toList();
    }

    @Override
    public List<String> dietNames() {
        return List.copyOf(dietNames);
    }
}
