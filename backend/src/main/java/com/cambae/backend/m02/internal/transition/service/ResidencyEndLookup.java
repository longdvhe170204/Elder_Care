package com.cambae.backend.m02.internal.transition.service;

import com.cambae.backend.m01.api.ResidentStatusApi;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.api.dto.StatusPeriod;
import com.cambae.backend.m15.account.api.spi.ResidencyEndApi;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Thời điểm người cao tuổi sang trạng thái cuối, cho phạm vi người thân và job khóa tài khoản của 002 (FR-049; SPI
 * {@link ResidencyEndApi} do 002 khai báo, M02 cài). Lấy từ lịch sử trạng thái của 001: trạng thái cuối là Kết thúc lưu
 * trú, Qua đời, Hủy tiếp nhận, và đã vào thì không ra.
 */
@Component
class ResidencyEndLookup implements ResidencyEndApi {

    private static final Set<ResidentStatus> FINAL_STATUSES =
            Set.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED);
    private static final Instant FAR_FUTURE = Instant.parse("9999-12-31T00:00:00Z");

    private final ResidentStatusApi statuses;

    ResidencyEndLookup(ResidentStatusApi statuses) {
        this.statuses = statuses;
    }

    @Override
    public Optional<Instant> finalStatusAt(long residentId) {
        return statuses.statusPeriods(residentId, FINAL_STATUSES, Instant.EPOCH, FAR_FUTURE).stream()
                .map(StatusPeriod::from)
                .min(Comparator.naturalOrder());
    }
}
