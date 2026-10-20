package com.cambae.backend.support.fake.m15;

import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import com.cambae.backend.m15.account.api.spi.BedAllocationApi;
import com.cambae.backend.m15.account.api.spi.BedAllocationView;
import com.cambae.backend.m15.account.api.spi.InvalidPermissionDataException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Bản giả {@link BedAllocationApi} (003 cài): phân bổ test đặt sẵn, không xét thời điểm. */
public class FakeBedAllocations implements BedAllocationApi {

    private final Map<Long, BedAllocationView> allocations = new HashMap<>();
    private final Map<Long, InvalidPermissionDataException> invalid = new HashMap<>();

    public FakeBedAllocations allocate(long residentId, BedAllocationView allocation) {
        allocations.put(residentId, allocation);
        invalid.remove(residentId);
        return this;
    }

    public FakeBedAllocations release(long residentId) {
        allocations.remove(residentId);
        return this;
    }

    /** Dữ liệu nguồn lỗi: {@link #openAt} của người cao tuổi này ném lỗi (002 FR-041b). */
    public FakeBedAllocations invalid(long residentId, String errorKind) {
        invalid.put(residentId, new InvalidPermissionDataException("M03.BED_ALLOCATION", errorKind, residentId));
        return this;
    }

    @Override
    public Optional<BedAllocationView> openAt(long residentId, Instant at) {
        if (invalid.containsKey(residentId)) {
            throw invalid.get(residentId);
        }
        return Optional.ofNullable(allocations.get(residentId));
    }

    @Override
    public Set<Long> residentsIn(ScopeFilter scope, Instant at) {
        return allocations.entrySet().stream()
                .filter(e -> scope.facilityWide()
                        || scope.floorAreaIds().contains(e.getValue().floorAreaId())
                        || scope.roomIds().contains(e.getValue().roomId()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
