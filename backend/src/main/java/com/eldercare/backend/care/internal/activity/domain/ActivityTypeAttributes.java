package com.eldercare.backend.care.internal.activity.domain;

import java.util.List;

/** 014 FR-001: loại hoạt động ({@code catalog_item}); nhóm sở thích liên quan giữ trong thuộc tính (R-15). */
public record ActivityTypeAttributes(boolean groupActivity, boolean offSite, List<Long> interestGroupIds) {
    public ActivityTypeAttributes {
        interestGroupIds = interestGroupIds == null ? List.of() : List.copyOf(interestGroupIds);
    }
}
