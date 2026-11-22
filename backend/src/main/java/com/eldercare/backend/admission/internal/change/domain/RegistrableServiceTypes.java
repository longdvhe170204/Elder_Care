package com.eldercare.backend.admission.internal.change.domain;

import java.util.Map;
import java.util.Set;

/**
 * Loại dịch vụ thêm được vào hợp đồng qua loại thay đổi "Dịch vụ" (004 FR-043k, Q-426, R21): phục hồi chức năng, hoạt động
 * đăng ký dài hạn, đưa đi khám, tiêm theo y lệnh. Loại khác đi theo luồng riêng; mô tả lỗi chỉ đúng luồng cần dùng. Bớt
 * dòng đang có không xét loại.
 */
public final class RegistrableServiceTypes {

    public static final String CODE = "SERVICE_TYPE_NOT_REGISTRABLE";

    public static final Set<String> REGISTRABLE = Set.of("REHABILITATION", "ACTIVITY", "MEDICAL_ESCORT", "INJECTION");

    private static final Map<String, String> HINTS = Map.of(
            "CARE", "Đổi dịch vụ chăm sóc bằng loại thay đổi Mức chăm sóc",
            "RESIDENCY_FEE", "Đổi phí lưu trú bằng loại thay đổi Giường",
            "DAY_CARE_SESSION", "Buổi bán trú đổi bằng loại thay đổi Loại lưu trú hoặc Lịch đến",
            "MEAL", "Suất ăn đi theo luồng dinh dưỡng (chế độ ăn, suất ăn), không đăng ký trong hợp đồng",
            "FAMILY_STAY", "Người thân ở lại đăng ký ở chức năng Người thân ở lại, không đăng ký trong hợp đồng",
            "FACILITY_SUPPLIED_MEDICATION",
                    "Thuốc nguồn viện tính theo liều từ đơn thuốc, không đăng ký trong hợp đồng",
            "CONSUMABLE", "Tã, bỉm, sữa tính theo lần dùng hoặc lập đề nghị mua hộ, không đăng ký trong hợp đồng",
            "MEDICAL_SUPPLY", "Vật tư y tế tính theo lần dùng hoặc lập đề nghị mua hộ, không đăng ký trong hợp đồng");

    private static final String DEFAULT_HINT =
            "Loại thay đổi Dịch vụ chỉ thêm được phục hồi chức năng, hoạt động dài hạn, đưa đi khám, tiêm theo y lệnh";

    private RegistrableServiceTypes() {}

    public static boolean registrable(String type) {
        return REGISTRABLE.contains(type);
    }

    /** Mô tả tiếng Việt cho dòng thêm dịch vụ {@code serviceId} loại {@code type} không được phép. */
    public static String description(long serviceId, String type) {
        return HINTS.getOrDefault(type, DEFAULT_HINT) + " (dịch vụ mã " + serviceId + ")";
    }
}
