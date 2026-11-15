package com.eldercare.backend.roombedallocation.internal.allocation.persistence;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * Chỉ mục kỹ thuật trên dòng vị trí đã khóa; không lưu thêm bản ghi nghiệp vụ. T066: {@code waitingHoldBedIds} của
 * {@link ResidentPositionStore} (thay cột {@code resident_position.waiting_hold_bed_ids}).
 */
@Repository
public class WaitingHoldIndexStore {
    private final ResidentPositionStore positions;

    public WaitingHoldIndexStore(ResidentPositionStore positions) {
        this.positions = positions;
    }

    public List<Long> current(long residentId) {
        return positions.locked(residentId).waitingHoldBedIds().stream()
                .distinct()
                .sorted()
                .toList();
    }

    public void update(long residentId, long bedId, boolean add) {
        var ids = new ArrayList<>(current(residentId));
        ids.remove(bedId);
        if (add) ids.add(bedId);
        positions.waitingHoldBedIds(residentId, ids.stream().sorted().toList());
    }
}
