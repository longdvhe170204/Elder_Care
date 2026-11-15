package com.eldercare.backend.billing.balance.internal.transaction.service;

import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTransactionRow;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTypes;
import com.eldercare.backend.billing.balance.internal.status.domain.BalanceNotice;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.common.time.BusinessTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Báo Kế toán khi lệnh duyệt bị chặn vì giới hạn số tiền hoặc người nhận (017 FR-030 dòng "lệnh duyệt bị chặn", FR-014;
 * US6 #3, #8): lệnh duyệt rollback nên thông báo ghi ở transaction riêng.
 */
@Component
class BlockedApprovalNotices {

    static final String TYPE = "M11_BALANCE_APPROVAL_BLOCKED";

    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    BlockedApprovalNotices(ApplicationEventPublisher events, BusinessTime time) {
        this.events = events;
        this.time = time;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyAccountant(BalanceTransactionRow row, List<UnmetCondition> unmet) {
        String text = "Lệnh duyệt giao dịch số " + row.id() + " bị chặn: "
                + unmet.stream().map(UnmetCondition::description).collect(Collectors.joining("; "))
                + ". Vui lòng lập lại.";
        var now = time.now();
        events.publishEvent(new BalanceNotice(List.of(new NotificationRequest(
                "M11",
                TYPE,
                SourceGroup.COSTS,
                new SourceRecord(BalanceTypes.TRANSACTION, Long.toString(row.id())),
                TYPE + ":" + row.id() + ":" + now,
                Optional.of(NotificationLevel.LOW),
                Optional.of(row.residentId()),
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
