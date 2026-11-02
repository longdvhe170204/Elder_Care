package com.eldercare.backend.notification.api.spi;

import java.util.List;
import java.util.Optional;

/**
 * Người thân nhận thông báo ({@code NguoiThanNhanThongBaoPort}; 012 cài, chỉ đọc). Bean thiếu thì nhóm quan hệ người
 * thân rỗng và người thân không có số: thông báo vào danh sách "cần liên hệ trực tiếp" (contracts mục 2.1).
 */
public interface FamilyNotificationPort {

    /** Người thân có quan hệ còn hiệu lực với người cao tuổi; người liên hệ chính luôn có quyền nhận khẩn (Q-96). */
    List<NotifiableRelationshipView> activeRelationships(long residentId);

    /** Thứ tự gọi (FR-029a): người đại diện, rồi người có quyền nhận khẩn, theo thứ tự 012 (012 FR-012). */
    List<Long> callOrder(long residentId);

    /** Số hiện hành và tài khoản cổng của một người thân, kể cả khi không có quan hệ (nhóm (d)). */
    Optional<FamilyContactView> contact(long familyMemberId);
}
