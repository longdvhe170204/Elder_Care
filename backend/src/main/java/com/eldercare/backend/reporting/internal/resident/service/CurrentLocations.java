package com.eldercare.backend.reporting.internal.resident.service;

import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import java.util.Collection;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Tầng, phòng hiện tại của người cao tuổi cho chỉ tiêu "lúc xem" mà nguồn chỉ trả mã người cao tuổi (dấu nguy kịch, đánh
 * giá lại quá hạn): phạm vi tầng của người xem lọc theo nơi ở hiện tại (FR-011, FR-013).
 */
public final class CurrentLocations {

    private CurrentLocations() {}

    public static Map<Long, LocationView> of(
            ObjectProvider<ResidentLocationApi> locations, Collection<Long> residents) {
        ResidentLocationApi api = locations.getIfAvailable();
        return api == null || residents.isEmpty() ? Map.of() : api.current(residents);
    }
}
