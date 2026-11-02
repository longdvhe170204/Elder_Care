package com.eldercare.backend.archfixture.m01guard;

import com.eldercare.backend.common.data.BusinessCommand;

/** Lớp mẫu cho {@code ProfileOpenGuardArchTest}: lệnh ghi hồ sơ mà không gọi guard, phải bị bắt. */
public class UnguardedService {

    @BusinessCommand(requiresReason = false)
    public void write(long residentId) {}
}
