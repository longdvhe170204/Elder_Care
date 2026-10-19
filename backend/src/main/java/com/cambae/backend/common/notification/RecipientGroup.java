package com.cambae.backend.common.notification;

import com.cambae.backend.common.Role;
import java.util.List;

/**
 * Nhóm người nhận (009 research B-04). 000 khai báo các biến thể chính 000 dùng; 009 T004 bổ sung các biến thể còn lại
 * của bảng B-04 (nhiệm vụ, gắn bản ghi nguồn, quan hệ người thân, chính người cao tuổi) vào cùng interface.
 */
public sealed interface RecipientGroup {

    /** (b) Mọi tài khoản đang hoạt động có vai trò toàn viện. */
    record FacilityRole(Role role) implements RecipientGroup {}

    /**
     * (c) Người có quyền duyệt một loại yêu cầu: 000 tính sẵn danh sách tài khoản đã lọc theo giới hạn vai trò duyệt
     * (sửa theo plan 004, Q-338).
     */
    record Approvers(String requestType, List<Long> accountIds) implements RecipientGroup {}
}
