package com.cambae.backend.m15.account.api;

import com.cambae.backend.common.Role;
import com.cambae.backend.m15.account.api.dto.AccessChannel;
import com.cambae.backend.m15.account.api.dto.AccountType;
import java.util.Optional;
import java.util.Set;

/**
 * Người dùng đang thực hiện lệnh; Bộ lập lịch có bản riêng với {@code isScheduler() = true} (FR-044; 000 FR-045). 000
 * khai báo trước (000 T025); 002 thêm loại, liên kết hồ sơ, thiết bị, kênh (002 contracts/module-interfaces.md mục
 * 1.1; T008).
 */
public interface CurrentUser {

    long accountId();

    AccountType type();

    /** Có khi {@link #type()} là {@code STAFF}. */
    Optional<Long> staffId();

    /** Có khi {@link #type()} là {@code FAMILY_MEMBER}. */
    Optional<Long> familyMemberId();

    /** Có khi {@link #type()} là {@code RESIDENT} (002 research A-15). */
    Optional<Long> residentId();

    Set<Role> roles();

    boolean isScheduler();

    /** Mã thiết bị {@code X-Device-Id} của phiên. */
    String deviceId();

    AccessChannel channel();
}
