package com.eldercare.backend.nutrition.internal.closing.domain;

import com.eldercare.backend.admission.api.dto.AttendanceScheduleView;
import com.eldercare.backend.care.api.dto.AttendanceStatus;
import com.eldercare.backend.nutrition.api.dto.StayMealView;
import com.eldercare.backend.nutrition.api.dto.TripOverlapView;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Hàm thuần danh sách người được tính suất của một bữa (FR-034, BR-M08-01, BR-M08-09; research M-08; tên
 * {@code NguoiDuocTinh} trong tasks) với dữ liệu tại thời điểm tính.
 */
public final class CountedResidents {

    /** Bữa trong ngày: giờ dự kiến (thời điểm và giờ địa phương), loại lưu trú được phục vụ (FR-006). */
    public record MealSlot(Instant mealTime, LocalTime localTime, boolean servesResidential, boolean servesDayCare) {}

    /**
     * Dữ liệu của một người cao tuổi: trạng thái (001), bán trú tại ngày (004), lượt vắng đang mở và mốc dự kiến trở lại
     * (004), chuyến trùng giờ bữa có người này (014), lịch đến (004), trạng thái có mặt của buổi theo lịch và buổi phát sinh
     * Có mặt (005), tầng/khu (003; bán trú là khu bán trú).
     */
    public record ResidentFacts(
            long residentId,
            ResidentStatus status,
            boolean dayCare,
            boolean absenceOpen,
            Instant absenceExpectedReturn,
            List<TripOverlapView> trips,
            List<AttendanceScheduleView> schedules,
            AttendanceStatus attendance,
            boolean adHocPresent,
            Long floorAreaId) {}

    /** Người được tính: loại, người cao tuổi, người thân và lượt ở lại (người thân), tầng/khu, dấu dự kiến trở về. */
    public record Counted(
            CountedKind kind,
            long residentId,
            Long familyMemberId,
            Long stayId,
            long floorAreaId,
            boolean expectedReturn) {}

    private CountedResidents() {}

    /** {@code residentFloors}: tầng/khu của người cao tuổi được gắn với lượt ở lại (FR-034 c). */
    public static List<Counted> count(
            MealSlot meal, List<ResidentFacts> residents, List<StayMealView> stays, Map<Long, Long> residentFloors) {
        List<Counted> out = new ArrayList<>();
        for (ResidentFacts r : residents) {
            if (r.floorAreaId() == null) {
                continue;
            }
            if (r.dayCare()) {
                if (meal.servesDayCare() && dayCareCounted(meal, r)) {
                    out.add(new Counted(CountedKind.DAY_CARE, r.residentId(), null, null, r.floorAreaId(), false));
                }
            } else if (meal.servesResidential()) {
                residential(meal, r)
                        .ifPresent(expected -> out.add(new Counted(
                                CountedKind.RESIDENTIAL, r.residentId(), null, null, r.floorAreaId(), expected)));
            }
        }
        for (StayMealView stay : stays) {
            Long floor = residentFloors.get(stay.residentId());
            if (stay.mealRegistered() && covers(stay.from(), stay.to(), meal.mealTime()) && floor != null) {
                out.add(new Counted(
                        CountedKind.FAMILY, stay.residentId(), stay.familyMemberId(), stay.stayId(), floor, false));
            }
        }
        return out;
    }

    /** Có được tính không, và dấu dự kiến trở về (FR-034 a). */
    private static Optional<Boolean> residential(MealSlot meal, ResidentFacts r) {
        Optional<TripOverlapView> trip = r.trips().stream()
                .filter(t -> t.residentIds().contains(r.residentId()))
                .findFirst();
        return switch (r.status()) {
            case IN_RESIDENCE ->
                trip.isEmpty() ? Optional.of(false) : backBy(trip.get().expectedReturn(), meal);
            case TEMPORARILY_ABSENT -> r.absenceOpen() ? backBy(r.absenceExpectedReturn(), meal) : Optional.empty();
            case OFF_SITE_ACTIVITY -> trip.flatMap(t -> backBy(t.expectedReturn(), meal));
            default -> Optional.empty();
        };
    }

    private static Optional<Boolean> backBy(Instant expectedReturn, MealSlot meal) {
        return expectedReturn != null && !expectedReturn.isAfter(meal.mealTime())
                ? Optional.of(true)
                : Optional.empty();
    }

    /** Lịch đến bao trùm giờ bữa, Chưa đến hoặc Có mặt; hoặc buổi phát sinh Có mặt (FR-034 b, Q-140). */
    private static boolean dayCareCounted(MealSlot meal, ResidentFacts r) {
        if (r.status() != ResidentStatus.IN_RESIDENCE) {
            return false;
        }
        if (r.adHocPresent()) {
            return true;
        }
        boolean scheduled = r.schedules().stream()
                .anyMatch(s -> !s.arrivalTime().isAfter(meal.localTime())
                        && s.departureTime().isAfter(meal.localTime()));
        return scheduled
                && (r.attendance() == null
                        || r.attendance() == AttendanceStatus.NOT_ARRIVED
                        || r.attendance() == AttendanceStatus.PRESENT);
    }

    private static boolean covers(Instant from, Instant to, Instant at) {
        return from != null && !from.isAfter(at) && (to == null || to.isAfter(at));
    }
}
