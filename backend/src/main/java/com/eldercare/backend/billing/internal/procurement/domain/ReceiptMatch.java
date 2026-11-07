package com.eldercare.backend.billing.internal.procurement.domain;

import com.eldercare.backend.roombedallocation.api.dto.ProcurementType;

/**
 * 019 research W-05 (KhopChungTu; FR-008b, BR-M11-19, Q-294): Kế toán so chứng từ với số tiền thực tế; với loại tài sản số
 * thực tế còn phải bằng tổng nguyên giá các tài sản gắn. Loại khác (nguyên liệu theo Q-392, dịch vụ, thuốc, khác) không có
 * tổng nguồn để so.
 */
public final class ReceiptMatch {
    private ReceiptMatch() {}

    public static boolean matches(ProcurementType type, long actualAmount, long linkedSourceTotal) {
        return type != ProcurementType.ASSET || actualAmount == linkedSourceTotal;
    }
}
