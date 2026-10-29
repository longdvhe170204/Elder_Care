package com.eldercare.backend.resident.api;

import com.eldercare.backend.resident.api.dto.EndOfLifeWishView;
import com.eldercare.backend.resident.api.dto.RecordEndOfLifeWish;
import com.eldercare.backend.resident.api.dto.TreatmentWishView;
import com.eldercare.backend.resident.api.dto.WishRecordSource;
import java.util.Optional;

/** Phiếu nguyện vọng cuối đời và khi điều trị cho 007 (thẻ khẩn cấp, cảnh báo nguy kịch, Chuyển viện). */
public interface EndOfLifeWishApi {

    Optional<EndOfLifeWishView> current(long residentId);

    /** {@code Propagation.MANDATORY}; 007 gọi với {@code CRITICAL_RECONFIRMATION} (BR-M05-16). */
    long recordVersion(RecordEndOfLifeWish command, WishRecordSource source);

    /** Phiếu nguyện vọng khi điều trị Hiệu lực (FR-023); 007 chỉ đọc, không ghi phiếu này. */
    Optional<TreatmentWishView> currentTreatmentWish(long residentId);
}
