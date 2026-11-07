package com.eldercare.backend.care.web.response;

import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.care.internal.activity.domain.Activity;
import java.util.Map;

/** 014 T004: hoạt động kèm trạng thái và phiên bản (If-Match). */
public record ActivityResponse(
        long id,
        String name,
        long typeId,
        String scope,
        Long floorAreaId,
        Activity.Location defaultLocation,
        Long defaultResponsibleStaffId,
        Integer durationMinutes,
        int maxParticipants,
        Map<String, Object> targetGroup,
        boolean charged,
        Long serviceId,
        boolean groupActivity,
        String groupActivityChangeReason,
        String status,
        int version) {
    public static ActivityResponse of(SimpleRecord<Activity> row) {
        var a = row.content();
        return new ActivityResponse(
                row.id(),
                a.name(),
                a.typeId(),
                a.scope(),
                a.floorAreaId(),
                a.defaultLocation(),
                a.defaultResponsibleStaffId(),
                a.durationMinutes(),
                a.maxParticipants(),
                a.targetGroup(),
                a.charged(),
                a.serviceId(),
                a.groupActivity(),
                a.groupActivityChangeReason(),
                row.status(),
                row.version());
    }
}
