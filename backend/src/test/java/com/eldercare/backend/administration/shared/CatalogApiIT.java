package com.eldercare.backend.administration.shared;

import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/** Hợp đồng {@link CatalogApiContract} trên bản thật và MySQL (research R-15, 000 T108). */
class CatalogApiIT extends MySqlIntegrationTestBase implements CatalogApiContract {

    @Autowired
    CatalogApi api;

    @Autowired
    TransactionTemplate transaction;

    @Override
    public CatalogApi catalog() {
        return api;
    }

    @Override
    public <R> R tx(Supplier<R> work) {
        return transaction.execute(status -> work.get());
    }
}
