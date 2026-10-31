package com.eldercare.backend.care.internal.careplan.domain;

import com.eldercare.backend.common.data.DataGroup;

/** Mục TASK thuộc đúng một phiên bản; sourceItemId là gốc chuỗi sao chép để so sánh. */
@DataGroup(DataGroup.Group.BUSINESS)
public record CarePlanItem(
        long id, long carePlanId, Long sourceItemId, long physicalTaskTypeId, CarePlanItemData data) {
    public long lineage() {
        return sourceItemId == null ? id : sourceItemId;
    }
}
