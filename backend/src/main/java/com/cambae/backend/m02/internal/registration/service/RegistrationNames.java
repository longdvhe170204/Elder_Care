package com.cambae.backend.m02.internal.registration.service;

import com.cambae.backend.m15.account.api.dto.Target;

/**
 * Tên loại đối tượng của khai báo (glossary 24.3): loại đính chính, nhật ký, lưu giữ, phạm vi
 * {@code M02.RESIDENCE_REGISTRATION}; loại chủ sở hữu tệp bằng chứng {@code M02.REGISTRATION_EVIDENCE}.
 */
public final class RegistrationNames {

    public static final String ENTITY_TYPE = "M02.RESIDENCE_REGISTRATION";
    public static final String EVIDENCE_OWNER_TYPE = "M02.REGISTRATION_EVIDENCE";

    private RegistrationNames() {}

    /** Đối tượng kiểm quyền: khai báo của người cao tuổi, không gắn tầng (research Z-04). */
    public static Target target(long registrationId, long residentId) {
        return new Target(ENTITY_TYPE, String.valueOf(registrationId), residentId, null);
    }
}
