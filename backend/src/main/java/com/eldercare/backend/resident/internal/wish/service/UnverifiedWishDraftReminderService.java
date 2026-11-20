package com.eldercare.backend.resident.internal.wish.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.events.UnverifiedWishDraftReminder;
import com.eldercare.backend.resident.internal.domain.DueReminder;
import com.eldercare.backend.resident.internal.persistence.PeriodicReminderStore;
import com.eldercare.backend.resident.internal.wish.persistence.WishDraftStore;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhắc Bác sĩ, Điều dưỡng khi đã tiếp nhận mà còn phiếu nguyện vọng tự khai chưa kiểm chứng (001 T114; Q-412, FR-023r):
 * mỗi CFG-M01-06 kể từ lúc Hoàn tất tiếp nhận, tới khi mọi Nháp được kiểm chứng hoặc Hủy nháp, hoặc hồ sơ sang trạng thái
 * cuối. Mẫu {@code UnconfirmedHealthRecordReminderService}: chỉ phát sự kiện và ghi {@code reminder_marker}.
 */
@Service
public class UnverifiedWishDraftReminderService {

    static final String TYPE = "UNVERIFIED_WISH_DRAFT";

    private final PeriodicReminderStore reminders;
    private final WishDraftStore drafts;
    private final ParameterApi parameters;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    UnverifiedWishDraftReminderService(
            PeriodicReminderStore reminders,
            WishDraftStore drafts,
            ParameterApi parameters,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.reminders = reminders;
        this.drafts = drafts;
        this.parameters = parameters;
        this.events = events;
        this.time = time;
    }

    @Transactional(readOnly = true)
    public boolean needsReminding(long residentId) {
        return !drafts.openDraftKinds(residentId).isEmpty();
    }

    /** Bắt đầu nhắc cho hồ sơ; gọi lại không thêm dòng (khóa chính chống lặp). */
    @Transactional
    public void startReminding(long residentId) {
        reminders.start(TYPE, residentId, 0);
    }

    /** Số lần nhắc phát trong lần chạy này; hồ sơ không còn Nháp thì bỏ qua. */
    @Transactional
    public int run() {
        Duration period = parameters.read(ParameterCode.CFG_M01_06).asDuration();
        Instant now = time.now();
        List<DueReminder> due = reminders.due(TYPE, now.minus(period));
        int sent = 0;
        for (DueReminder row : due) {
            if (!needsReminding(row.residentId())) {
                continue;
            }
            int number = row.reminderCount() + 1;
            reminders.markReminded(TYPE, row.residentId(), 0, number);
            events.publishEvent(new UnverifiedWishDraftReminder(row.residentId(), number, now));
            sent++;
        }
        return sent;
    }
}
