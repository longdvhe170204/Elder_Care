package com.cambae.backend.m01.internal.wish.service;

import com.cambae.backend.m01.api.EndOfLifeWishApi;
import com.cambae.backend.m01.api.dto.EndOfLifeWishView;
import com.cambae.backend.m01.api.dto.RecordEndOfLifeWish;
import com.cambae.backend.m01.api.dto.TreatmentWishView;
import com.cambae.backend.m01.api.dto.WishRecordSource;
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
