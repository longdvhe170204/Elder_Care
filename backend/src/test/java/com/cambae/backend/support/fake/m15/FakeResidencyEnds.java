package com.cambae.backend.support.fake.m15;

import com.cambae.backend.m15.account.api.spi.ResidencyEndApi;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Bản giả {@link ResidencyEndApi} (004 cài). */
public class FakeResidencyEnds implements ResidencyEndApi {

    private final Map<Long, Instant> finalStatusAt = new HashMap<>();

    public FakeResidencyEnds ended(long residentId, Instant at) {
        finalStatusAt.put(residentId, at);
        return this;
    }

    @Override
    public Optional<Instant> finalStatusAt(long residentId) {
        return Optional.ofNullable(finalStatusAt.get(residentId));
    }
}
