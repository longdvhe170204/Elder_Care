package com.eldercare.backend.resident.internal.health.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.PermissionDeniedException;
import java.util.Map;

/**
 * Hành chính ghi tự khai của gia đình về sức khỏe (001 T102; Q-400, BR-M01-17; Phụ lục 27 chú thích ⁵⁰). Ma trận quyền cho
 * Hành chính cả ba thao tác của dòng "Dị ứng, bệnh nền" (ô T); phần hẹp hơn kiểm ở dịch vụ: người dùng chỉ có vai trò Hành
 * chính (không đồng thời Bác sĩ hay Điều dưỡng) chỉ lập bản Nháp và thêm mục mang dấu chưa kiểm chứng, chỉ xem phần mình
 * ghi, không loại trừ, không xác nhận, không kiểm chứng.
 */
public final class SelfReportedHealth {

    private SelfReportedHealth() {}

    /** Chỉ có quyền của Hành chính, không phải Bác sĩ hay Điều dưỡng. */
    public static boolean isOfficeOnly(CurrentUser user) {
        return user.roles().contains(Role.OFFICE_STAFF)
                && !user.roles().contains(Role.DOCTOR)
                && !user.roles().contains(Role.NURSING_STAFF);
    }

    public static PermissionDeniedException deniedForOffice(String message) {
        return new PermissionDeniedException("PERMISSION_DENIED", message, Map.of("failedLayer", "ROLE"));
    }
}
