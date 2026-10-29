package com.eldercare.backend.support.fake.administration;

import com.eldercare.backend.administration.account.api.spi.FloorAreaNameApi;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Bản giả {@link FloorAreaNameApi} (003 cài): tên tầng test đặt sẵn. */
public class FakeFloorAreaNames implements FloorAreaNameApi {

    private final Map<Long, String> names = new HashMap<>();

    public FakeFloorAreaNames name(long floorAreaId, String name) {
        names.put(floorAreaId, name);
        return this;
    }

    @Override
    public Optional<String> name(long floorAreaId) {
        return Optional.ofNullable(names.get(floorAreaId));
    }
}
