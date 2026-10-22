package com.cambae.backend.support.fake.m03;

import com.cambae.backend.m03.internal.bed.domain.BedAssetPort;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Bản giả 019: ghi tăng không ghi dữ liệu; dấu chờ chuyển người do test bật/tắt (hợp đồng mục 3). */
public class FakeBedAssets implements BedAssetPort {
    private final Set<Long> flagged = new HashSet<>();

    public FakeBedAssets flag(long bedId, boolean value) {
        if (value) {
            flagged.add(bedId);
        } else {
            flagged.remove(bedId);
        }
        return this;
    }

    @Override
    public void recordIncreaseForBed(long bedId) {}

    @Override
    public boolean awaitingResidentTransfer(long bedId) {
        return flagged.contains(bedId);
    }

    @Override
    public Set<Long> flaggedBeds(Collection<Long> bedIds) {
        return bedIds.stream().filter(flagged::contains).collect(Collectors.toUnmodifiableSet());
    }

    public void reset() {
        flagged.clear();
    }
}
