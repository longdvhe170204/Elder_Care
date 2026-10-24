package com.cambae.backend.m15.account.api.spi;

import java.util.Optional;

/**
 * SPI: hồ sơ người cao tuổi cho tài khoản người cao tuổi (tên logic phần {@code NguoiCaoTuoiApi} 002 dùng; FR-067,
 * FR-073, T032a). Khai báo ở 002 để tránh vòng M15 ↔ M01 (M01 đã dùng {@code m15.api}); M01 (001) cài.
 */
public interface ResidentProfileApi {

    /** Rỗng khi không có hồ sơ. */
    Optional<ResidentProfileView> profile(long residentId);
}
