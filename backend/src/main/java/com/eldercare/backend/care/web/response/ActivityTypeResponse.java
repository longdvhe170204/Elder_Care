package com.eldercare.backend.care.web.response;

import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.care.internal.activity.domain.ActivityTypeAttributes;
import java.util.List;

/** Schema duyệt 08/11/2026. */
public record ActivityTypeResponse(
        long id, String name, String status, boolean groupActivity, boolean offSite, List<Long> interestGroupIds) {
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
