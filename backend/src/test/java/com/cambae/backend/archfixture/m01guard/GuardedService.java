package com.cambae.backend.archfixture.m01guard;

import com.cambae.backend.common.data.BusinessCommand;
import com.cambae.backend.m01.internal.status.service.ProfileOpenGuard;

/** Lớp mẫu cho {@code ProfileOpenGuardArchTest}: lệnh gọi guard, hợp lệ. */
public class GuardedService {

    private final ProfileOpenGuard guard;

    public GuardedService(ProfileOpenGuard guard) {
        this.guard = guard;
    }

    @BusinessCommand(requiresReason = false)
    public void write(long residentId) {
        guard.requireOpen(residentId);
    }
}
