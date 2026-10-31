package com.eldercare.backend.administration.approval.internal.service;

import com.eldercare.backend.administration.approval.api.OverdueReminder;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Hai mốc tính theo giờ liên tục từ {@code since}, đọc tham số lúc gọi (FR-031a, FR-018). Tới mốc nhắc mà không còn
 * người duyệt thì báo Quản lý viện ngay (BR-M15-09). Mỗi việc chỉ một lần nhờ {@code remindedAt}, {@code escalatedAt}.
 */
@Component
class OverdueReminderImpl implements OverdueReminder {

    private final ParameterApi parameters;
    private final BusinessTime time;

    OverdueReminderImpl(ParameterApi parameters, BusinessTime time) {
        this.parameters = parameters;
        this.time = time;
    }

    @Override
    public Decision check(
            Instant since,
            Instant remindedAt,
            Instant escalatedAt,
            ParameterCode remind,
            ParameterCode escalate,
            boolean hasApprover) {
        Duration elapsed = Duration.between(since, time.now());
        boolean pastRemind = elapsed.compareTo(parameters.read(remind).asDuration()) >= 0;
        boolean pastEscalate = elapsed.compareTo(parameters.read(escalate).asDuration()) >= 0;
        if (!hasApprover) {
            return pastRemind && escalatedAt == null
                    ? new Decision(false, EscalationReason.NO_APPROVER_LEFT)
                    : Decision.NONE;
        }
        return new Decision(
                pastRemind && remindedAt == null,
                pastEscalate && escalatedAt == null ? EscalationReason.OVERDUE : null);
    }
}
