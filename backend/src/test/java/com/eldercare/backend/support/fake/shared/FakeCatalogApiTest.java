package com.eldercare.backend.support.fake.shared;

import com.eldercare.backend.administration.shared.CatalogApiContract;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import java.util.function.Supplier;

/** Bản giả {@link FakeCatalogApi} giữ đúng hợp đồng của bản thật (constitution XVI, L). */
class FakeCatalogApiTest implements CatalogApiContract {

    private final FakeCatalogApi api = new FakeCatalogApi();

    @Override
    public CatalogApi catalog() {
        return api;
    }

    @Override
    public <R> R tx(Supplier<R> work) {
        return work.get();
    }
}
