package com.eldercare.backend.care.internal.careplan.service;

import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.events.RiskAgreementReviewReminder;
import com.eldercare.backend.care.internal.careplan.domain.*;
import com.eldercare.backend.care.internal.careplan.persistence.*;
import com.eldercare.backend.care.internal.scheduling.service.CareDailyWork;
import com.eldercare.backend.care.internal.status.persistence.CareLockStore;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.notification.*;
import java.time.*;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** Q-360: chỉ nhắc, không sửa thỏa thuận hoặc tạo công việc. */
@Component
public class CareRiskReview implements CareDailyWork {
    private final CarePlanStore plans;
    private final CareRiskStore risks;
    private final CareLockStore locks;
    private final ReminderMarkers markers;
    private final ApplicationEventPublisher events;

    public CareRiskReview(
            CarePlanStore plans,
            CareRiskStore risks,
            CareLockStore locks,
            ReminderMarkers markers,
            ApplicationEventPublisher events) {
        this.plans = plans;
        this.risks = risks;
        this.locks = locks;
        this.markers = markers;
        this.events = events;
    }

    @Override
    public String name() {
        return "CARE_RISK_REVIEW";
    }

    @Override
    public List<LocalDate> dueDates(LocalDate today) {
        return risks.reviewDates(today);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int run(LocalDate date, Instant now) {
        var candidates = risks.plansForReview(date).stream().map(plans::require).toList();
        locks.lock(candidates.stream().map(CarePlan::residentId).toList());
        int count = 0;
        for (var candidate : candidates) {
            var plan = plans.locked(candidate.id());
            if (plan.status() != CarePlanStatus.EFFECTIVE
                    || plan.effectiveFrom().isAfter(date)) continue;
            for (var risk : risks.list(plan.id())) {
                if (risk.reviewDate().equals(date)) count += remind(plan, risk, "AGREEMENT", date, now);
                if (risk.selfDeterminationAssessment().reviewDate().equals(date))
                    count += remind(plan, risk, "ASSESSMENT", date, now);
            }
        }
        return count;
    }

    private int remind(CarePlan plan, CareRiskAgreement risk, String type, LocalDate date, Instant now) {
        if (!markers.mark(
                "M04.RISK_REVIEW",
                "M04.RISK_AGREEMENT",
                risk.id(),
                type + ":" + date,
                Map.of("residentId", plan.residentId(), "date", date.toString(), "reviewDateType", type))) return 0;
        var context = new CareEventContext(
                "M04.RISK_AGREEMENT",
                risk.id(),
                date.toString(),
                now,
                Optional.empty(),
                List.of(
                        new RecipientGroup.AssignedNurse(plan.residentId(), new Substitution.Default()),
                        new RecipientGroup.Staff(risk.assessingDoctorId(), "ASSESSING_DOCTOR")));
        events.publishEvent(new RiskAgreementReviewReminder(risk.id(), type, context));
        return 1;
    }
}
