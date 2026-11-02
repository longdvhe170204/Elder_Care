package com.eldercare.backend.archfixture.m01guard;

import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.resident.internal.status.service.ProfileOpenGuard;

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
