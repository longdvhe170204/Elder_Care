package com.eldercare.backend.nutrition.internal.catalog.persistence;

import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.nutrition.internal.catalog.domain.Diet;
import com.eldercare.backend.nutrition.internal.catalog.domain.DietAttributes;
import com.eldercare.backend.nutrition.internal.catalog.domain.Dish;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishAttributes;
import com.eldercare.backend.nutrition.internal.catalog.domain.Meal;
import com.eldercare.backend.nutrition.internal.catalog.domain.MealAttributes;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * Chế độ ăn, món ăn ({@code catalog_item}) và phiên bản nội dung món ({@code simple_record}) của M08; mọi truy cập bảng
 * chung qua API của chủ bảng (R-15, team-plan mục 3 điều 6). Lệnh ghi chạy trong transaction của service.
 */
@Repository
public class NutritionCatalogStore {

    private final CatalogApi catalogs;

    public NutritionCatalogStore(CatalogApi catalogs) {
        this.catalogs = catalogs;
    }

    public List<Diet> diets() {
        return catalogs.list(DietAttributes.KIND, null, false).stream()
                .map(NutritionCatalogStore::diet)
                .toList();
    }

    public Diet diet(long id) {
        return diet(catalogs.require(DietAttributes.KIND, id));
    }

    public Diet createDiet(String name, DietAttributes attributes) {
        return diet(catalogs.create(DietAttributes.KIND, new NewCatalogItem<>(null, name, null, 0, attributes)));
    }

    public Diet updateDiet(Diet diet, String name, DietAttributes attributes) {
        return diet(catalogs.update(DietAttributes.KIND, diet.id(), diet.version(), name, 0, attributes)
                .after());
    }

    public Diet deactivateDiet(Diet diet) {
        return diet(catalogs.deactivate(DietAttributes.KIND, diet.id(), diet.version())
                .after());
    }

    /** Bữa Hiệu lực theo thứ tự trong ngày (FR-006). */
    public List<Meal> activeMeals() {
        return catalogs.list(MealAttributes.KIND, null, true).stream()
                .map(NutritionCatalogStore::meal)
                .sorted(java.util.Comparator.comparingInt(Meal::sortOrder).thenComparingLong(Meal::id))
                .toList();
    }

    public Meal meal(long id) {
        return meal(catalogs.require(MealAttributes.KIND, id));
    }

    /** Mọi bữa của danh mục (cả Ngừng hiệu lực), theo thứ tự trong ngày (FR-006). */
    public List<CatalogItem<MealAttributes>> mealItems() {
        return catalogs.list(MealAttributes.KIND, null, false);
    }

    public CatalogItem<MealAttributes> mealItem(long id) {
        return catalogs.require(MealAttributes.KIND, id);
    }

    public CatalogItem<MealAttributes> createMeal(String code, String name, int sortOrder, MealAttributes attributes) {
        return catalogs.create(MealAttributes.KIND, new NewCatalogItem<>(code, name, null, sortOrder, attributes));
    }

    public CatalogItem<MealAttributes> updateMeal(
            CatalogItem<MealAttributes> meal, String name, int sortOrder, MealAttributes attributes) {
        return catalogs.update(MealAttributes.KIND, meal.id(), meal.version(), name, sortOrder, attributes)
                .after();
    }

    public CatalogItem<MealAttributes> deactivateMeal(CatalogItem<MealAttributes> meal) {
        return catalogs.deactivate(MealAttributes.KIND, meal.id(), meal.version())
                .after();
    }

    public CatalogItem<MealAttributes> reactivateMeal(CatalogItem<MealAttributes> meal) {
        return catalogs.reactivate(MealAttributes.KIND, meal.id(), meal.version())
                .after();
    }

    /** Mã bữa trong danh mục (khóa của {@code mealTimes} ở CFG-M08-04). */
    public String mealCode(long id) {
        return catalogs.require(MealAttributes.KIND, id).code();
    }

    public List<Dish> dishes() {
        return catalogs.list(DishAttributes.KIND, null, false).stream()
                .map(NutritionCatalogStore::dish)
                .toList();
    }

    public Dish dish(long id) {
        return dish(catalogs.require(DishAttributes.KIND, id));
    }

    public Dish createDish(String name, DishAttributes attributes) {
        return dish(catalogs.create(DishAttributes.KIND, new NewCatalogItem<>(null, name, null, 0, attributes)));
    }

    public Dish updateDish(Dish dish, String name, DishAttributes attributes) {
        return dish(catalogs.update(DishAttributes.KIND, dish.id(), dish.version(), name, 0, attributes)
                .after());
    }

    /** Ghi xác nhận chuyên môn của Bác sĩ lên mục danh mục ({@code confirmed_by}, {@code confirmed_at}). */
    public Dish confirmDish(Dish dish, long confirmedBy) {
        return dish(catalogs.confirm(DishAttributes.KIND, dish.id(), dish.version(), confirmedBy)
                .after());
    }

    /** Món Ngừng hiệu lực hoặc Đã hủy: cột {@code status} của danh mục thành {@code INACTIVE} (trạng thái cuối). */
    public Dish closeDish(Dish dish) {
        return dish(catalogs.deactivate(DishAttributes.KIND, dish.id(), dish.version())
                .after());
    }

    private static Diet diet(CatalogItem<DietAttributes> item) {
        return new Diet(item.id(), item.name(), item.status(), item.attributes(), item.version());
    }

    private static Meal meal(CatalogItem<MealAttributes> item) {
        return new Meal(item.id(), item.name(), item.status(), item.sortOrder(), item.attributes());
    }

    private static Dish dish(CatalogItem<DishAttributes> item) {
        return new Dish(item.id(), item.name(), item.status(), item.attributes(), item.version());
    }
}
