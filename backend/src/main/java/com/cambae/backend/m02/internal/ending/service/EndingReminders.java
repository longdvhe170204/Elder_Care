package com.cambae.backend.m02.internal.ending.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m02.internal.ending.domain.ResidencyEnding;
import com.cambae.backend.m02.internal.ending.persistence.ResidencyEndingStore;
import com.cambae.backend.m02.internal.transition.service.ReminderLog;
import com.cambae.backend.m02.internal.transition.service.ReminderRule;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Nhắc Hành chính đặt ngày kết thúc dự kiến mới mỗi ngày khi hồ sơ kết thúc mang dấu "quá ngày dự kiến" (FR-066a; loại nhắc
 * {@code PAST_EXPECTED_DATE}, mức Nhẹ). Không tạo bản ghi nghiệp vụ nào (000 FR-010a).
 */
@Component
class EndingReminders implements ReminderRule {

    static final String TYPE = "PAST_EXPECTED_DATE";

    private final ResidencyEndingStore store;
    private final ReminderLog log;
    private final BusinessTime time;

    EndingReminders(ResidencyEndingStore store, ReminderLog log, BusinessTime time) {
        this.store = store;
        this.log = log;
        this.time = time;
    }

    @Override
    public int remind(LocalDate day) {
        Instant now = time.now();
        int sent = 0;
        for (ResidencyEnding ending : store.overdue()) {
            log.send(TYPE, ending.id(), day, now);
            sent++;
        }
        return sent;
    }
}
