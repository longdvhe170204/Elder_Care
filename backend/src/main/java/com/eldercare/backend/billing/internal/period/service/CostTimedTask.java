package com.eldercare.backend.billing.internal.period.service;

import com.eldercare.backend.billing.internal.period.domain.CostJobCadence;
import java.time.Instant;

/** Tác vụ do các story đăng ký vào khung T007; không có tác vụ giả trong production. */
public interface CostTimedTask {
    String name();

    CostJobCadence cadence();

    int run(Instant slot);

    default boolean latestOnly() {
        return false;
    }
}
