package com.eldercare.backend.roombedallocation.internal.asset.service;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.roombedallocation.internal.asset.persistence.AssetStore;
import com.eldercare.backend.roombedallocation.internal.bed.domain.BedAssetPort;
import java.util.Collection;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 019 T020, hợp đồng 003 mục 3: bản cài {@link BedAssetPort}. Dấu "chờ chuyển người" đọc từ {@code asset}. Ghi tăng tài
 * sản giường kèm lệnh Tạo giường (FR-011, Q-238) cần id bộ phận Chăm sóc mà 008 chưa cung cấp cách tra; chủ dự án chọn
 * sau (2026-11-06), tới đó trả 503 và lệnh Tạo giường không ghi.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class BedAssets implements BedAssetPort {
    private final AssetStore store;

    public BedAssets(AssetStore store) {
        this.store = store;
    }

    @Override
    public void recordIncreaseForBed(long bedId) {
        throw new BusinessException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "SOURCE_UNAVAILABLE",
                "Chưa có cách xác định bộ phận Chăm sóc cho tài sản giường (FR-011)");
    }

    @Override
    public boolean awaitingResidentTransfer(long bedId) {
        return !store.flaggedBeds(Set.of(bedId)).isEmpty();
    }

    @Override
    public Set<Long> flaggedBeds(Collection<Long> bedIds) {
        return store.flaggedBeds(bedIds);
    }
}
