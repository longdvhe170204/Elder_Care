package com.cambae.backend.m03.api;

import com.cambae.backend.m03.api.dto.LocationView;
import java.time.Instant;
import java.util.Optional;

/** ViTriApi.taiThoiDiem; nguồn vị trí tại thời điểm, không suy từ phân công nhân viên. */
public interface LocationApi {
    Optional<LocationView> at(long residentId, Instant at);
}
