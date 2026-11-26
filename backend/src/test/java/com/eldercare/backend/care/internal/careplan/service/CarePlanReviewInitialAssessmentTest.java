package com.eldercare.backend.care.internal.careplan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.ApproverApi;
import com.eldercare.backend.administration.account.api.spi.BedAllocationApi;
import com.eldercare.backend.care.api.dto.CarePlanStatus;
import com.eldercare.backend.care.api.events.CarePlanReviewNeeded;
import com.eldercare.backend.care.internal.careplan.domain.CarePlan;
import com.eldercare.backend.care.internal.careplan.persistence.CarePlanReviewStore;
import com.eldercare.backend.care.internal.careplan.persistence.CarePlanStore;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.AssessmentApi;
import com.eldercare.backend.resident.api.dto.AssessmentSummary;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.CareNeedView;
import com.eldercare.backend.resident.api.events.InitialAssessmentConfirmed;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Q-448 (FR-011, FR-058, US8 kịch bản 5): đánh giá đầu vào được xác nhận có ít nhất một nhu cầu chăm sóc mà người cao tuổi
 * chưa có kế hoạch Hiệu lực hay Chờ hiệu lực thì báo và ghi mốc yêu cầu xem xét; không có nhu cầu thì người cao tuổi theo
 * lịch chung, không báo.
 */
class CarePlanReviewInitialAssessmentTest {

    private static final long RESIDENT = 21L;
    private static final long ASSESSMENT = 31L;
    private static final Instant NOW = Instant.parse("2030-03-09T03:00:00Z");

    private final List<Object> published = new ArrayList<>();
    private final AssessmentApi assessments = mock(AssessmentApi.class);
    private final CarePlanStore plans = mock(CarePlanStore.class);
    private final CarePlanReviewStore reviews = mock(CarePlanReviewStore.class);
    private final CarePlanReviewListener listener;

    CarePlanReviewInitialAssessmentTest() {
        ApproverApi approvers = mock(ApproverApi.class);
        when(approvers.approvers(any(), any())).thenReturn(List.of());
        BedAllocationApi allocations = mock(BedAllocationApi.class);
        when(allocations.openAt(anyLong(), any())).thenReturn(Optional.empty());
        BusinessTime time = mock(BusinessTime.class);
        when(time.now()).thenReturn(NOW);
        when(plans.list(RESIDENT, false)).thenReturn(List.of());
        listener = new CarePlanReviewListener(
                approvers, allocations, time, published::add, mock(ReminderMarkers.class), assessments, plans, reviews);
    }

    private void confirmed(long assessmentId, Set<CareNeedView> needs) {
        when(assessments.latestConfirmed(RESIDENT))
                .thenReturn(Optional.of(new AssessmentSummary(assessmentId, RESIDENT, CareLevel.BASIC, needs, NOW)));
    }

    private void confirm() {
        listener.onInitialAssessment(new InitialAssessmentConfirmed(RESIDENT, ASSESSMENT, CareLevel.BASIC));
    }

    private long notices() {
        return published.stream().filter(CarePlanReviewNeeded.class::isInstance).count();
    }

    private static CarePlan plan(CarePlanStatus status) {
        return new CarePlan(
                1L,
                RESIDENT,
                1,
                status,
                LocalDate.of(2030, 3, 1),
                null,
                null,
                null,
                3L,
                NOW,
                null,
                null,
                false,
                null,
                null,
                null,
                null,
                0);
    }

    @Test
    void Q448_initialAssessmentWithNeedsOpensReview() {
        confirmed(ASSESSMENT, Set.of(new CareNeedView(1L, "Hỗ trợ đi lại")));

        confirm();

        verify(reviews).raise(RESIDENT, "M01.INITIAL_ASSESSMENT", ASSESSMENT, Long.toString(ASSESSMENT), NOW);
        assertThat(notices()).isPositive();
    }

    @Test
    void Q448_initialAssessmentWithoutNeedsStaysOnGeneralSchedule() {
        confirmed(ASSESSMENT, Set.of());

        confirm();

        verify(reviews, never()).raise(anyLong(), anyString(), anyLong(), anyString(), any());
        assertThat(notices()).isZero();
    }

    @Test
    void Q448_residentWithEffectiveOrScheduledPlanIsNotReviewedAgain() {
        confirmed(ASSESSMENT, Set.of(new CareNeedView(1L, "Hỗ trợ đi lại")));
        for (CarePlanStatus status : List.of(CarePlanStatus.EFFECTIVE, CarePlanStatus.PENDING_EFFECT)) {
            when(plans.list(RESIDENT, false)).thenReturn(List.of(plan(status)));

            confirm();
        }

        verify(reviews, never()).raise(anyLong(), anyString(), anyLong(), anyString(), any());
        assertThat(notices()).isZero();
    }

    @Test
    void Q448_latestConfirmedIsAnotherAssessment_noReview() {
        confirmed(ASSESSMENT + 1, Set.of(new CareNeedView(1L, "Hỗ trợ đi lại")));

        confirm();

        verify(reviews, never()).raise(eq(RESIDENT), anyString(), anyLong(), anyString(), any());
        assertThat(notices()).isZero();
    }
}
