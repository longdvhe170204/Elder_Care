package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.admission.api.dto.AttendanceScheduleView;
import com.eldercare.backend.care.api.dto.AttendanceStatus;
import com.eldercare.backend.nutrition.api.dto.TripOverlapView;
import com.eldercare.backend.nutrition.internal.closing.domain.CountedKind;
import com.eldercare.backend.nutrition.internal.closing.domain.CountedResidents;
import com.eldercare.backend.nutrition.internal.closing.domain.CountedResidents.Counted;
import com.eldercare.backend.nutrition.internal.closing.domain.CountedResidents.MealSlot;
import com.eldercare.backend.nutrition.internal.closing.domain.CountedResidents.ResidentFacts;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 011 T021 (FR-034, BR-M08-01, BR-M08-09; tên {@code NguoiDuocTinhTest} trong tasks): nội trú Đang lưu trú trừ chuyến trùng
 * giờ bữa; Tạm vắng, Hoạt động bên ngoài chỉ tính khi dự kiến trở lại không muộn hơn giờ bữa (dấu dự kiến trở về); bệnh
 * viện không tính; bán trú có lịch bao trùm giờ bữa, Chưa đến hoặc Có mặt, cộng buổi phát sinh Có mặt; người thân ở lại có
 * đăng ký ăn bao trùm giờ bữa, theo tầng của người cao tuổi.
 */
class CountedResidentsTest {

    private static final LocalDate DAY = LocalDate.of(2025, 11, 12);
    private static final Instant LUNCH = Instant.parse("2025-11-12T04:00:00Z");
    private static final MealSlot MEAL = new MealSlot(LUNCH, LocalTime.of(11, 0), true, true);
    private static final long FLOOR_2 = 2;
    private static final long DAY_CARE_AREA = 9;

    private static ResidentFacts residential(long id, ResidentStatus status) {
        return new ResidentFacts(id, status, false, false, null, List.of(), List.of(), null, false, FLOOR_2);
    }

    @Test
    void FR_034_inResidence_isCounted_hospitalized_isNot() {
        List<Counted> counted = CountedResidents.count(
                MEAL,
                List.of(residential(1, ResidentStatus.IN_RESIDENCE), residential(2, ResidentStatus.HOSPITALIZED)),
                List.of(),
                Map.of());

        assertThat(counted).containsExactly(new Counted(CountedKind.RESIDENTIAL, 1, null, null, FLOOR_2, false));
    }

    @Test
    void FR_034_tripOverlappingMeal_excludes_unlessExpectedBackBeforeMeal() {
        TripOverlapView late = new TripOverlapView(7, Set.of(1L), LUNCH.minusSeconds(3600), LUNCH.plusSeconds(60));
        TripOverlapView early = new TripOverlapView(8, Set.of(2L), LUNCH.minusSeconds(7200), LUNCH.minusSeconds(60));
        ResidentFacts a = new ResidentFacts(
                1, ResidentStatus.IN_RESIDENCE, false, false, null, List.of(late), List.of(), null, false, FLOOR_2);
        ResidentFacts b = new ResidentFacts(
                2,
                ResidentStatus.OFF_SITE_ACTIVITY,
                false,
                false,
                null,
                List.of(early),
                List.of(),
                null,
                false,
                FLOOR_2);

        assertThat(CountedResidents.count(MEAL, List.of(a, b), List.of(), Map.of()))
                .containsExactly(new Counted(CountedKind.RESIDENTIAL, 2, null, null, FLOOR_2, true));
    }

    @Test
    void FR_006_mealNotServingDayCare_skipsDayCare_mealNotServingResidential_skipsResidential() {
        AttendanceScheduleView covers = new AttendanceScheduleView(DAY, LocalTime.of(7, 30), LocalTime.of(16, 0));
        List<ResidentFacts> people =
                List.of(residential(1, ResidentStatus.IN_RESIDENCE), dayCare(2, List.of(covers), null, false));

        assertThat(CountedResidents.count(
                        new MealSlot(LUNCH, LocalTime.of(11, 0), true, false), people, List.of(), Map.of()))
                .extracting(Counted::residentId)
                .containsExactly(1L);
        assertThat(CountedResidents.count(
                        new MealSlot(LUNCH, LocalTime.of(11, 0), false, true), people, List.of(), Map.of()))
                .extracting(Counted::residentId)
                .containsExactly(2L);
    }

    private static ResidentFacts dayCare(
            long id, List<AttendanceScheduleView> schedules, AttendanceStatus status, boolean adHocPresent) {
        return new ResidentFacts(
                id,
                ResidentStatus.IN_RESIDENCE,
                true,
                false,
                null,
                List.of(),
                schedules,
                status,
                adHocPresent,
                DAY_CARE_AREA);
    }
}
