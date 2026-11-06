package com.eldercare.backend.resident.internal.health.domain;

import com.eldercare.backend.resident.api.dto.AllergenGroup;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

/**
 * Nội dung JSON của dị nguyên ở {@code catalog_item} loại {@code M01.ALLERGEN} (001 T084; data-model 001 mục 0.1;
 * FR-018a): nhóm hoạt chất thuốc, thực phẩm, khác; {@code legacy} ghi bảng, id cũ của dòng chép từ {@code allergen}
 * ({@code V001_95}), mục tạo mới sau đó để {@code null}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AllergenAttributes(AllergenGroup allergenGroup, Map<String, Object> legacy) {

    public static AllergenAttributes of(AllergenGroup group) {
        return new AllergenAttributes(group, null);
    }
}
