package com.eldercare.backend.family.internal.pickup.domain;

import com.eldercare.backend.administration.account.api.dto.Function;
import java.util.Set;

/**
 * Loại đón ({@code ban_ghi_don.loai}; FR-021, FR-026, Q-295) và lệnh nguồn mà lượt đón phục vụ (FR-022): Cho tạm vắng
 * (004, gồm Đi chơi với gia đình), Điểm danh về của bán trú (005, gồm Tự về, Viện đưa về), Kết thúc lưu trú (004).
 */
public enum PickupType {
    TEMPORARY_ABSENCE(Function.ABSENCE_RETURN, null),
    FAMILY_OUTING(Function.ABSENCE_RETURN, null),
    DAY_CARE_DEPARTURE(Function.DAY_CARE_ATTENDANCE, "PICKUP"),
    SELF_RETURN(Function.DAY_CARE_ATTENDANCE, "SELF_RETURN"),
    INSTITUTE_RETURN(Function.DAY_CARE_ATTENDANCE, "INSTITUTE_RETURN"),
    RESIDENCY_END(Function.RESIDENCY_END_DEATH, null);

    /** Chức năng của lệnh nguồn; người thực hiện quy trình đón phải có quyền Tạo của chức năng này (FR-022). */
    public static final Set<Function> SOURCE_FUNCTIONS =
            Set.of(Function.ABSENCE_RETURN, Function.DAY_CARE_ATTENDANCE, Function.RESIDENCY_END_DEATH);

    private final Function sourceFunction;
    private final String careCheckoutType;

    PickupType(Function sourceFunction, String careCheckoutType) {
        this.sourceFunction = sourceFunction;
        this.careCheckoutType = careCheckoutType;
    }

    public Function sourceFunction() {
        return sourceFunction;
    }

    /** Lệnh nguồn ở 005 (Điểm danh về). */
    public boolean dayCare() {
        return careCheckoutType != null;
    }

    /** Tên loại bàn giao của 005 ({@code CheckoutType}; glossary: QUA_DON, TU_VE, VIEN_DUA_VE). */
    public String careCheckoutType() {
        return careCheckoutType;
    }
}
