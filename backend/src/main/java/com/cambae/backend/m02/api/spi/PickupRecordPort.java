package com.cambae.backend.m02.api.spi;

import com.cambae.backend.m02.api.dto.PickupRecordView;
import java.time.Instant;
import java.util.Optional;

/** SPI 012 cài (tên logic {@code BanGhiDonPort}). Provisional: tên chờ E duyệt. */
public interface PickupRecordPort {

    /** FR-048: bản ghi đón hợp lệ của người đón cho người cao tuổi tại thời điểm đó. */
    Optional<PickupRecordView> validPickupRecord(long residentId, long pickerId, Instant at);

    /**
     * {@code Propagation.MANDATORY}, trong lệnh Cho tạm vắng và bàn giao khi Kết thúc lưu trú. Dùng lần hai thì ném lỗi
     * {@code PICKUP_RECORD_ALREADY_USED}.
     */
    void markUsed(long pickupRecordId, String sourceCommand, long sourceId);
}
