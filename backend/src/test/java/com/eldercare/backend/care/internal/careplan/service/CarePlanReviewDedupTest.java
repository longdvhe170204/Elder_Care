package com.eldercare.backend.care.internal.careplan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.ApproverApi;
import com.eldercare.backend.administration.account.api.spi.BedAllocationApi;
import com.eldercare.backend.admission.api.events.AddendumApplied;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 005 FR-011 (BR-M04-20, Q-423; R1, R2): phụ lục đổi mức được áp dụng không báo xem xét kế hoạch lần nữa khi đã có phiên
 * bản kế hoạch lập sau lần đánh giá lại gần nhất; chưa có thì báo; phiên bản Đã hủy không tính.
 */
class CarePlanReviewDedupTest {

    private static final long RESIDENT = 11L;
    private static final Instant ASSESSED = Instant.parse("2030-03-01T03:00:00Z");

    private final List<Object> published = new ArrayList<>();
    private final AssessmentApi assessments = mock(AssessmentApi.class);
    private final CarePlanStore plans = mock(CarePlanStore.class);
    private final CarePlanReviewListener listener;

    CarePlanReviewDedupTest() {
        ApproverApi approvers = mock(ApproverApi.class);
        when(approvers.approvers(any(), any())).thenReturn(List.of());
        BedAllocationApi allocations = mock(BedAllocationApi.class);
        when(allocations.openAt(anyLong(), any())).thenReturn(Optional.empty());
        BusinessTime time = mock(BusinessTime.class);
        when(time.now()).thenReturn(Instant.parse("2030-03-09T03:00:00Z"));
        when(assessments.latestConfirmed(RESIDENT))
                .thenReturn(Optional.of(new AssessmentSummary(5L, RESIDENT, CareLevel.SPECIAL, Set.of(), ASSESSED)));
        listener = new CarePlanReviewListener(
                approvers,
                allocations,
                time,
                published::add,
                mock(ReminderMarkers.class),
                assessments,
                plans,
                mock(CarePlanReviewStore.class));
    }

    private static CarePlan plan(CarePlanStatus status, Instant createdAt) {
        return new CarePlan(
                1L,
                RESIDENT,
                2,
                status,
                LocalDate.of(2030, 3, 5),
                null,
                null,
                "Theo đánh giá lại",
                3L,
                createdAt,
                null,
                null,
                false,
                null,
                null,
                null,
                null,
                0);
    }

    private long reviews() {
        return published.stream().filter(CarePlanReviewNeeded.class::isInstance).count();
    }

    private void applyCareLevelAddendum() {
        listener.onAddendum(new AddendumApplied(7L, 8L, RESIDENT, Set.of("CARE_LEVEL"), LocalDate.of(2030, 3, 9)));
    }

    @Test
    void R2_planVersionAfterReassessment_noSecondReview() {
        when(plans.list(RESIDENT, false))
                .thenReturn(List.of(plan(CarePlanStatus.EFFECTIVE, ASSESSED.plusSeconds(3600))));
        applyCareLevelAddendum();
        assertThat(reviews()).isZero();
    }

    @Test
    void R2_noPlanVersionAfterReassessment_reviewed() {
        when(plans.list(RESIDENT, false))
                .thenReturn(List.of(plan(CarePlanStatus.EFFECTIVE, ASSESSED.minusSeconds(3600))));
        applyCareLevelAddendum();
        assertThat(reviews()).isPositive();
    }

    @Test
    void R2_cancelledVersionDoesNotCount() {
        when(plans.list(RESIDENT, false))
                .thenReturn(List.of(plan(CarePlanStatus.CANCELLED, ASSESSED.plusSeconds(3600))));
        applyCareLevelAddendum();
        assertThat(reviews()).isPositive();
    }
}
