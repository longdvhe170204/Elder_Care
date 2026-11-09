package com.eldercare.backend.nutrition.internal.catalog.persistence;

import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.nutrition.internal.catalog.domain.Diet;
import com.eldercare.backend.nutrition.internal.catalog.domain.DietAttributes;
import com.eldercare.backend.nutrition.internal.catalog.domain.Dish;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishAttributes;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishVersion;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishVersionContent;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishVersionStatus;
import com.eldercare.backend.nutrition.internal.catalog.domain.Meal;
import com.eldercare.backend.nutrition.internal.catalog.domain.MealAttributes;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Chế độ ăn, món ăn ({@code catalog_item}) và phiên bản nội dung món ({@code simple_record}) của M08; mọi truy cập bảng
 * chung qua API của chủ bảng (R-15, team-plan mục 3 điều 6). Lệnh ghi chạy trong transaction của service.
 */
@Repository
public class NutritionCatalogStore {

    private static final String DISH_ENTITY = "M08.DISH";

    private final CatalogApi catalogs;
    private final SimpleRecordApi records;

    public NutritionCatalogStore(CatalogApi catalogs, SimpleRecordApi records) {
        this.catalogs = catalogs;
        this.records = records;
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

    /** Thành phần của phiên bản nội dung đang hiệu lực của món (rỗng khi món chưa từng Hiệu lực). */
    public java.util.Set<Long> effectiveIngredients(long dishId) {
        Long version = dish(dishId).attributes().effectiveVersionId();
        return version == null
                ? java.util.Set.of()
                : java.util.Set.copyOf(version(version).content().ingredients());
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

    public DishVersion createVersion(
            long dishId, DishVersionStatus status, DishVersionContent content, long createdBy) {
        return version(records.create(
                DishVersionContent.KIND,
                new NewSimpleRecord<>(
                        null,
                        new EntityRef(DISH_ENTITY, dishId),
                        status.name(),
                        content,
                        null,
                        null,
                        openKey(dishId, status),
                        createdBy,
                        false)));
    }

    public DishVersion version(long id) {
        return version(records.require(DishVersionContent.KIND, id));
    }

    /** Đổi phiên bản; phiên bản không còn mở thì nhả khóa duy nhất và khóa dòng (nhóm 3). */
    public DishVersion changeVersion(DishVersion version, DishVersionStatus status, DishVersionContent content) {
        boolean closed = !status.open();
        return version(records.change(
                        DishVersionContent.KIND,
                        version.id(),
                        version.version(),
                        new RecordChange<>(status.name(), content, null, openKey(version.dishId(), status), closed))
                .after());
    }

    /** Phiên bản đang soạn hoặc chờ xác nhận của món (tối đa một). */
    public Optional<DishVersion> openVersion(long dishId) {
        return records.listByParent(DishVersionContent.KIND, new EntityRef(DISH_ENTITY, dishId)).stream()
                .map(NutritionCatalogStore::version)
                .filter(v -> v.status().open())
                .findFirst();
    }

    private static String openKey(long dishId, DishVersionStatus status) {
        return status.open() ? "M08.DISH_VERSION.OPEN:" + dishId : null;
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

    private static DishVersion version(SimpleRecord<DishVersionContent> row) {
        return new DishVersion(
                row.id(),
                row.parent().entityId(),
                DishVersionStatus.valueOf(row.status()),
                row.content(),
                row.version());
    }
}
