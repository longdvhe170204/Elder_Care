package com.eldercare.backend.admission.internal.registration.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;

/**
 * Cờ thường trú cùng xã/phường của hồ sơ ở {@code simple_record} (loại {@code M02.REGISTRATION_RESIDENT}; data-model 018 mục
 * 0.1; R-15): các cột của {@code khai_bao_nct} cũ; mỗi người một dòng, tạo khi xác nhận lần đầu. Khóa theo người là
 * {@code technical_lock}, không phải dòng này.
 */
public record ResidentRegistrationFlag(
        boolean localPermanentResident, String basisNote, Long confirmedById, Instant confirmedAt) {

    public static final SharedKind<ResidentRegistrationFlag> KIND =
            SharedKind.of("M02.REGISTRATION_RESIDENT", ResidentRegistrationFlag.class);
}
