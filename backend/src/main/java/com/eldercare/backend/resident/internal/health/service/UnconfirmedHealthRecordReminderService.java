package com.eldercare.backend.resident.internal.health.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.events.UnconfirmedHealthRecordReminder;
import com.eldercare.backend.resident.internal.domain.DueReminder;
import com.eldercare.backend.resident.internal.health.domain.HealthItemStatus;
import com.eldercare.backend.resident.internal.health.domain.InitialHealthRecord;
import com.eldercare.backend.resident.internal.health.persistence.HealthItemRepository;
import com.eldercare.backend.resident.internal.health.persistence.InitialHealthRecordStore;
import com.eldercare.backend.resident.internal.persistence.PeriodicReminderStore;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhắc Bác sĩ, Điều dưỡng khi đã tiếp nhận mà hồ sơ sức khỏe ban đầu chưa được xác nhận hoặc còn mục tự khai chưa kiểm chứng
 * (001 T105; Q-400, BR-M01-17): mỗi CFG-M01-06 kể từ lúc Hoàn tất tiếp nhận, cho tới khi hồ sơ được Bác sĩ xác nhận và hết
 * mục chưa kiểm chứng, hoặc hồ sơ sang trạng thái cuối. Chỉ phát sự kiện và ghi bảng kỹ thuật {@code reminder_marker}, không
 * tạo bản ghi nghiệp vụ (000 FR-010a); người nhận và mức do sự kiện khai báo cho 009.
 */
@Service
public class UnconfirmedHealthRecordReminderService {

    static final String TYPE = "UNCONFIRMED_HEALTH_RECORD";

    private final PeriodicReminderStore reminders;
    private final InitialHealthRecordStore records;
    private final HealthItemRepository items;
    private final ParameterApi parameters;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    UnconfirmedHealthRecordReminderService(
            PeriodicReminderStore reminders,
            InitialHealthRecordStore records,
            HealthItemRepository items,
            ParameterApi parameters,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.reminders = reminders;
        this.records = records;
        this.items = items;
        this.parameters = parameters;
        this.events = events;
        this.time = time;
    }

    /** Hồ sơ chưa xác nhận, hoặc còn mục tự khai Hiệu lực chưa kiểm chứng. */
    @Transactional(readOnly = true)
    public boolean needsReminding(long residentId) {
        boolean confirmed = records.findByResidentId(residentId)
                .map(InitialHealthRecord::isConfirmed)
                .orElse(false);
        return !confirmed || items.existsByResidentIdAndStatusAndUnverifiedTrue(residentId, HealthItemStatus.ACTIVE);
    }

    /** Bắt đầu nhắc cho hồ sơ; gọi lại không thêm dòng (khóa chính chống lặp). */
    @Transactional
    public void startReminding(long residentId) {
        reminders.start(TYPE, residentId, 0);
    }

    /** Số lần nhắc phát trong lần chạy này; hồ sơ đã đủ điều kiện thì bỏ qua. */
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
            events.publishEvent(new UnconfirmedHealthRecordReminder(row.residentId(), number, now));
            sent++;
        }
        return sent;
    }
}
