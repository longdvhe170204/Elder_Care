package com.cambae.backend.m15.account.api;

import com.cambae.backend.m15.account.api.dto.ResidentDevice;
import java.util.Optional;

/**
 * Xác định người cao tuổi từ token thiết bị cho nút SOS (002 contracts module-interfaces 1.11 {@code ThietBiGanApi};
 * FR-071, research A-15; 007 FR-047d). Không cần phiên. Chỉ endpoint nút SOS của 007 gọi ({@code BoundDeviceApiArchTest}).
 */
public interface BoundDeviceApi {

    /** Rỗng khi token sai, thiết bị đã gỡ hoặc tài khoản Đã khóa; Khóa tạm vẫn xác định được. */
    Optional<ResidentDevice> identify(String deviceToken);
}
