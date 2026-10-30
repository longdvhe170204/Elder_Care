package com.eldercare.backend.support.fake.roombedallocation;

import com.eldercare.backend.roombedallocation.api.spi.AllocationResidencySource;
import com.eldercare.backend.roombedallocation.api.spi.CareLevelCode;
import com.eldercare.backend.roombedallocation.api.spi.HeldBed;
import com.eldercare.backend.roombedallocation.api.spi.ResidencyType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Bản giả nguồn 004, T002: dữ liệu và kết quả lệnh chỉ nằm trong bộ nhớ của test. */
public class FakeAllocationResidencySource implements AllocationResidencySource {
    private record Resident(
            ResidencyType type,
            CareLevelCode contractLevel,
            CareLevelCode registrationLevel,
            Instant end,
            HeldBed heldBed) {}

    private record PriceKey(long residentId, long previousRoomTypeId, long nextRoomTypeId, LocalDate date) {}

    private final Map<Long, Resident> residents = new HashMap<>();
    private final Set<PriceKey> differentPrices = new HashSet<>();
    private final List<Long> expiredWaitingRecords = new ArrayList<>();
    private final Map<Long, Long> allocations = new HashMap<>();

    public FakeAllocationResidencySource resident(
            long residentId,
            ResidencyType type,
            CareLevelCode contractLevel,
            CareLevelCode registrationLevel,
            Instant end,
            HeldBed heldBed) {
        residents.put(residentId, new Resident(type, contractLevel, registrationLevel, end, heldBed));
        return this;
    }

    public FakeAllocationResidencySource priceDifference(
            long residentId, long previousRoomTypeId, long nextRoomTypeId, LocalDate date, boolean different) {
        PriceKey key = new PriceKey(residentId, previousRoomTypeId, nextRoomTypeId, date);
        if (different) {
            differentPrices.add(key);
        } else {
            differentPrices.remove(key);
        }
        return this;
    }

    @Override
    public Optional<ResidencyType> residencyType(long residentId) {
        return Optional.ofNullable(residents.get(residentId)).map(Resident::type);
    }

    @Override
    public Optional<CareLevelCode> careLevelFromContract(long residentId) {
        return Optional.ofNullable(residents.get(residentId)).map(Resident::contractLevel);
    }

    @Override
    public Optional<CareLevelCode> careLevelFromRegistration(long residentId) {
        return Optional.ofNullable(residents.get(residentId)).map(Resident::registrationLevel);
    }

    @Override
    public Optional<Instant> expectedEnd(long residentId) {
        return Optional.ofNullable(residents.get(residentId)).map(Resident::end);
    }

    @Override
    public HeldBed holdBedWhenAbsent(long residentId) {
        return Optional.ofNullable(residents.get(residentId))
                .map(Resident::heldBed)
                .orElse(new HeldBed(false, Optional.empty()));
    }

    @Override
    public boolean differentUnitPrice(long residentId, long previousRoomTypeId, long nextRoomTypeId, LocalDate date) {
        return differentPrices.contains(new PriceKey(residentId, previousRoomTypeId, nextRoomTypeId, date));
    }

    @Override
    public void waitingRecordHoldExpired(long waitingRecordId) {
        expiredWaitingRecords.add(waitingRecordId);
    }

    @Override
    public void allocated(long residentId, long allocationId) {
        allocations.put(residentId, allocationId);
    }

    public List<Long> expiredWaitingRecords() {
        return List.copyOf(expiredWaitingRecords);
    }

    public Map<Long, Long> allocations() {
        return Map.copyOf(allocations);
    }

    public void reset() {
        residents.clear();
        differentPrices.clear();
        expiredWaitingRecords.clear();
        allocations.clear();
    }
}
