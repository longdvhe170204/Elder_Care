package com.eldercare.backend.care.internal.careplan.service;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.care.api.CarePlanApi;
import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.internal.careplan.domain.*;
import com.eldercare.backend.care.internal.careplan.persistence.CarePlanStore;
import com.eldercare.backend.care.internal.careplan.persistence.CareRiskStore;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.LocalDate;
import java.util.*;
import org.springframework.context.annotation.Fallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Nguồn thật kế hoạch cho module khác; fake fixture được ưu tiên khi test chủ động import. */
@Service
@Fallback
@Transactional(readOnly = true)
public class CarePlanQueries implements CarePlanApi {
    private final CarePlanStore store;
    private final CurrentUser user;
    private final BusinessTime time;
    private final CareRiskStore risks;

    public CarePlanQueries(CarePlanStore store, CurrentUser user, BusinessTime time, CareRiskStore risks) {
        this.store = store;
        this.user = user;
        this.time = time;
        this.risks = risks;
    }

    @RequiresPermission(function = Function.CARE_PLAN, operation = Operation.VIEW)
    public CarePlan get(Target target, long id) {
        var row = store.require(id);
        if (caregiverOnly() && row.status() != CarePlanStatus.EFFECTIVE) throw new NotFoundException();
        return row;
    }

    @RequiresPermission(function = Function.CARE_PLAN, operation = Operation.VIEW)
    public List<CarePlan> list(Target target, long residentId) {
        return store.list(residentId, false).stream()
                .filter(p -> !caregiverOnly() || p.status() == CarePlanStatus.EFFECTIVE)
                .toList();
    }

    private boolean caregiverOnly() {
        return user.roles().contains(Role.CAREGIVER)
                && Collections.disjoint(
                        user.roles(),
                        Set.of(Role.NURSING_STAFF, Role.DOCTOR, Role.FLOOR_MANAGER, Role.NURSING_HOME_MANAGER));
    }

    public List<CarePlanItem> items(long id) {
        return store.items(id);
    }

    public List<CareRiskAgreement> risks(long id) {
        return risks.list(id);
    }

    @Override
    public Optional<CarePlanView> effectiveOn(long residentId, LocalDate date) {
        return store.list(residentId, false).stream()
                .filter(p -> (p.status() == CarePlanStatus.EFFECTIVE || p.status() == CarePlanStatus.EXPIRED)
                        && p.effectiveFrom() != null
                        && !date.isBefore(p.effectiveFrom())
                        && (p.effectiveTo() == null || !date.isAfter(p.effectiveTo())))
                .findFirst()
                .map(p -> new CarePlanView(
                        p.id(),
                        residentId,
                        p.revision(),
                        p.status(),
                        p.effectiveFrom(),
                        Optional.ofNullable(p.effectiveTo()),
                        store.items(p.id()).stream()
                                .map(i -> i.data().view(i.id()))
                                .toList()));
    }

    @Override
    public Optional<Integer> waterTargetMl(long residentId, LocalDate date) {
        return effectiveOn(residentId, date)
                .flatMap(plan -> plan.items().stream()
                        .map(CarePlanItemView::waterTargetMl)
                        .flatMap(Optional::stream)
                        .findFirst());
    }

    @Override
    public boolean hasEffectivePlan(long residentId) {
        return effectiveOn(residentId, time.today()).isPresent();
    }

    @Override
    public Set<Long> currentTaskTypes(long residentId) {
        var ids = new LinkedHashSet<Long>();
        effectiveOn(residentId, time.today())
                .ifPresent(plan -> plan.items().forEach(item -> ids.add(item.taskTypeId())));
        return Set.copyOf(ids);
    }
}
