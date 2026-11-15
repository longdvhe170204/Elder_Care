package com.eldercare.backend.billing.balance.internal.status.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.admission.api.dto.ContractStatus;
import com.eldercare.backend.admission.api.events.ContractStatusChanged;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTypes;
import com.eldercare.backend.billing.balance.internal.status.domain.BalanceNotice;
import com.eldercare.backend.billing.balance.internal.status.domain.DepositRefundDue;
import com.eldercare.backend.billing.balance.internal.status.persistence.DepositRefundMarks;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.events.ResidentStatusChanged;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhắc hoàn cọc (017 T026; FR-015a; Q-225): hợp đồng Đã hủy trước khi Hiệu lực, hoặc hồ sơ Hủy tiếp nhận, mà số dư cọc
 * lớn hơn 0 thì đặt mốc; job hằng ngày nhắc Kế toán mỗi CFG-M02-09 tới khi số dư cọc về 0. Không chặn Hủy tiếp nhận,
 * không tự lập hoàn cọc (Q-306). Đặt ở phần tình trạng vì job số dư không được phụ thuộc phần giao dịch
 * ({@code M11ArchTest}).
 */
@Component
public class DepositRefundReminders {

    static final String TYPE = "M11_DEPOSIT_REFUND_DUE";

    private final DepositRefundMarks marks;
    private final ParameterApi parameters;
    private final ResidentApi residents;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    DepositRefundReminders(
            DepositRefundMarks marks,
            ParameterApi parameters,
            ResidentApi residents,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.marks = marks;
        this.parameters = parameters;
        this.residents = residents;
        this.events = events;
        this.time = time;
    }

    @EventListener
    @Transactional
    public void onContract(ContractStatusChanged event) {
        if (event.to() == ContractStatus.CANCELLED && event.from() != ContractStatus.ACTIVE) {
            marks.due(event.residentId(), time.now());
        }
    }

    @EventListener
    @Transactional
    public void onResident(ResidentStatusChanged event) {
        if (event.to() == ResidentStatus.ADMISSION_CANCELLED) marks.due(event.residentId(), time.now());
    }

    /** Phần nhắc của job hằng ngày; trả số lần nhắc. */
    @Transactional
    public int remind() {
        Instant now = time.now();
        var every = parameters.read(ParameterCode.CFG_M02_09).asDuration();
        int count = 0;
        for (DepositRefundDue due : marks.dueList()) {
            if (due.remindedAt() != null && due.remindedAt().plus(every).isAfter(now)) continue;
            notifyAccountant(due.residentId(), due.depositBalance(), now);
            marks.reminded(due.residentId(), now);
            count++;
        }
        return count;
    }

    private void notifyAccountant(long residentId, long deposit, Instant now) {
        String text = "Tiền cọc của " + residents.identity(residentId).fullName() + " còn "
                + String.format(Locale.GERMANY, "%,d", deposit)
                + " đồng sau khi hợp đồng bị hủy hoặc hủy tiếp nhận: vui lòng lập hoàn cọc.";
        events.publishEvent(new BalanceNotice(List.of(new NotificationRequest(
                "M11",
                TYPE,
                SourceGroup.COSTS,
                new SourceRecord(BalanceTypes.LEDGER, Long.toString(residentId)),
                TYPE + ":" + residentId + ":" + time.today(),
                Optional.of(NotificationLevel.LOW),
                Optional.of(residentId),
                Optional.empty(),
                List.of(new RecipientGroup.FacilityRole(Role.ACCOUNTANT)),
                List.of(new ContentPart(InformationType.GENERAL, text)),
                Optional.of(text),
                Optional.of("Số dư"),
                Optional.empty(),
                false,
                now,
                Map.of(),
                false,
                Optional.empty(),
                Optional.empty()))));
    }
}
