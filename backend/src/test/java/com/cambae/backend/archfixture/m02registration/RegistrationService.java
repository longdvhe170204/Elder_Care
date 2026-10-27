package com.cambae.backend.archfixture.m02registration;

import com.cambae.backend.common.data.BusinessCommand;

/**
 * Lớp mẫu cho {@code ResidenceRegistrationArchTest}: lệnh lập khai báo, một lệnh khác của actor và việc chuyển trạng
 * thái theo giờ.
 */
public class RegistrationService {

    @BusinessCommand(requiresReason = false)
    public void create(long residentId) {}

    @BusinessCommand(requiresReason = false)
    public void recordAccepted(long registrationId) {}

    public void expire(long registrationId) {}
}
