package com.eldercare.backend.administration.shared;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/** Hợp đồng {@link RecordEventApiContract} trên bản thật và MySQL (research R-15, 000 T108). */
class RecordEventApiIT extends MySqlIntegrationTestBase implements RecordEventApiContract {

    @Autowired
    RecordEventApi api;

    @Autowired
    TransactionTemplate transaction;

    @Override
    public RecordEventApi events() {
        return api;
    }

    @Override
    public <R> R tx(Supplier<R> work) {
        return transaction.execute(status -> work.get());
    }
}
