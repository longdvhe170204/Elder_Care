package com.eldercare.backend.incident.api;

import com.eldercare.backend.incident.api.dto.IsolationZoneView;
import java.time.Instant;
import java.util.List;

/** Nguồn khoanh vùng hiện hành; không cho module khác sửa trực tiếp (mục 1.8). */
public interface IsolationApi {
    List<IsolationZoneView> activeByRoom(long roomId, Instant at);

    List<IsolationZoneView> activeByFloorArea(long floorAreaId, Instant at);

    List<IsolationZoneView> activeByResident(long residentId, Instant at);

    List<IsolationZoneView> activeZones();
}
