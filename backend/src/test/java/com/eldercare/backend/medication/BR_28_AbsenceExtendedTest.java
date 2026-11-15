package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.admission.api.events.AbsenceExtended;
import com.eldercare.backend.medication.internal.handover.domain.AbsenceIntervalType;
import com.eldercare.backend.medication.internal.handover.domain.MedicationAbsenceInterval;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationAbsenceIntervalRepository;
import com.eldercare.backend.medication.internal.status.service.AbsenceExtensionListener;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * BR-M07-03, FR-021 (research D-15): gia hạn lượt vắng của 004 ({@code AbsenceExtended}) dời giờ về dự kiến của khoảng
 * vắng đang mở, để liều trong khoảng gia hạn vẫn tính Tạm dừng hoặc Mang theo. Khoảng vắng của chuyến đi (014) không
 * phải lượt vắng của 004 nên giữ nguyên.
 */
class BR_28_AbsenceExtendedTest {

    private static final long RESIDENT = 9L;
    private static final Instant FROM = Instant.parse("2026-11-08T02:00:00Z");
    private static final Instant EXPECTED = Instant.parse("2026-11-08T08:00:00Z");
    private static final Instant EXTENDED = Instant.parse("2026-11-10T08:00:00Z");

    @Test
    void BR_28_extendingAbsenceMovesExpectedReturnOfOpenLeaveInterval() {
        var repo = mock(MedicationAbsenceIntervalRepository.class);
        var open = new MedicationAbsenceInterval(
                RESIDENT, AbsenceIntervalType.TEMPORARY_LEAVE_WITH_MED, FROM, EXPECTED, null, null, FROM);
        when(repo.findOpenByResidentId(RESIDENT)).thenReturn(Optional.of(open));

        new AbsenceExtensionListener(repo).on(new AbsenceExtended(51L, RESIDENT, EXTENDED));

        assertThat(open.getExpectedToTime()).isEqualTo(EXTENDED);
        assertThat(open.isOpen()).isTrue();
        verify(repo).save(open);
    }

    @Test
    void BR_28_tripIntervalIsNotAnAdmissionAbsence() {
        var repo = mock(MedicationAbsenceIntervalRepository.class);
        var trip = new MedicationAbsenceInterval(
                RESIDENT, AbsenceIntervalType.TRIP_WITH_MED, FROM, EXPECTED, null, 77L, FROM);
        when(repo.findOpenByResidentId(RESIDENT)).thenReturn(Optional.of(trip));

        new AbsenceExtensionListener(repo).on(new AbsenceExtended(51L, RESIDENT, EXTENDED));

        assertThat(trip.getExpectedToTime()).isEqualTo(EXPECTED);
        verify(repo, never()).save(any());
    }
}
