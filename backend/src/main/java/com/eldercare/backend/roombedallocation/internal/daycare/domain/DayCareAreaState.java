package com.eldercare.backend.roombedallocation.internal.daycare.domain;

import com.eldercare.backend.roombedallocation.api.dto.LocationRef;

/** Ảnh đọc hiện hành của danh mục khu nghỉ. */
public record DayCareAreaState(
        long id, String code, String name, Long floorId, Long areaId, String status, int version) {
    public LocationRef location() {
        return floorId != null ? new LocationRef("FLOOR", floorId) : new LocationRef("AREA", areaId);
    }
}
