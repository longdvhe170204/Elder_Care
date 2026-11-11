package com.eldercare.backend.care.internal.trip.service;

import com.eldercare.backend.care.api.*;
import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.internal.trip.persistence.ActivityTripStore;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Nguồn thật cho adapter 006/008; dùng trạng thái tham gia thay trạng thái đăng ký chung. */
@Service
@Transactional(readOnly = true)
public class TripReadSources implements TripMedicationReadApi, TripDutyReadApi, ResidentTripReadApi, TripMealReadApi {
    private final ActivityTripStore trips;
    private final TripPreparationService preparation;

    public TripReadSources(ActivityTripStore trips, TripPreparationService preparation) {
        this.trips = trips;
        this.preparation = preparation;
    }

    @Override
    public Optional<TripInterval> approvedTripInterval(long resident, long id) {
        if (trips.participants(id).stream()
                .noneMatch(p -> p.residentId() == resident
                        && Set.of("ELIGIBLE", "DEPARTED").contains(p.status()))) return Optional.empty();
        return tripInterval(id);
    }

    @Override
    public Optional<ResidentOngoingTripView> ongoing(long residentId) {
        return trips.active().stream()
                .filter(t -> "IN_PROGRESS".equals(t.status()))
                .filter(t -> trips.participants(t.id()).stream()
                        .anyMatch(p -> p.residentId() == residentId
                                && Set.of("DEPARTED", "MISSING_ON_RETURN").contains(p.status())))
                .findFirst()
                .map(t -> {
                    var duties = preparation.duties(t.id());
                    return new ResidentOngoingTripView(
                            t.id(), Optional.ofNullable(duties.leaderId()), duties.companionIds());
                });
    }

    @Override
    public List<TripMealOverlapView> overlappingMealTime(Instant mealTime) {
        return trips.active().stream()
                .filter(t -> !t.plan().plannedDepartureAt().isAfter(mealTime)
                        && t.plan().plannedReturnAt().isAfter(mealTime))
                .map(t -> new TripMealOverlapView(
                        t.id(),
                        trips.participants(t.id()).stream()
                                .filter(p -> Set.of("ELIGIBLE", "DEPARTED").contains(p.status()))
                                .map(p -> p.residentId())
                                .toList(),
                        t.plan().plannedDepartureAt(),
                        t.plan().plannedReturnAt()))
                .toList();
    }

    @Override
    public Optional<TripInterval> tripInterval(long id) {
        return trips.find(id)
                .filter(t -> !"CANCELLED".equals(t.status()))
                .map(t ->
                        new TripInterval(t.plan().plannedDepartureAt(), t.plan().plannedReturnAt()));
    }

    @Override
    public Optional<Long> groupLeader(long id) {
        return Optional.ofNullable(preparation.duties(id).leaderId());
    }

    @Override
    public List<Long> companions(long id) {
        return preparation.duties(id).companionIds();
    }

    @Override
    public List<TripInShiftView> inShift(long floor, Instant from, Instant to) {
        return trips.active().stream()
                .filter(t -> Objects.equals(t.floorAreaId(), floor)
                        && "IN_PROGRESS".equals(t.status())
                        && trips.firstDeparture(t.id())
                                .orElse(t.plan().plannedDepartureAt())
                                .isBefore(to))
                .map(t -> {
                    var d = preparation.duties(t.id());
                    var staff = new ArrayList<>(d.companionIds());
                    if (d.leaderId() != null) staff.addFirst(d.leaderId());
                    var residents = trips.participants(t.id()).stream()
                            .filter(p -> Set.of("DEPARTED", "MISSING_ON_RETURN").contains(p.status()))
                            .map(p -> p.residentId())
                            .toList();
                    return new TripInShiftView(
                            t.id(),
                            staff,
                            t.plan().plannedReturnAt(),
                            residents,
                            trips.firstDeparture(t.id()).orElse(null));
                })
                .toList();
    }
}
