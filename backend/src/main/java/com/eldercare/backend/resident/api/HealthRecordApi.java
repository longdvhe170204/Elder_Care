package com.eldercare.backend.resident.api;

import com.eldercare.backend.resident.api.dto.InitialHealthRecordView;
import java.util.Optional;

/** Hồ sơ sức khỏe ban đầu cho 006 (thuốc đang dùng khi tiếp nhận). */
public interface HealthRecordApi {

    Optional<InitialHealthRecordView> initial(long residentId);
}
