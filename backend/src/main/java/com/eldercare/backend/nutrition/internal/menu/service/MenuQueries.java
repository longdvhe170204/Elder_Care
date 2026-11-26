package com.eldercare.backend.nutrition.internal.menu.service;

import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.nutrition.api.ExpectedMealApi;
import com.eldercare.backend.nutrition.api.GeneralMenuApi;
import com.eldercare.backend.nutrition.api.dto.DishByMealView;
import com.eldercare.backend.nutrition.api.dto.ExpectedPortionView;
import com.eldercare.backend.nutrition.internal.catalog.domain.Dish;
import com.eldercare.backend.nutrition.internal.catalog.persistence.NutritionCatalogStore;
import com.eldercare.backend.nutrition.internal.menu.domain.MenuDish;
import com.eldercare.backend.nutrition.internal.menu.persistence.MenuStore;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đọc thực đơn chung đã Công bố, Đã áp dụng (FR-028, Q-445) cho app (nhân viên qua {@code GET /menus/shared}), cổng người
 * thân và kênh người cao tuổi (012): mọi món theo ngày, bữa, theo thứ tự; không chốt suất, không ghi dữ liệu.
 */
@Service
public class MenuQueries implements GeneralMenuApi, ExpectedMealApi {

    private final MenuStore menus;
    private final NutritionCatalogStore catalog;
    private final MealTimes mealTimes;
    private final BusinessTime time;

    MenuQueries(MenuStore menus, NutritionCatalogStore catalog, MealTimes mealTimes, BusinessTime time) {
        this.menus = menus;
        this.catalog = catalog;
        this.mealTimes = mealTimes;
        this.time = time;
    }

    /**
     * Thực đơn chung cho người dùng app có quyền xem thực đơn ({@code GET /menus/shared}): mặc định hôm nay; khoảng tối đa 31
     * ngày.
     */
    @RequiresPermission(function = Function.DISH_MENU, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<DishByMealView> shared(LocalDate from, LocalDate to) {
        LocalDate start = from != null ? from : to != null ? to : time.today();
        LocalDate end = to != null ? to : start;
        if (end.isBefore(start) || end.isAfter(start.plusDays(31))) {
            throw new InvalidValueException("Khoảng ngày không hợp lệ (tối đa 31 ngày)");
        }
        return generalMenu(start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DishByMealView> generalMenu(LocalDate from, LocalDate to) {
        Map<Long, Dish> dishes = catalog.dishes().stream().collect(Collectors.toMap(Dish::id, d -> d));
        Map<Long, Integer> mealOrder =
                catalog.mealItems().stream().collect(Collectors.toMap(m -> m.id(), m -> m.sortOrder()));
        return menus.publishedDishes(from, to).stream()
                .sorted(Comparator.comparing(MenuDish::date)
                        .thenComparing(d -> mealOrder.getOrDefault(d.mealId(), Integer.MAX_VALUE)))
                .map(d -> view(d, dishes.get(d.dishId())))
                .toList();
    }

    /** Kênh người cao tuổi: thực đơn chung của tuần (Q-445 bỏ suất ăn dự kiến theo người, không có món thay thế). */
    @Override
    @Transactional(readOnly = true)
    public List<ExpectedPortionView> ofResident(long residentId, LocalDate from, LocalDate to) {
        return generalMenu(from, to).stream()
                .map(d -> new ExpectedPortionView(d.date(), d.mealId(), d.mealName(), d.dishName(), false))
                .toList();
    }

    private DishByMealView view(MenuDish row, Dish dish) {
        String time = mealTimes.expected(row.mealId()).map(Object::toString).orElse(null);
        return new DishByMealView(
                row.date(),
                row.mealId(),
                catalog.meal(row.mealId()).name(),
                time,
                row.dishId(),
                dish == null ? null : dish.name(),
                dish == null ? null : dish.attributes().description(),
                dish == null || dish.attributes().imageFileId() == null
                        ? null
                        : "/api/v1/files/" + dish.attributes().imageFileId());
    }
}
