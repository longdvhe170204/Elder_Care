package com.eldercare.backend.notification.internal.urgent.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.notification.api.dto.RecipientType;
import com.eldercare.backend.notification.internal.job.service.M13FastTask;
import com.eldercare.backend.notification.internal.persistence.NotificationStore;
import com.eldercare.backend.notification.internal.urgent.domain.DueNotification;
import com.eldercare.backend.notification.internal.urgent.domain.Reminder;
import com.eldercare.backend.notification.internal.urgent.persistence.CallReminderStore;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Tạo lượt nhắc gọi (việc 20 giây {@code CREATE_CALL_REMINDERS}; FR-027, FR-027a, FR-028, FR-032, research B-08): thông báo
 * Khẩn cấp chưa xác nhận quá CFG-M13-01 tính từ lúc gửi, hoặc gửi thất bại mọi kênh (không chờ CFG-M13-01, trễ tối đa một
 * nhịp việc), gộp vào lượt Chờ kết quả của cùng người cần gọi về cùng người cao tuổi (không gắn người cao tuổi thì cùng bản
 * ghi nguồn), không có thì tạo lượt mới giao người được nhắc theo FR-029. Mỗi thông báo một transaction, khóa dòng trạng
 * thái thông báo rồi xét lại để xác nhận đồng thời không bị bỏ sót.
 */
@Service
public class CallReminderCreation implements M13FastTask {

    private static final Logger log = LoggerFactory.getLogger(CallReminderCreation.class);

    static final int BATCH = 200;

    private final CallReminderStore store;
    private final NotificationStore notifications;
    private final CallReminderRouting routing;
    private final CallReminderNotices notices;
    private final CallReminderActions actions;
    private final ParameterApi parameters;
    private final TransactionTemplate tx;
    private final BusinessTime time;

    CallReminderCreation(
            CallReminderStore store,
            NotificationStore notifications,
            CallReminderRouting routing,
            CallReminderNotices notices,
            CallReminderActions actions,
            ParameterApi parameters,
            TransactionTemplate tx,
            BusinessTime time) {
        this.store = store;
        this.notifications = notifications;
        this.routing = routing;
        this.notices = notices;
        this.actions = actions;
        this.parameters = parameters;
        this.tx = tx;
        this.time = time;
    }

    @Override
    public String name() {
        return "CREATE_CALL_REMINDERS";
    }

    @Override
    public int run() {
        Instant now = time.now();
        int created = 0;
        for (DueNotification d :
                store.due(now.minus(parameters.read(ParameterCode.CFG_M13_01).asDuration()), BATCH)) {
            try {
                Boolean done = tx.execute(s -> remind(d, now));
                if (Boolean.TRUE.equals(done)) {
                    created++;
                }
            } catch (RuntimeException e) {
                // Một lượt lỗi không chặn các lượt khác; nhịp sau xét lại
                log.warn("Tạo lượt nhắc gọi lỗi", e);
            }
        }
        return created;
    }

    private boolean remind(DueNotification d, Instant now) {
        Optional<String> status = notifications.lockStatus(d.notificationId());
        if (status.filter(st -> st.equals("ACKNOWLEDGED") || st.equals("UNREACHABLE"))
                        .isPresent()
                || store.linked(d.notificationId())) {
            return false;
        }
        String key = calleeKey(d.calleeType(), d.calleeId(), d.residentId(), d.sourceRecordType(), d.sourceRecordId());
        Optional<Long> pending = store.lockPending(key);
        if (pending.isPresent()) {
            Reminder r = store.find(pending.get()).orElseThrow();
            store.link(r.id(), d.notificationId(), now);
            long handoff = store.handoff(
                    r.id(), r.currentStaffId(), r.currentStaffId(), r.tier(), "CONTENT_UPDATED", r.dueAt(), now);
            notices.inform(
                    r,
                    java.util.List.of(r.currentStaffId()),
                    "CALL_REMINDER_UPDATED",
                    Long.toString(handoff),
                    "Lượt nhắc gọi có thêm nội dung cần báo",
                    now);
            return true;
        }
        Optional<Long> calleeStaff =
                d.calleeType() == RecipientType.STAFF ? Optional.of(d.calleeId()) : Optional.empty();
        Optional<CallReminderRouting.Assignment> to = routing.initial(d.floorAreaId(), calleeStaff, now);
        if (to.isEmpty()) {
            // Không ai trong thứ tự FR-029 đủ điều kiện: xét lại ở nhịp sau
            return false;
        }
        Instant due = now.plus(parameters.read(ParameterCode.CFG_M13_04).asDuration());
        long id = store.insertReminder(
                null,
                d.calleeType(),
                d.calleeId(),
                d.residentId(),
                d.sourceRecordType(),
                d.sourceRecordId(),
                d.floorAreaId(),
                d.deliveryFailed() ? "DELIVERY_FAILED" : "UNACKNOWLEDGED",
                due,
                to.get().staffId(),
                key,
                now);
        store.link(id, d.notificationId(), now);
        actions.assign(id, null, to.get(), "INITIAL", due, now);
        return true;
    }

    static String calleeKey(
            RecipientType type, long calleeId, Long residentId, String sourceRecordType, String sourceRecordId) {
        return type.name() + ":" + calleeId + ":"
                + (residentId != null ? "resident:" + residentId : "source:" + sourceRecordType + ":" + sourceRecordId);
    }
}
