package com.eldercare.backend.notification.internal.quiethours.service;

import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.parameter.api.events.ParameterChanged;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.notification.internal.domain.NotificationEventType;
import com.eldercare.backend.notification.internal.persistence.NotificationStore;
import com.eldercare.backend.notification.internal.quiethours.domain.QuietHours;
import com.eldercare.backend.notification.internal.quiethours.persistence.HeldNotificationStore;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * CFG-M13-02 đổi (FR-024, research B-06): tính lại thời điểm gửi dự kiến của thông báo đang giữ theo giá trị mới (sự
 * kiện {@code RESCHEDULE}); bây giờ không còn trong giờ yên tĩnh thì dự kiến là bây giờ và giải phóng ngay.
 */
@Service
class QuietHoursRescheduler {

    private final HeldNotificationStore held;
    private final NotificationStore notifications;
    private final QuietHoursRelease release;
    private final TransactionTemplate tx;
    private final BusinessTime time;

    QuietHoursRescheduler(
            HeldNotificationStore held,
            NotificationStore notifications,
            QuietHoursRelease release,
            TransactionTemplate tx,
            BusinessTime time) {
        this.held = held;
        this.notifications = notifications;
        this.release = release;
        this.tx = tx;
        this.time = time;
    }

    @ApplicationModuleListener
    void on(ParameterChanged event) {
        if (event.code() != ParameterCode.CFG_M13_02) {
            return;
        }
        // Giá trị mới lấy từ sự kiện; độ chính xác micro giây như cột DATETIME(6) để so được với bây giờ
        Instant now = time.now().truncatedTo(ChronoUnit.MICROS);
        QuietHours quiet = QuietHoursPolicy.of(event.after());
        Instant scheduled = quiet.endIfWithin(now).orElse(now);
        for (long id : held.allHeld()) {
            tx.executeWithoutResult(s -> held.lock(id)
                    .filter(h -> !h.scheduledSendAt().equals(scheduled))
                    .ifPresent(h -> notifications.appendEvent(
                            id,
                            NotificationEventType.RESCHEDULE,
                            Optional.empty(),
                            Optional.of(scheduled),
                            null,
                            null,
                            now)));
        }
        release.run();
    }
}
