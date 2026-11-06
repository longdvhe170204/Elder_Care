package com.eldercare.backend.family.internal.pickup.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;

/**
 * Lần chặn đón ({@code lan_chan_don}, nhóm 3; R-15: {@code record_event} loại {@link #KIND} gắn hồ sơ người cao tuổi;
 * FR-023): người đón được khai, lý do; người thực hiện, thời điểm ở cột của dòng.
 */
public record PickupBlock(
        Reason reason, PickupType type, Long familyMemberId, Long exceptionId, String presentedIdDocumentNumber) {

    public static final SharedKind<PickupBlock> KIND = SharedKind.of("M10.PICKUP_BLOCK", PickupBlock.class);

    /** Glossary: NGOAI_DANH_SACH, GIAY_TO_KHONG_KHOP, NGOAI_LE_HET_HAN, DAU_TU_VE_TAT. */
    public enum Reason {
        NOT_ON_LIST,
        ID_DOCUMENT_MISMATCH,
        EXCEPTION_EXPIRED,
        SELF_RETURN_OFF
    }
}
