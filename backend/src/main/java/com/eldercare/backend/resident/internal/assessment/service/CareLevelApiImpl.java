package com.eldercare.backend.resident.internal.assessment.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.CareLevelApi;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.CareLevelAtDate;
import com.eldercare.backend.resident.api.dto.CareLevelBasisType;
import com.eldercare.backend.resident.api.dto.CareNeedView;
import com.eldercare.backend.resident.api.events.CareLevelChanged;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentStatus;
import com.eldercare.backend.resident.internal.assessment.persistence.AssessmentRepository;
import com.eldercare.backend.resident.internal.assessment.persistence.CareLevelHistoryStore;
import com.eldercare.backend.resident.internal.health.persistence.CareNeedRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link CareLevelApi} cho 003, 004, 008, 010, 016 (FR-052, FR-053): mức hiện hành lấy từ {@code resident}, tra theo ngày
 * lấy từ lịch sử. Mức đổi theo yêu cầu thay đổi lưu trú chỉ qua {@link #applyChange}, không có endpoint sửa trực tiếp.
 */
@Service
public class CareLevelApiImpl implements CareLevelApi {

    private final CareLevelHistoryStore history;
    private final AssessmentRepository assessments;
    private final CareNeedRepository careNeeds;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;
    private final ObjectProvider<CurrentUser> currentUser;

    CareLevelApiImpl(
            CareLevelHistoryStore history,
            AssessmentRepository assessments,
            CareNeedRepository careNeeds,
            BusinessTime time,
            ApplicationEventPublisher events,
            ObjectProvider<CurrentUser> currentUser) {
        this.history = history;
        this.assessments = assessments;
        this.careNeeds = careNeeds;
        this.time = time;
        this.events = events;
        this.currentUser = currentUser;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CareLevel> current(long residentId) {
        return history.current(residentId);
    }

    /** Dòng lịch sử có hiệu lực từ lớn nhất mà không sau cuối ngày đó (giờ Việt Nam; FR-053). */
    @Override
    @Transactional(readOnly = true)
    public Optional<CareLevelAtDate> atDate(long residentId, LocalDate date) {
        return history.at(residentId, time.startOfDay(date.plusDays(1)).minusNanos(1_000));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<CareNeedView> currentCareNeeds(long residentId) {
        return assessments
                .findFirstByResidentIdAndStatusOrderByConfirmedAtDescIdDesc(residentId, AssessmentStatus.CONFIRMED)
                .map(a -> careNeeds.findAllById(a.getCareNeedIds()).stream()
                        .map(n -> new CareNeedView(n.getId(), n.getName()))
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void applyChange(long residentId, CareLevel level, LocalDate effectiveDate, long approvalRequestId) {
        Optional<CareLevel> previous = history.current(residentId);
        Instant effectiveFrom = time.startOfDay(effectiveDate);
        history.apply(
                residentId,
                level,
                effectiveFrom,
                CareLevelBasisType.RESIDENCY_CHANGE_REQUEST,
                approvalRequestId,
                time.now(),
                currentUser.getObject().accountId());
        events.publishEvent(new CareLevelChanged(
                residentId,
                previous,
                level,
                effectiveFrom,
                CareLevelBasisType.RESIDENCY_CHANGE_REQUEST,
                approvalRequestId));
    }
}
