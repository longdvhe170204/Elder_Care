package com.cambae.backend.m03.internal.bed.domain.port;

import java.util.Collection;
import java.util.Set;

/**
 * 019 cài, 003 gọi; port nội bộ M03 (contracts mục 3). Bản cài thật chạy MANDATORY, không xuất thành NamedInterface.
 */
public interface BedAssetPort {
    /** Tạo tài sản trong cùng lệnh Tạo giường (Q-238, DBR-32). */
    void createForBed(long bedId);

    boolean pendingResidentMove(long bedId);

    Set<Long> flaggedBeds(Collection<Long> bedIds);
}
