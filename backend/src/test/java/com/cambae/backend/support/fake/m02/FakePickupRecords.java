package com.cambae.backend.support.fake.m02;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.m02.api.dto.PickupRecordView;
import com.cambae.backend.m02.api.spi.PickupRecordPort;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;

/** Bản giả {@link PickupRecordPort} (012 cài): dùng lần hai ném lỗi như bản thật. */
public class FakePickupRecords implements PickupRecordPort {

    private final Map<String, PickupRecordView> valid = new HashMap<>();
    private final Set<Long> used = new HashSet<>();

    private static String key(long residentId, long pickerId) {
        return residentId + ":" + pickerId;
    }

    public FakePickupRecords valid(long residentId, long pickerId, PickupRecordView view) {
        valid.put(key(residentId, pickerId), view);
        return this;
    }

    public FakePickupRecords reset() {
        valid.clear();
        used.clear();
        return this;
    }

    public boolean isUsed(long pickupRecordId) {
        return used.contains(pickupRecordId);
    }

    @Override
    public Optional<PickupRecordView> validPickupRecord(long residentId, long pickerId, Instant at) {
        return Optional.ofNullable(valid.get(key(residentId, pickerId)));
    }

    @Override
    public void markUsed(long pickupRecordId, String sourceCommand, long sourceId) {
        if (!used.add(pickupRecordId)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "PICKUP_RECORD_ALREADY_USED", "Bản ghi đón đã được dùng cho một lệnh khác");
        }
    }
}
