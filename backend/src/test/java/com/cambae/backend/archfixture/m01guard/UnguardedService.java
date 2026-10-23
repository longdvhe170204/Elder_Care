package com.cambae.backend.archfixture.m01guard;

import com.cambae.backend.common.data.BusinessCommand;

/** Lớp mẫu cho {@code ProfileOpenGuardArchTest}: lệnh ghi hồ sơ mà không gọi guard, phải bị bắt. */
public class UnguardedService {

    @BusinessCommand(requiresReason = false)
    public void write(long residentId) {}
}
