package com.eldercare.backend.support.fake.shared;

import com.eldercare.backend.administration.shared.RecordEventApiContract;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import java.util.function.Supplier;

/** Bản giả {@link FakeRecordEventApi} giữ đúng hợp đồng của bản thật (constitution XVI, L). */
class FakeRecordEventApiTest implements RecordEventApiContract {

    private final FakeRecordEventApi api = new FakeRecordEventApi();

    @Override
    public RecordEventApi events() {
        return api;
    }

    @Override
    public <R> R tx(Supplier<R> work) {
        return work.get();
    }
}
