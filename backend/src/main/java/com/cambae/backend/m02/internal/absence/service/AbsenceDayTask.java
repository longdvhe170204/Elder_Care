package com.cambae.backend.m02.internal.absence.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m02.api.events.BedHoldDecisionNeeded;
import com.cambae.backend.m02.api.events.BedHoldReviewDue;
import com.cambae.backend.m02.internal.absence.domain.Absence;
import com.cambae.backend.m02.internal.absence.domain.BedHoldDecision;
import com.cambae.backend.m02.internal.absence.persistence.AbsenceStore;
import com.cambae.backend.m02.internal.absence.service.AbsenceDayCalculator.Written;
import com.cambae.backend.m02.internal.transition.service.M02DailyTask;
import com.cambae.backend.m02.internal.transition.service.ResidencyLock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Việc {@code ABSENCE_DAYS} của job hằng ngày (FR-052, FR-055, FR-056, BR-M02-06, 07; research T-08, T-11): mỗi lượt vắng
 * đang mở ghi nốt các ngày còn thiếu tới ngày chạy với bảng CFG-M02-05 hiện hành; tới ngưỡng giữ giường thì báo Quản lý
 * viện một lần (không tạo yêu cầu, Q-307); tới ngày xem xét lại thì báo lại. Ngày đã ghi không đổi khi bảng đổi.
 */
@Component
@Order(20)
class AbsenceDayTask implements M02DailyTask {

    static final String NAME = "ABSENCE_DAYS";

    private final AbsenceStore store;
    private final AbsenceDayCalculator calculator;
    private final AbsenceService service;
    private final ResidencyLock residencyLock;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    AbsenceDayTask(
            AbsenceStore store,
            AbsenceDayCalculator calculator,
            AbsenceService service,
            ResidencyLock residencyLock,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.store = store;
        this.calculator = calculator;
        this.service = service;
        this.residencyLock = residencyLock;
        this.events = events;
        this.time = time;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public int run(LocalDate day) {
        int changed = 0;
        for (Absence listed : store.allOpen()) {
            if (AbsenceDayCalculator.dateOf(listed.departedAt()).isAfter(day)) {
                continue;
            }
            residencyLock.lock(listed.residentId());
            Absence absence = store.find(listed.id()).orElseThrow();
            if (!absence.open()) {
                continue;
            }
            List<BedHoldDecision> decisions = service.effectiveDecisions(absence.id());
            Written written = calculator.fillMissing(absence, day, decisions);
            service.publishDayEvents(absence, written);
            changed += written.days().size();
            Instant now = time.now();
            if (absence.thresholdNotifiedAt() == null
                    && decisions.isEmpty()
                    && AbsenceDayCalculator.held(absence, decisions, day)) {
                var threshold = store.firstNonHoldDay(absence.id());
                if (threshold.isPresent()) {
                    store.markThresholdNotified(absence.id(), now);
                    events.publishEvent(new BedHoldDecisionNeeded(
                            absence.id(),
                            absence.residentId(),
                            threshold.get().sequenceNo(),
                            threshold.get().policyLine(),
                            now));
                    changed++;
                }
            }
            if (day.equals(absence.reviewDate())) {
                events.publishEvent(new BedHoldReviewDue(absence.id(), absence.residentId(), now));
                changed++;
            }
        }
        return changed;
    }
}
