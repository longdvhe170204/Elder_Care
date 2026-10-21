package com.cambae.backend.support.fake.m03;

import com.cambae.backend.m03.api.dto.CareLevel;
import com.cambae.backend.m03.api.dto.ResidencyType;
import com.cambae.backend.m03.api.spi.AllocationResidencySource;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Bản giả nguồn lưu trú do 004 cài; dữ liệu do test đặt, không tự suy diễn hợp đồng (003 T002). */
public class FakeAllocationResidencies implements AllocationResidencySource {

    private final Map<Long, ResidencyType> residencyTypes = new HashMap<>();
    private final Map<Long, CareLevel> contractLevels = new HashMap<>();
    private final Map<Long, CareLevel> registrationLevels = new HashMap<>();
    private final Map<Long, Instant> expectedEnds = new HashMap<>();
    private final Map<Long, BedRetention> retentions = new HashMap<>();
    private final Map<PriceKey, Boolean> priceDifferences = new HashMap<>();
    private final List<Long> expiredWaitingFiles = new ArrayList<>();
    private final List<Allocation> allocations = new ArrayList<>();

    public FakeAllocationResidencies residencyType(long residentId, ResidencyType type) {
        residencyTypes.put(residentId, type);
        return this;
    }

    public FakeAllocationResidencies contractCareLevel(long residentId, CareLevel level) {
        contractLevels.put(residentId, level);
        return this;
    }

    public FakeAllocationResidencies registrationCareLevel(long residentId, CareLevel level) {
        registrationLevels.put(residentId, level);
        return this;
    }

    public FakeAllocationResidencies expectedEnd(long residentId, Instant at) {
        expectedEnds.put(residentId, at);
        return this;
    }

    public FakeAllocationResidencies bedRetention(long residentId, BedRetention retention) {
        retentions.put(residentId, retention);
        return this;
    }

    public FakeAllocationResidencies differentUnitPrice(
            long residentId, long oldRoomTypeId, long newRoomTypeId, LocalDate date, boolean different) {
        priceDifferences.put(new PriceKey(residentId, oldRoomTypeId, newRoomTypeId, date), different);
        return this;
    }

    public List<Long> expiredWaitingFiles() {
        return List.copyOf(expiredWaitingFiles);
    }

    public List<Allocation> allocations() {
        return List.copyOf(allocations);
    }

    public void reset() {
        residencyTypes.clear();
        contractLevels.clear();
        registrationLevels.clear();
        expectedEnds.clear();
        retentions.clear();
        priceDifferences.clear();
        expiredWaitingFiles.clear();
        allocations.clear();
    }

    @Override
    public Optional<ResidencyType> residencyType(long residentId) {
        return Optional.ofNullable(residencyTypes.get(residentId));
    }

    @Override
    public Optional<CareLevel> contractCareLevel(long residentId) {
        return Optional.ofNullable(contractLevels.get(residentId));
    }

    @Override
    public Optional<CareLevel> registrationCareLevel(long residentId) {
        return Optional.ofNullable(registrationLevels.get(residentId));
    }

    @Override
    public Optional<Instant> expectedEnd(long residentId) {
        return Optional.ofNullable(expectedEnds.get(residentId));
    }

    @Override
    public BedRetention bedRetentionWhenAbsent(long residentId) {
        return retentions.getOrDefault(residentId, new BedRetention(false, Optional.empty()));
    }

    @Override
    public boolean differentUnitPrice(long residentId, long oldRoomTypeId, long newRoomTypeId, LocalDate date) {
        return priceDifferences.getOrDefault(new PriceKey(residentId, oldRoomTypeId, newRoomTypeId, date), false);
    }

    @Override
    public void waitingFileHoldExpired(long waitingFileId) {
        expiredWaitingFiles.add(waitingFileId);
    }

    @Override
    public void allocated(long residentId, long allocationId) {
        allocations.add(new Allocation(residentId, allocationId));
    }

    /** Lời gọi lại của 003 để test kiểm tác động cùng lệnh, không phải bản ghi lưu trú. */
    public record Allocation(long residentId, long allocationId) {}

    private record PriceKey(long residentId, long oldRoomTypeId, long newRoomTypeId, LocalDate date) {}
}
