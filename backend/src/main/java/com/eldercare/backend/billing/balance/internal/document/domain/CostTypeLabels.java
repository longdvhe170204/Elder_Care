package com.eldercare.backend.billing.balance.internal.document.domain;

import com.eldercare.backend.billing.api.dto.CostType;

/** Tên loại khoản chi phí in trên chứng từ (phiếu báo phí, bảng quyết toán); cùng tên với bảng chi phí của 010. */
public final class CostTypeLabels {

    private CostTypeLabels() {}

    public static String of(CostType type) {
        return switch (type) {
            case RESIDENCY_FEE -> "Phí lưu trú";
            case DAY_CARE_SESSION_FEE -> "Phí buổi bán trú";
            case SERVICE -> "Dịch vụ";
            case CONSUMABLE -> "Vật phẩm tiêu hao";
            case ACTIVITY -> "Hoạt động";
            case MEDICATION -> "Thuốc";
            case FAMILY_OVERNIGHT_STAY -> "Người thân ở lại";
            case FAMILY_MEAL -> "Suất ăn người thân";
            case PURCHASE_ASSISTANCE -> "Mua hộ";
            case OTHER -> "Khoản khác";
        };
    }
}
