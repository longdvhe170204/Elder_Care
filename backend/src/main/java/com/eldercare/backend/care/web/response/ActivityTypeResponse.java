package com.eldercare.backend.care.web.response;

import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.care.internal.activity.domain.ActivityTypeAttributes;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Schema duyệt 08/11/2026. */
public record ActivityTypeResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean groupActivity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean offSite,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Long> interestGroupIds) {
    public static ActivityTypeResponse of(CatalogItem<ActivityTypeAttributes> item) {
        return new ActivityTypeResponse(
                item.id(),
                item.name(),
                item.status().name(),
                item.attributes().groupActivity(),
                item.attributes().offSite(),
                item.attributes().interestGroupIds());
    }
}
