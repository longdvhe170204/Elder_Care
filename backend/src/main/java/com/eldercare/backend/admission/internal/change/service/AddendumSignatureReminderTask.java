package com.eldercare.backend.admission.internal.change.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.admission.api.events.AddendumSignatureOverdue;
import com.eldercare.backend.admission.api.events.AddendumSignatureReminder;
import com.eldercare.backend.admission.internal.change.domain.Addendum;
import com.eldercare.backend.admission.internal.change.persistence.AddendumStore;
import com.eldercare.backend.admission.internal.contract.persistence.ContractStore;
import com.eldercare.backend.admission.internal.persistence.M02RunStore;
import com.eldercare.backend.admission.internal.transition.service.M02DailyTask;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.LocalDate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Việc {@code ADDENDUM_SIGNATURE_REMINDER} của job hằng ngày (FR-043d, Q-420): phụ lục còn Chờ ký thì nhắc Hành chính mỗi
 * CFG-M02-17 kể từ ngày duyệt (ngày tạo phụ lục); từ ngày duyệt cộng CFG-M02-18 thì báo Quản lý viện đúng một lần. Không
 * tự hủy (Q-306). Dấu nhắc theo ngày ở {@code reminder_marker} nên chạy lại cùng ngày không nhắc lặp.
 */
@Component
@Order(65)
class AddendumSignatureReminderTask implements M02DailyTask {

    static final String NAME = "ADDENDUM_SIGNATURE_REMINDER";
    static final String REMINDED = "ADDENDUM_SIGNATURE";
    static final String OVERDUE = "ADDENDUM_SIGNATURE_OVERDUE";

    private final AddendumStore addenda;
    private final ContractStore contracts;
    private final M02RunStore runs;
    private final ParameterApi parameters;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    AddendumSignatureReminderTask(
            AddendumStore addenda,
            ContractStore contracts,
            M02RunStore runs,
            ParameterApi parameters,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.addenda = addenda;
        this.contracts = contracts;
        this.runs = runs;
        this.parameters = parameters;
        this.events = events;
        this.time = time;
    }

    @Override
    public String name() {
        return NAME;
    }

    /** Nhắc gộp theo ngày mới nhất: ngày bị lỡ không nhắc bù. */
    @Override
    public boolean latestDayOnly() {
        return true;
    }

    @Override
    public int run(LocalDate day) {
        long period = Math.max(
                1, parameters.read(ParameterCode.CFG_M02_17).asDuration().toDays());
        long overdueDays =
                parameters.read(ParameterCode.CFG_M02_18).asDuration().toDays();
        int sent = 0;
        for (Addendum addendum : addenda.pendingSignature()) {
            LocalDate approvedOn =
                    addendum.createdAt().atZone(BusinessTime.ZONE).toLocalDate();
            if (!day.isAfter(approvedOn)) {
                continue;
            }
            long residentId = residentOf(addendum);
            boolean due = runs.lastReminded(REMINDED, addendum.id())
                    .map(last -> !day.isBefore(last.plusDays(period)))
                    .orElse(true);
            if (due && runs.markReminded(REMINDED, addendum.id(), day)) {
                events.publishEvent(new AddendumSignatureReminder(
                        addendum.id(), addendum.approvalRequestId(), residentId, addendum.addendumNo(), time.now()));
                sent++;
            }
            if (!day.isBefore(approvedOn.plusDays(overdueDays))
                    && runs.lastReminded(OVERDUE, addendum.id()).isEmpty()
                    && runs.markReminded(OVERDUE, addendum.id(), day)) {
                events.publishEvent(new AddendumSignatureOverdue(
                        addendum.id(), addendum.approvalRequestId(), residentId, addendum.addendumNo(), time.now()));
                sent++;
            }
        }
        return sent;
    }

    private long residentOf(Addendum addendum) {
        return contracts.find(addendum.contractId()).orElseThrow().residentId();
    }
}
