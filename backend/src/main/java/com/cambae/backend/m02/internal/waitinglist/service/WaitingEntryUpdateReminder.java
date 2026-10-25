package com.cambae.backend.m02.internal.waitinglist.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m02.internal.transition.service.ReminderLog;
import com.cambae.backend.m02.internal.transition.service.ReminderRule;
import com.cambae.backend.m02.internal.waitinglist.domain.WaitingListEntry;
import com.cambae.backend.m02.internal.waitinglist.persistence.WaitingListStore;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Nhắc hằng ngày "hồ sơ chờ không cập nhật quá CFG-M02-03" (FR-017, BR-M02-03): Hành chính xác nhận gia đình còn nhu cầu.
 * Chỉ hồ sơ Đang chờ (Đã liên hệ đang được xử lý). Lệnh "Xác nhận còn nhu cầu" hay sửa tình huống đặt lại mốc nên hết nhắc;
 * hệ thống không tự hủy hồ sơ vì quá hạn. Mức Nhẹ, chỉ Hành chính.
 */
@Component
class WaitingEntryUpdateReminder implements ReminderRule {

    static final String TYPE = "WAITING_ENTRY_UPDATE";

    private final WaitingListStore store;
    private final ParameterApi parameters;
    private final ReminderLog log;
    private final BusinessTime time;

    WaitingEntryUpdateReminder(WaitingListStore store, ParameterApi parameters, ReminderLog log, BusinessTime time) {
        this.store = store;
        this.parameters = parameters;
        this.log = log;
        this.time = time;
    }

    @Override
    public int remind(LocalDate day) {
        Instant now = time.now();
        Instant staleBefore =
                now.minus(parameters.read(ParameterCode.CFG_M02_03).asDuration());
        int sent = 0;
        for (WaitingListEntry entry : store.listWaitingNotConfirmedSince(staleBefore)) {
            log.send(TYPE, entry.id(), day, now);
            sent++;
        }
        return sent;
    }
}
