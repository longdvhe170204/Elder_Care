package com.eldercare.backend.notification.api;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lệnh lượt nhắc gọi cho module khác gọi trong transaction lệnh của mình (009 T019; chủ dự án chốt 2026-11-12). 012 gọi
 * khi quan hệ hoặc quyền nhận thông báo khẩn của người thân đổi.
 */
@Transactional(propagation = Propagation.MANDATORY)
public interface CallReminderCommandApi {

    /**
     * Người thân không còn trong thứ tự gọi của người cao tuổi ({@code FamilyNotificationPort.callOrder}): lượt nhắc gọi
     * Chờ kết quả của người đó dừng ngay với lý do {@code CALLEE_NOT_ELIGIBLE}, chuỗi gọi người kế tiếp như "Không liên
     * lạc được" (FR-029a (a)). Còn đủ điều kiện hoặc không có lượt thì không làm gì.
     */
    void calleeEligibilityChanged(long familyMemberId, long residentId);
}
