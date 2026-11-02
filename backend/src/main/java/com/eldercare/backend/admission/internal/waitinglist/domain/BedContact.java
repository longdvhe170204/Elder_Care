package com.eldercare.backend.admission.internal.waitinglist.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;

/**
 * Lần liên hệ, từ chối, hết hạn giữ chỗ với một giường (nhóm 3, loại {@code M02.BED_CONTACT} của {@code record_event}; data-model
 * mục 0, 4; FR-012, FR-014, BR-M02-10). Người thực hiện rỗng khi hệ thống ghi hết hạn.
 */
public record BedContact(long bedId, BedContactResult result, String reason, String outOfOrderReason) {

    public static final SharedKind<BedContact> KIND = SharedKind.of("M02.BED_CONTACT", BedContact.class);
}
