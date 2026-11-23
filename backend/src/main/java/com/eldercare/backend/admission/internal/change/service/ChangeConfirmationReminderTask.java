package com.eldercare.backend.admission.internal.change.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.admission.api.events.ResidencyChangeConfirmationReminder;
import com.eldercare.backend.admission.internal.change.domain.ChangeConfirmationRequest;
import com.eldercare.backend.admission.internal.transition.service.M02DailyTask;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Việc {@code CHANGE_CONFIRMATION_REMINDER} của job hằng ngày (FR-043c): đề nghị xác nhận còn chờ người đại diện quá
 * CFG-M15-05 (thời hạn nhắc yêu cầu phê duyệt chờ quá lâu, dùng chung thay cho tham số mới) tính tới cuối ngày chạy thì
 * nhắc người đại diện và báo Hành chính đúng một lần; không tự hủy (Q-306).
 */
@Component
@Order(60)
class ChangeConfirmationReminderTask implements M02DailyTask {

    static final String NAME = "CHANGE_CONFIRMATION_REMINDER";

    private final ChangeConfirmations confirmations;
    private final ParameterApi parameters;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    ChangeConfirmationReminderTask(
            ChangeConfirmations confirmations,
            ParameterApi parameters,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.confirmations = confirmations;
        this.parameters = parameters;
        this.events = events;
        this.time = time;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public int run(LocalDate day) {
        Duration threshold = parameters.read(ParameterCode.CFG_M15_05).asDuration();
        Instant endOfDay = time.startOfDay(day.plusDays(1));
        Instant now = time.now();
        int sent = 0;
        for (SimpleRecord<ChangeConfirmationRequest> r : confirmations.awaiting()) {
            ChangeConfirmationRequest content = r.content();
            if (content.remindedAt() != null
                    || r.residentId() == null
                    || content.sentAt().plus(threshold).isAfter(endOfDay)) {
                continue;
            }
            confirmations.change(r, r.status(), content.reminded(now));
            events.publishEvent(
                    new ResidencyChangeConfirmationReminder(r.id(), content.requestId(), r.residentId(), now));
            sent++;
        }
        return sent;
    }
}
