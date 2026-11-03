package com.eldercare.backend.notification.internal.urgent.service;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.events.CallReminderStopRequested;
import com.eldercare.backend.incident.api.events.WishReconfirmationCompleted;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Hai sự kiện điều khiển lượt nhắc gọi của 007 (research B-08; module-interfaces 009 mục 2): không phải thông báo, chỉ
 * dừng lượt. Lượt đã dừng thì bỏ qua nên chạy lại không sao.
 */
@Component
class IncidentCallReminderListener {

    /** Loại bản ghi nguồn của cảnh báo 007 trong yêu cầu thông báo. */
    static final String ALERT = "M05.ALERT";

    private final CallReminderActions actions;
    private final BusinessTime time;

    IncidentCallReminderListener(CallReminderActions actions, BusinessTime time) {
        this.actions = actions;
        this.time = time;
    }

    /** Sự cố hạ mức khỏi Khẩn cấp (BR-M05-22). */
    @ApplicationModuleListener
    void on(CallReminderStopRequested event) {
        actions.stopForSource(
                event.sourceRecordType(),
                Long.toString(event.sourceRecordId()),
                "SOURCE_DOWNGRADED",
                "Không cần gọi: sự cố đã hạ mức khỏi Khẩn cấp",
                time.now());
    }

    /** Đã có kết quả xác nhận lại nguyện vọng của cảnh báo nguy kịch (007 FR-050b d). */
    @ApplicationModuleListener
    void on(WishReconfirmationCompleted event) {
        actions.stopForSource(
                ALERT,
                Long.toString(event.alertId()),
                "WISH_RECONFIRMED",
                "Không cần gọi: đã có kết quả xác nhận lại nguyện vọng",
                time.now());
    }
}
