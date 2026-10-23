package com.cambae.backend.m01.api;

import com.cambae.backend.m01.api.dto.InitialHealthRecordView;
import java.util.Optional;

/** Hồ sơ sức khỏe ban đầu cho 006 (thuốc đang dùng khi tiếp nhận). */
public interface HealthRecordApi {

    Optional<InitialHealthRecordView> initial(long residentId);
}
