package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.CareLevelBasisType;
import com.eldercare.backend.resident.api.events.CareLevelChanged;
import com.eldercare.backend.resident.api.events.InitialAssessmentConfirmed;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import com.eldercare.backend.roombedallocation.api.events.AllocationNoLongerSuitable;
import com.eldercare.backend.roombedallocation.internal.allocation.domain.AllocationDetails;
import com.eldercare.backend.roombedallocation.internal.allocation.domain.AllocationStatus;
import com.eldercare.backend.roombedallocation.internal.allocation.persistence.AllocationCommandStore;
import com.eldercare.backend.roombedallocation.internal.allocation.persistence.AllocationReadStore;
import com.eldercare.backend.roombedallocation.internal.allocation.service.AllocationChecker;
import com.eldercare.backend.roombedallocation.internal.allocation.service.AllocationLocks;
import com.eldercare.backend.roombedallocation.internal.allocation.service.AllocationSuitabilityListener;
import com.eldercare.backend.roombedallocation.internal.allocation.service.AllocationSuitabilityService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 003 T031 (BR-M03-23, FR-034; Edge Cases "Giới tính hoặc mức chăm sóc đổi khi người đó có phân bổ tương lai", Q-301):
 * mức chăm sóc, đánh giá đầu vào hoặc giới tính đổi thì phân bổ Đang hiệu lực và Tương lai được kiểm lại theo dữ liệu
 * hiện hành; không còn thỏa thì mang dấu "phòng không còn phù hợp" (không hủy) và báo Hành chính, Trưởng tầng một lần; thỏa
 * lại thì gỡ dấu; phân bổ đã kết thúc không xét. Đổi trường khác giới tính không kiểm lại.
 */
class BR_M03_23_AllocationSuitabilityTest {

    private static final long RESIDENT = 7;
    private static final UnmetCondition CARE_LEVEL =
            new UnmetCondition("CARE_LEVEL", "Mức chăm sóc không được phòng cho phép");

    private final AllocationReadStore read = mock(AllocationReadStore.class);
    private final AllocationCommandStore write = mock(AllocationCommandStore.class);
    private final AllocationChecker checker = mock(AllocationChecker.class);
    private final BusinessTime time = mock(BusinessTime.class);
    private final List<Object> published = new ArrayList<>();
    private final ApplicationEventPublisher events = published::add;
    private final AllocationSuitabilityService service =
            new AllocationSuitabilityService(read, write, mock(AllocationLocks.class), checker, time, events);
    private final AllocationSuitabilityListener listener = new AllocationSuitabilityListener(service);

    private static AllocationDetails allocation(long id, long bed, AllocationStatus status) {
        AllocationDetails row = mock(AllocationDetails.class);
        when(row.id()).thenReturn(id);
        when(row.status()).thenReturn(status);
        when(row.location()).thenReturn(new LocationView(null, null, 30L, "Tầng 3", 300L, "P301", bed, "G" + bed));
        return row;
    }

    private void given(AllocationDetails... rows) {
        when(time.now()).thenReturn(Instant.parse("2026-11-11T03:00:00Z"));
        when(read.list(RESIDENT, null, null)).thenReturn(List.of(rows));
        when(write.markUnsuitable(anyLong(), anyBoolean())).thenReturn(true);
    }

    @Test
    void BR_M03_23_careLevelChange_futureAllocationFlaggedNotCancelled_notifiedOnce() {
        given(allocation(1, 11, AllocationStatus.FUTURE), allocation(2, 12, AllocationStatus.CLOSED));
        when(checker.policyConditions(RESIDENT, 11)).thenReturn(List.of(CARE_LEVEL));

        listener.careLevelChanged(new CareLevelChanged(
                RESIDENT,
                Optional.of(CareLevel.BASIC),
                CareLevel.SPECIAL,
                Instant.parse("2026-11-11T02:00:00Z"),
                CareLevelBasisType.ASSESSMENT,
                5));

        verify(write).markUnsuitable(1, true);
        verify(write, never()).markUnsuitable(2, true);
        verify(checker, never()).policyConditions(RESIDENT, 12);
        assertThat(published).singleElement().isInstanceOfSatisfying(AllocationNoLongerSuitable.class, e -> {
            assertThat(e.allocationId()).isEqualTo(1);
            assertThat(e.conditions()).containsExactly(CARE_LEVEL);
        });
    }

    @Test
    void Q_301_initialAssessmentSuitableAgain_clearsFlag_noNotification() {
        given(allocation(4, 14, AllocationStatus.FUTURE));
        when(checker.policyConditions(RESIDENT, 14)).thenReturn(List.of());

        listener.initialAssessmentConfirmed(new InitialAssessmentConfirmed(RESIDENT, 9, CareLevel.REGULAR));

        verify(write).markUnsuitable(4, false);
        assertThat(published).isEmpty();
    }

    @Test
    void BR_M03_23_alreadyFlagged_notNotifiedAgain() {
        given(allocation(5, 15, AllocationStatus.ACTIVE));
        when(write.markUnsuitable(5, true)).thenReturn(false);
        when(checker.policyConditions(RESIDENT, 15)).thenReturn(List.of(CARE_LEVEL));

        service.reassess(RESIDENT);

        assertThat(published).isEmpty();
    }
}
