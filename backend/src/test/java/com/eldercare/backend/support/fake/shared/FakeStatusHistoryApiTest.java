package com.eldercare.backend.support.fake.shared;

import com.eldercare.backend.administration.shared.StatusHistoryApiContract;
import com.eldercare.backend.administration.shared.api.StatusHistoryApi;
import java.util.function.Supplier;

/** Bản giả {@link FakeStatusHistoryApi} giữ đúng hợp đồng của bản thật (constitution XVI, L). */
class FakeStatusHistoryApiTest implements StatusHistoryApiContract {

    private final FakeStatusHistoryApi api = new FakeStatusHistoryApi();

    @Override
    public StatusHistoryApi history() {
        return api;
    }

    @Override
    public <R> R tx(Supplier<R> work) {
        return work.get();
    }
}
