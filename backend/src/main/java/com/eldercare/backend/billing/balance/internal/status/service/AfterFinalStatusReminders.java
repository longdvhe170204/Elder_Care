package com.eldercare.backend.billing.balance.internal.status.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.admission.api.ResidencyEndingReadApi;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTypes;
import com.eldercare.backend.billing.balance.internal.settlement.domain.SettlementRules;
import com.eldercare.backend.billing.balance.internal.status.domain.AfterFinalCandidate;
import com.eldercare.backend.billing.balance.internal.status.domain.BalanceNotice;
import com.eldercare.backend.billing.balance.internal.status.persistence.AfterFinalStatusMarks;
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
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhắc Kế toán sau trạng thái cuối (017 T031; FR-024, Q-224; research E-08): giao dịch Đã xác nhận sau khi hồ sơ ở trạng
 * thái cuối mà chưa xác nhận quyết toán lại (hoặc chưa từng có bảng quyết toán và hai sổ chưa về 0) thì nhắc mỗi
 * CFG-M02-09; không mở lại hồ sơ, không tự lập giao dịch (Q-306). Đặt ở phần tình trạng vì job số dư không được phụ thuộc
 * phần giao dịch ({@code M11ArchTest}).
 */
@Component
public class AfterFinalStatusReminders {

    static final String TYPE = "M11_SETTLEMENT_AFTER_FINAL_STATUS";

    private final AfterFinalStatusMarks marks;
    private final ResidentApi residents;
    private final ObjectProvider<ResidencyEndingReadApi> endings;
    private final ParameterApi parameters;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    AfterFinalStatusReminders(
            AfterFinalStatusMarks marks,
            ResidentApi residents,
            ObjectProvider<ResidencyEndingReadApi> endings,
            ParameterApi parameters,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.marks = marks;
        this.residents = residents;
        this.endings = endings;
        this.parameters = parameters;
        this.events = events;
        this.time = time;
    }

    /** Phần nhắc của job hằng ngày; trả số lần nhắc. */
    @Transactional
    public int remind() {
        Instant now = time.now();
        var every = parameters.read(ParameterCode.CFG_M02_09).asDuration();
        ResidencyEndingReadApi ending = endings.getIfAvailable();
        int count = 0;
        for (AfterFinalCandidate c : marks.candidates()) {
            long residentId = c.residentId();
            // hồ sơ chưa ở trạng thái cuối là phần lớn ứng viên: loại trước khi đọc 004
            if (!residents.isFinalStatus(residentId)) continue;
            Optional<Instant> finalAt = residents.finalStatusAt(residentId);
            boolean due = SettlementRules.afterFinalReminderDue(
                    true,
                    ending != null && ending.settlementInfo(residentId).isPresent(),
                    finalAt.isPresent()
                            && c.lastConfirmedAt() != null
                            && c.lastConfirmedAt().isAfter(finalAt.get()),
                    c.statementExists(),
                    true,
                    c.balance(),
                    c.depositBalance());
            if (!due || (c.remindedAt() != null && c.remindedAt().plus(every).isAfter(now))) continue;
            notifyAccountant(residentId, c.balance(), now);
            marks.reminded(residentId, now);
            count++;
        }
        return count;
    }

    private void notifyAccountant(long residentId, long balance, Instant now) {
        String text = "Hồ sơ " + residents.identity(residentId).fullName()
                + " đã ở trạng thái cuối nhưng có giao dịch mới (số dư "
                + String.format(Locale.GERMANY, "%,d", balance)
                + " đồng): vui lòng quyết toán phần mới và xác nhận quyết toán lại.";
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
                Optional.of("Quyết toán"),
                Optional.empty(),
                false,
                now,
                Map.of(),
                false,
                Optional.empty(),
                Optional.empty()))));
    }
}
