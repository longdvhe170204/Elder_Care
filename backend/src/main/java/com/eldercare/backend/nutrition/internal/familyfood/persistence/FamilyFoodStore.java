package com.eldercare.backend.nutrition.internal.familyfood.persistence;

import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.nutrition.api.dto.FamilyFoodStatus;
import com.eldercare.backend.nutrition.internal.familyfood.domain.FamilyFood;
import com.eldercare.backend.nutrition.internal.familyfood.domain.ForbiddenFoodType;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * Đồ ăn gia đình ({@code simple_record}) và danh mục loại đồ ăn bị cấm ({@code catalog_item}) ở bảng chung (R-15); truy
 * cập qua API của chủ bảng. Lệnh ghi chạy trong transaction của service.
 */
@Repository
public class FamilyFoodStore {

    private static final Set<String> ALL_STATUSES =
            Arrays.stream(FamilyFoodStatus.values()).map(Enum::name).collect(Collectors.toSet());

    private final SimpleRecordApi records;
    private final CatalogApi catalogs;

    FamilyFoodStore(SimpleRecordApi records, CatalogApi catalogs) {
        this.records = records;
        this.catalogs = catalogs;
    }

    public SimpleRecord<FamilyFood> create(long residentId, FamilyFoodStatus status, FamilyFood food, long by) {
        return records.create(
                FamilyFood.KIND,
                new NewSimpleRecord<>(residentId, null, status.name(), food, null, null, null, by, false));
    }

    public List<SimpleRecord<FamilyFood>> ofResident(long residentId) {
        return records.listByResident(FamilyFood.KIND, residentId, ALL_STATUSES);
    }

    public List<SimpleRecord<FamilyFood>> withStatus(FamilyFoodStatus status) {
        return records.listByStatus(FamilyFood.KIND, Set.of(status.name()));
    }

    public SimpleRecord<FamilyFood> require(long id) {
        return records.require(FamilyFood.KIND, id);
    }

    public SimpleRecord<FamilyFood> lock(long id) {
        return records.lockForUpdate(FamilyFood.KIND, id);
    }

    /** Đổi trạng thái, nội dung; {@code ENDED} là trạng thái cuối nên khóa dòng. */
    public void change(SimpleRecord<FamilyFood> row, FamilyFoodStatus status, FamilyFood content) {
        records.change(
                FamilyFood.KIND,
                row.id(),
                row.version(),
                new RecordChange<>(status.name(), content, null, null, status == FamilyFoodStatus.ENDED));
    }

    public List<CatalogItem<ForbiddenFoodType>> forbiddenTypes() {
        return catalogs.list(ForbiddenFoodType.KIND, null, false);
    }

    /** Tên các loại bị cấm đang Hiệu lực. */
    public Set<String> activeForbiddenNames() {
        return catalogs.list(ForbiddenFoodType.KIND, null, true).stream()
                .map(CatalogItem::name)
                .collect(Collectors.toSet());
    }

    public CatalogItem<ForbiddenFoodType> createForbidden(String name, ForbiddenFoodType attributes) {
        return catalogs.create(ForbiddenFoodType.KIND, new NewCatalogItem<>(null, name, null, 0, attributes));
    }

    public CatalogItem<ForbiddenFoodType> deactivateForbidden(CatalogItem<ForbiddenFoodType> item) {
        return catalogs.deactivate(ForbiddenFoodType.KIND, item.id(), item.version())
                .after();
    }
}
