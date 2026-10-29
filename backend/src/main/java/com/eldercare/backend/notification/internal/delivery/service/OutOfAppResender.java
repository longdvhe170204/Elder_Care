package com.eldercare.backend.notification.internal.delivery.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.ParameterCode;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.notification.internal.delivery.persistence.DeliveryStore;
import com.eldercare.backend.notification.internal.delivery.persistence.DeliveryStore.Retry;
import com.eldercare.backend.notification.internal.job.service.M13FastTask;
import com.eldercare.backend.notification.internal.persistence.NotificationStore;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Gửi lại kênh ngoài ứng dụng (việc 20 giây {@code RESEND_OUT_OF_APP}; FR-018, research B-05): lần gửi lỗi tạm được gửi
 * lại tới khi số lần gửi lại đạt CFG-M13-05; lần gửi mới cùng địa chỉ, cùng nội dung. {@code UNIQUE (thông báo, kênh,
 * lần thử)} chặn gửi trùng khi hai lượt chạy cùng lúc (NFR-04).
 */
@Service
public class OutOfAppResender implements M13FastTask {

    private static final int BATCH = 200;

    private final DeliveryStore deliveries;
    private final NotificationStore notifications;
    private final DeliveryDispatcher dispatcher;
    private final ParameterApi parameters;
    private final TransactionTemplate tx;
    private final BusinessTime time;

    OutOfAppResender(
            DeliveryStore deliveries,
            NotificationStore notifications,
            DeliveryDispatcher dispatcher,
            ParameterApi parameters,
            TransactionTemplate tx,
            BusinessTime time) {
        this.deliveries = deliveries;
        this.notifications = notifications;
        this.dispatcher = dispatcher;
        this.parameters = parameters;
        this.tx = tx;
        this.time = time;
    }

    @Override
    public String name() {
        return "RESEND_OUT_OF_APP";
    }

    @Override
    public int run() {
        int maxResends = parameters.read(ParameterCode.CFG_M13_05).asInt();
        Instant now = time.now();
        List<Retry> due = deliveries.retryable(maxResends, BATCH);
        for (Retry r : due) {
            tx.executeWithoutResult(s -> {
                long attempt =
                        notifications.planAttempt(r.notificationId(), r.channel(), r.address(), now, r.content());
                notifications.recompute(r.notificationId());
                dispatcher.dispatch(List.of(attempt));
            });
        }
        return due.size();
    }
}
