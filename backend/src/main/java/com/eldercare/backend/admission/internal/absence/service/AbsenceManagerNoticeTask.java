package com.eldercare.backend.admission.internal.absence.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.admission.api.events.LongAbsenceManagerNotice;
import com.eldercare.backend.admission.internal.absence.domain.Absence;
import com.eldercare.backend.admission.internal.absence.persistence.AbsenceStore;
import com.eldercare.backend.admission.internal.transition.service.M02DailyTask;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Việc {@code ABSENCE_NOTICE} của job hằng ngày (T052; FR-055, BR-M02-07, Q-395): lượt vắng của nội trú đang mở, giường còn giữ, tới
 * ngày vắng thứ CFG-M02-05 thì báo Quản lý viện đúng một lần (mức Trung bình); không tạo yêu cầu (Q-307). Ngày rời viện là ngày 1.
 */
@Component
@Order(20)
class AbsenceManagerNoticeTask implements M02DailyTask {

    static final String NAME = "ABSENCE_NOTICE";

    private final AbsenceStore store;
    private final ParameterApi parameters;
    private final ApplicationEventPublisher events;

    AbsenceManagerNoticeTask(AbsenceStore store, ParameterApi parameters, ApplicationEventPublisher events) {
        this.store = store;
        this.parameters = parameters;
        this.events = events;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public int run(LocalDate day) {
        int threshold = parameters
                .read(ParameterCode.CFG_M02_05)
                .raw()
                .get("managerNoticeDays")
                .asInt();
        Instant at = day.atStartOfDay(com.eldercare.backend.common.time.BusinessTime.ZONE)
                .toInstant();
        int sent = 0;
        for (Absence absence : store.allOpen()) {
            if (absence.managerNotifiedAt() != null || !absence.holdBed()) {
                continue;
            }
            long absentDays = ChronoUnit.DAYS.between(AbsenceService.dateOf(absence.departedAt()), day) + 1;
            if (absentDays >= threshold && store.markManagerNotified(absence.id(), at)) {
                events.publishEvent(
                        new LongAbsenceManagerNotice(absence.id(), absence.residentId(), (int) absentDays, at));
                sent++;
            }
        }
        return sent;
    }
}
