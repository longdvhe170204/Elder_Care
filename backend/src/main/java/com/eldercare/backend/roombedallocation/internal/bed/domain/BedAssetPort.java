package com.eldercare.backend.roombedallocation.internal.bed.domain;

import java.util.Collection;
import java.util.Set;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Cổng nội bộ 003 gọi, 019 cài; chỉ trao đổi qua cổng, không đọc bảng tài sản (G-01, mục 3 hợp đồng). */
public interface BedAssetPort {
    @Transactional(propagation = Propagation.MANDATORY)
    void recordIncreaseForBed(long bedId);

    @Transactional(propagation = Propagation.MANDATORY)
    boolean awaitingResidentTransfer(long bedId);

    @Transactional(propagation = Propagation.MANDATORY)
    Set<Long> flaggedBeds(Collection<Long> bedIds);

    /** 016 (019 FR-015): giường mà tài sản mang dấu "chờ chuyển người" tại {@code at}. */
    @Transactional(propagation = Propagation.MANDATORY)
    Set<Long> flaggedBedsAt(Collection<Long> bedIds, java.time.Instant at);
}
