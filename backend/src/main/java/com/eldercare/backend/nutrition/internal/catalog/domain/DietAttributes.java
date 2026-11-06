package com.eldercare.backend.nutrition.internal.catalog.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.util.List;

/**
 * Thuộc tính của chế độ ăn ở {@code catalog_item} loại {@code M08.DIET} (data-model mục 0.1, 3; R-15). Hạn chế là id dị
 * nguyên (thay bảng {@code che_do_an_han_che}); đúng một mặc định Hiệu lực giữ bằng cột sinh {@code m08_default_diet}
 * (V011_94). Không xóa được vì bản gán, thực đơn tham chiếu qua JSON.
 */
public record DietAttributes(
        String description, boolean treatmentRelated, boolean isDefault, List<Long> restrictions, Long safeDishId) {

    public static final SharedKind<DietAttributes> KIND =
            SharedKind.of("M08.DIET", DietAttributes.class).notDeletable();

    public DietAttributes {
        restrictions = restrictions == null ? List.of() : List.copyOf(restrictions);
    }

    public DietAttributes withDefault(boolean value) {
        return new DietAttributes(description, treatmentRelated, value, restrictions, safeDishId);
    }
}
