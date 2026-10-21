package com.cambae.backend.support.fake.m03;

import com.cambae.backend.m03.internal.bed.domain.port.BedAssetPort;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bản giả port 019 (003 contracts mục 3): tạo tài sản không ghi dữ liệu, mặc định không có dấu.
 * Test có thể bật dấu hoặc lỗi tạo tài sản để kiểm rollback.
 */
public class FakeBedAssets implements BedAssetPort {

    private final Set<Long> flagged = new HashSet<>();
    private boolean failCreation;

    public FakeBedAssets pendingResidentMove(long bedId, boolean pending) {
        if (pending) {
            flagged.add(bedId);
        } else {
            flagged.remove(bedId);
        }
        return this;
    }

    public FakeBedAssets failCreation(boolean fail) {
        failCreation = fail;
        return this;
    }

    public void reset() {
        flagged.clear();
        failCreation = false;
    }

    @Override
    public void createForBed(long bedId) {
        if (failCreation) {
            throw new IllegalStateException("Lỗi tạo tài sản giả lập (003 T002, Q-238)");
        }
    }

    @Override
    public boolean pendingResidentMove(long bedId) {
        return flagged.contains(bedId);
    }

    @Override
    public Set<Long> flaggedBeds(Collection<Long> bedIds) {
        return bedIds.stream().filter(flagged::contains).collect(Collectors.toUnmodifiableSet());
    }
}
