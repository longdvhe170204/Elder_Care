package com.eldercare.backend.roombedallocation.web.response;

import com.eldercare.backend.roombedallocation.internal.daycare.domain.DayCareAreaState;
import com.fasterxml.jackson.annotation.JsonInclude;

/** Không lộ entity; sức chứa là CFG hiện hành. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DayCareRestAreaResponse(
        long id, String code, String name, Long floorId, Long areaId, Integer capacityPerSession, String status) {
    public static DayCareRestAreaResponse of(DayCareAreaState value, Integer capacity) {
        return new DayCareRestAreaResponse(
                value.id(), value.code(), value.name(), value.floorId(), value.areaId(), capacity, value.status());
    }
}
