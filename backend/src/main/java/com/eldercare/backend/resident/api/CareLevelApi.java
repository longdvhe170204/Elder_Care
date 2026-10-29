package com.eldercare.backend.resident.api;

import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.CareLevelAtDate;
import com.eldercare.backend.resident.api.dto.CareNeedView;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

/** Mức chăm sóc cho 003, 004, 008, 010, 016. */
public interface CareLevelApi {

    Optional<CareLevel> current(long residentId);

    /** Kèm căn cứ (FR-053). */
    Optional<CareLevelAtDate> atDate(long residentId, LocalDate date);

    /** Nhu cầu từ lần đánh giá Đã xác nhận gần nhất. */
    Set<CareNeedView> currentCareNeeds(long residentId);

    /** {@code Propagation.MANDATORY}; chỉ handler áp dụng của 004 gọi. */
    void applyChange(long residentId, CareLevel level, LocalDate effectiveDate, long approvalRequestId);
}
