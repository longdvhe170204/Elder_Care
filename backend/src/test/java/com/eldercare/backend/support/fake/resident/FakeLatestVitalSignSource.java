package com.eldercare.backend.support.fake.resident;

import com.eldercare.backend.resident.api.dto.LatestVitalSign;
import com.eldercare.backend.resident.api.spi.LatestVitalSignSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Bản giả {@link LatestVitalSignSource} (health cài): chỉ số gần nhất do test đặt sẵn theo người cao tuổi. */
public class FakeLatestVitalSignSource implements LatestVitalSignSource {

    private final Map<Long, List<LatestVitalSign>> latest = new HashMap<>();

    public FakeLatestVitalSignSource latest(long residentId, List<LatestVitalSign> signs) {
        latest.put(residentId, signs);
        return this;
    }

    @Override
    public List<LatestVitalSign> latest(long residentId) {
        return latest.getOrDefault(residentId, List.of());
    }
}
