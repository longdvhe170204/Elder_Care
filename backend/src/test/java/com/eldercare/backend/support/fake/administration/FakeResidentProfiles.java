package com.eldercare.backend.support.fake.administration;

import com.eldercare.backend.administration.account.api.spi.ResidentProfileApi;
import com.eldercare.backend.administration.account.api.spi.ResidentProfileView;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Bản giả {@link ResidentProfileApi} (001 cài); người cao tuổi chưa đặt thì không có hồ sơ. */
public class FakeResidentProfiles implements ResidentProfileApi {

    private final Map<Long, ResidentProfileView> profiles = new HashMap<>();

    public FakeResidentProfiles resident(long residentId, String fullName, boolean inResidence, boolean finalStatus) {
        profiles.put(residentId, new ResidentProfileView(fullName, inResidence, finalStatus));
        return this;
    }

    @Override
    public Optional<ResidentProfileView> profile(long residentId) {
        return Optional.ofNullable(profiles.get(residentId));
    }
}
