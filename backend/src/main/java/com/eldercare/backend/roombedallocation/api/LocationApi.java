package com.eldercare.backend.roombedallocation.api;

import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import java.time.Instant;
import java.util.Optional;

/** ViTriApi.taiThoiDiem; nguồn vị trí tại thời điểm, không suy từ phân công nhân viên. */
public interface LocationApi {
    Optional<LocationView> at(long residentId, Instant at);
}
