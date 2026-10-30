package com.eldercare.backend.support.fake.shared;

import com.eldercare.backend.administration.shared.ResidentModuleStateApiContract;
import com.eldercare.backend.administration.shared.api.ResidentModuleStateApi;
import java.util.function.Supplier;

/** Bản giả {@link FakeResidentModuleStateApi} giữ đúng hợp đồng của bản thật (constitution XVI, L). */
class FakeResidentModuleStateApiTest implements ResidentModuleStateApiContract {

    private final FakeResidentModuleStateApi api = new FakeResidentModuleStateApi();

    @Override
    public ResidentModuleStateApi states() {
        return api;
    }

    @Override
    public <R> R tx(Supplier<R> work) {
        return work.get();
    }
}
