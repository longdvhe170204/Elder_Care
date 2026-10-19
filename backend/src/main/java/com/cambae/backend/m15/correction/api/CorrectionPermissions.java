package com.cambae.backend.m15.correction.api;

import com.cambae.backend.m15.account.api.PermissionKey;

/**
 * Khóa quyền của yêu cầu đính chính cách C ({@code M15.CORRECTION}; FR-026 c, nghiệp vụ 1.5). Ai được lập do bảng chọn
 * cách tạo quyết định (cùng vai trò với người ghi gốc, có quyền ghi nhận); {@code REQUEST} chỉ là khóa để 002 cấp cho
 * nhân viên có tài khoản. {@code APPROVE} cấp cho Quản lý viện; handler còn giới hạn vai trò duyệt là Quản lý viện.
 */
public final class CorrectionPermissions {

    public static final PermissionKey REQUEST = new PermissionKey("CORRECTION", "REQUEST");
    public static final PermissionKey APPROVE = new PermissionKey("CORRECTION", "APPROVE");

    private CorrectionPermissions() {}
}
