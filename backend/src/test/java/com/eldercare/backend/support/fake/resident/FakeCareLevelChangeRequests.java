package com.eldercare.backend.support.fake.resident;

import com.eldercare.backend.resident.api.spi.CareLevelChangeRequestApi;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Bản giả {@link CareLevelChangeRequestApi} (004 cài): yêu cầu đang mở do test đặt sẵn. */
public class FakeCareLevelChangeRequests implements CareLevelChangeRequestApi {

    private final Map<Long, List<Long>> open = new HashMap<>();

    public FakeCareLevelChangeRequests open(long residentId, long requestId) {
        open.computeIfAbsent(residentId, id -> new ArrayList<>()).add(requestId);
        return this;
    }

    @Override
    public List<Long> supersedeByAssessment(long residentId, long assessmentId) {
        List<Long> superseded = open.remove(residentId);
        return superseded == null ? List.of() : List.copyOf(superseded);
    }

    @Override
    public boolean hasOpenRequest(long residentId) {
        return !open.getOrDefault(residentId, List.of()).isEmpty();
    }
}
