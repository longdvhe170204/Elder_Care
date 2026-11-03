package com.eldercare.backend.care.internal.careplan.domain;

import com.eldercare.backend.care.api.dto.CarePlanItemView;
import java.util.List;

/** Chỉ các trường nội dung thay đổi;id bản sao không phải thay đổi nghiệp vụ. */
public record CarePlanItemChange(CarePlanItemView before, CarePlanItemView after, List<String> changedFields) {
    public CarePlanItemChange {
        changedFields = List.copyOf(changedFields);
    }
}
