package com.eldercare.backend.resident.internal.consent.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.events.MissingConsentReminder;
import com.eldercare.backend.resident.internal.domain.DueReminder;
import com.eldercare.backend.resident.internal.persistence.PeriodicReminderStore;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhắc Hành chính khi đã tiếp nhận mà chưa có bản đồng ý Hiệu lực (001 T058; FR-012, SC-013): mỗi CFG-M01-06 kể từ lúc
 * bắt đầu (Hoàn tất tiếp nhận thiếu bản đồng ý), cho tới khi có bản Hiệu lực hoặc hồ sơ sang trạng thái cuối. Chỉ phát
 * sự kiện và ghi bảng kỹ thuật {@code reminder_marker}, không tạo bản ghi nghiệp vụ (000 FR-010a). Người nhận và mức
 * do 009 xếp.
 */
@Service
public class MissingConsentReminderService {

    public static final String JOB = "MISSING_CONSENT_REMINDER";
    static final String TYPE = "MISSING_CONSENT";

    private final PeriodicReminderStore reminders;
    private final ParameterApi parameters;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    MissingConsentReminderService(
            PeriodicReminderStore reminders,
            ParameterApi parameters,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.reminders = reminders;
        this.parameters = parameters;
        this.events = events;
        this.time = time;
    }

    /** Bắt đầu nhắc cho hồ sơ; gọi lại không thêm dòng (khóa chính chống lặp). */
    @Transactional
    public void startReminding(long residentId) {
        reminders.start(TYPE, residentId, 0);
    }

    /** Số lần nhắc phát trong lần chạy này. */
    @Transactional
    public int run() {
        Duration period = parameters.read(ParameterCode.CFG_M01_06).asDuration();
        Instant now = time.now();
        List<DueReminder> due = reminders.dueWithoutActive(TYPE, "consent", now.minus(period));
        for (DueReminder row : due) {
            int number = row.reminderCount() + 1;
            reminders.markReminded(TYPE, row.residentId(), 0, number);
            events.publishEvent(new MissingConsentReminder(row.residentId(), number, now));
        }
        return due.size();
    }
}
