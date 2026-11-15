package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.medication.internal.handover.domain.AbsenceIntervalType;
import com.eldercare.backend.medication.internal.handover.domain.MedicationAbsenceInterval;
import com.eldercare.backend.medication.internal.schedule.service.AbsenceScheduleContexts;
import com.eldercare.backend.medication.internal.schedule.service.AbsenceType;
import com.eldercare.backend.medication.internal.schedule.service.ResidentAbsenceInterval;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * BR-M07-03, FR-021, FR-048b (research D-06, D-15): khoảng vắng đã lưu là đầu vào lịch thuốc. Tạm vắng không mang
 * thuốc, bệnh viện, chuyến đi có lệnh "Không mang thuốc" tính Tạm dừng; Tạm vắng hoặc chuyến đi có mang thuốc tính Mang
 * theo; đã về thì khoảng dừng ở giờ về thực tế.
 */
class BR_28_AbsenceScheduleContextTest {

    private static final Instant FROM = Instant.parse("2026-11-08T02:00:00Z");
    private static final Instant EXPECTED = Instant.parse("2026-11-08T08:00:00Z");
    private static final Instant RETURNED = Instant.parse("2026-11-08T05:00:00Z");

    @Test
    void BR_28_absenceTypesMapToSuspendedOrCarriedAndUseActualReturn() {
        Map<AbsenceIntervalType, AbsenceType> expected = Map.of(
                AbsenceIntervalType.TEMPORARY_LEAVE_NO_MED, AbsenceType.TEMPORARY_LEAVE,
                AbsenceIntervalType.HOSPITAL, AbsenceType.HOSPITALIZATION,
                AbsenceIntervalType.TRIP_NO_MED, AbsenceType.TEMPORARY_LEAVE,
                AbsenceIntervalType.TEMPORARY_LEAVE_WITH_MED, AbsenceType.TRIP_WITH_MEDICATION,
                AbsenceIntervalType.TRIP_WITH_MED, AbsenceType.TRIP_WITH_MEDICATION);
        expected.forEach((stored, computed) -> {
            MedicationAbsenceInterval open =
                    new MedicationAbsenceInterval(9L, stored, FROM, EXPECTED, null, null, FROM);
            assertThat(AbsenceScheduleContexts.toIntervals(List.of(open)))
                    .as(stored.name())
                    .containsExactly(new ResidentAbsenceInterval(9L, FROM, EXPECTED, computed));
        });

        MedicationAbsenceInterval returned = new MedicationAbsenceInterval(
                9L, AbsenceIntervalType.TEMPORARY_LEAVE_NO_MED, FROM, EXPECTED, null, null, FROM);
        returned.close(RETURNED);
        assertThat(AbsenceScheduleContexts.toIntervals(List.of(returned)))
                .containsExactly(new ResidentAbsenceInterval(9L, FROM, RETURNED, AbsenceType.TEMPORARY_LEAVE));
    }
}
