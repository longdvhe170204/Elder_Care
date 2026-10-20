package com.cambae.backend.m15.correction.api;

import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.PermissionKey;

/**
 * Khóa quyền của yêu cầu đính chính cách C ({@code M15.CORRECTION}; FR-026 c, nghiệp vụ 1.5) theo Phụ lục 27, dòng
 * "Đính chính bản ghi không gắn tầng": T¹³ lập yêu cầu, Quản lý viện D. Ai được lập do bảng chọn
 * cách tạo quyết định (cùng vai trò với người ghi gốc, có quyền ghi nhận); {@code REQUEST} chỉ là khóa để 002 cấp cho
 * nhân viên có tài khoản. {@code APPROVE} cấp cho Quản lý viện; handler còn giới hạn vai trò duyệt là Quản lý viện.
 */
public final class CorrectionPermissions {

    public static final PermissionKey REQUEST =
            new PermissionKey(Function.UNASSIGNED_RECORD_CORRECTION, Operation.CREATE);
    public static final PermissionKey APPROVE =
            new PermissionKey(Function.UNASSIGNED_RECORD_CORRECTION, Operation.APPROVE);

    private CorrectionPermissions() {}
}
