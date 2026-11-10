package com.eldercare.backend.care.api;

import com.eldercare.backend.care.api.dto.ResidentOngoingTripView;
import java.util.Optional;

/** FR-068a: 007 đọc chuyến đã rời viện, chưa điểm danh về để xử lý SOS. */
public interface ResidentTripReadApi {
    Optional<ResidentOngoingTripView> ongoing(long residentId);
}
