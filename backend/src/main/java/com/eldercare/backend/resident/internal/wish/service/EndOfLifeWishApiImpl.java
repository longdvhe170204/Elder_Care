package com.eldercare.backend.resident.internal.wish.service;

import com.eldercare.backend.resident.api.EndOfLifeWishApi;
import com.eldercare.backend.resident.api.dto.EndOfLifeWishView;
import com.eldercare.backend.resident.api.dto.RecordEndOfLifeWish;
import com.eldercare.backend.resident.api.dto.TreatmentWishView;
import com.eldercare.backend.resident.api.dto.WishRecordSource;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** {@link EndOfLifeWishApi} cho 007: đọc hai phiếu Hiệu lực và ghi phiên bản xác nhận lại lúc nguy kịch. */
@Service
class EndOfLifeWishApiImpl implements EndOfLifeWishApi {

    private final EndOfLifeWishService wishes;
    private final TreatmentWishService treatments;

    EndOfLifeWishApiImpl(EndOfLifeWishService wishes, TreatmentWishService treatments) {
        this.wishes = wishes;
        this.treatments = treatments;
    }

    @Override
    public Optional<EndOfLifeWishView> current(long residentId) {
        return wishes.currentView(residentId);
    }

    @Override
    public long recordVersion(RecordEndOfLifeWish command, WishRecordSource source) {
        return wishes.recordVersion(command, source);
    }

    @Override
    public Optional<TreatmentWishView> currentTreatmentWish(long residentId) {
        return treatments.currentView(residentId);
    }
}
