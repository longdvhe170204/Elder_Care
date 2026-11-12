package com.eldercare.backend.family.internal.relationship.service;

import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.family.api.events.FamilyRightChanged;
import com.eldercare.backend.family.api.events.RelationshipChanged;
import com.eldercare.backend.notification.api.CallReminderCommandApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 009 T019 (chủ dự án chốt 2026-11-12): mọi lệnh của 012 đổi quan hệ hoặc tắt quyền nhận thông báo khẩn đều phát sự kiện
 * của chính 012; listener đồng bộ này gọi {@link CallReminderCommandApi} trong transaction của lệnh đó để lượt nhắc gọi
 * người thân không còn đủ điều kiện dừng ngay. Lỗi ở 009 làm lệnh của 012 rollback.
 */
@Component
class CallReminderEligibilityListener {

    private final ObjectProvider<CallReminderCommandApi> reminders;

    CallReminderEligibilityListener(ObjectProvider<CallReminderCommandApi> reminders) {
        this.reminders = reminders;
    }

    @EventListener
    void on(RelationshipChanged event) {
        reminders.ifAvailable(api -> api.calleeEligibilityChanged(event.familyMemberId(), event.residentId()));
    }

    @EventListener
    void on(FamilyRightChanged event) {
        if (event.right() == FamilyRight.RECEIVE_URGENT && !event.value()) {
            reminders.ifAvailable(api -> api.calleeEligibilityChanged(event.familyMemberId(), event.residentId()));
        }
    }
}
