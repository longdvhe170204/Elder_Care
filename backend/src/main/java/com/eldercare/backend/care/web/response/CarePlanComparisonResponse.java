package com.eldercare.backend.care.web.response;

import com.eldercare.backend.care.internal.careplan.domain.CarePlanDifference;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "CarePlanComparison")
public record CarePlanComparisonResponse(
        List<CarePlanItemResponse> added,
        List<CarePlanItemResponse> removed,
        List<CarePlanItemChangeResponse> changed) {
    public static CarePlanComparisonResponse of(CarePlanDifference diff) {
        return new CarePlanComparisonResponse(
                diff.added().stream().map(CarePlanItemResponse::of).toList(),
                diff.removed().stream().map(CarePlanItemResponse::of).toList(),
                diff.changed().stream()
                        .map(change -> new CarePlanItemChangeResponse(
                                CarePlanItemResponse.of(change.before()),
                                CarePlanItemResponse.of(change.after()),
                                change.changedFields()))
                        .toList());
    }
}
