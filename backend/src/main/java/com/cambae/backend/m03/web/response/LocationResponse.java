package com.cambae.backend.m03.web.response;

import com.cambae.backend.m03.api.dto.LocationView;

/** Nhãn vị trí đầy đủ theo FR-003. */
public record LocationResponse(
        String area, String floor, String room, String bed, String label, long bedId, Long roomId, Long floorId) {
    public static LocationResponse of(LocationView v) {
        return new LocationResponse(
                v.areaName(),
                v.floorName(),
                v.roomName(),
                v.bedCode(),
                String.join(" – ", v.areaName(), v.floorName(), v.roomName(), v.bedCode()),
                v.bedId(),
                v.roomId(),
                v.floorId());
    }
}
