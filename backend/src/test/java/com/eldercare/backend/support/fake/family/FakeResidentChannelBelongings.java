package com.eldercare.backend.support.fake.family;

import com.eldercare.backend.family.api.dto.ResidentChannelBelongingView;
import com.eldercare.backend.family.api.spi.ResidentChannelBelongingPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Bản giả {@link ResidentChannelBelongingPort} (012 T067) tới khi 013 cài: đồ gửi đang giữ theo người cao tuổi. */
public class FakeResidentChannelBelongings implements ResidentChannelBelongingPort {

    private final Map<Long, List<ResidentChannelBelongingView>> held = new ConcurrentHashMap<>();

    public FakeResidentChannelBelongings belonging(long residentId, ResidentChannelBelongingView view) {
        held.computeIfAbsent(residentId, id -> new ArrayList<>()).add(view);
        return this;
    }

    @Override
    public List<ResidentChannelBelongingView> held(long residentId) {
        return held.getOrDefault(residentId, List.of());
    }
}
