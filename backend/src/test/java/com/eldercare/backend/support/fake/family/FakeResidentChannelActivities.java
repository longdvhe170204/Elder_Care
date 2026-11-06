package com.eldercare.backend.support.fake.family;

import com.eldercare.backend.family.api.dto.ResidentChannelSessionView;
import com.eldercare.backend.family.api.spi.ResidentChannelActivityPort;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Bản giả {@link ResidentChannelActivityPort} (012 T067) tới khi 014 cài: buổi sắp tới theo người cao tuổi. */
public class FakeResidentChannelActivities implements ResidentChannelActivityPort {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final Map<Long, List<ResidentChannelSessionView>> sessions = new ConcurrentHashMap<>();

    public FakeResidentChannelActivities session(long residentId, ResidentChannelSessionView view) {
        sessions.computeIfAbsent(residentId, id -> new ArrayList<>()).add(view);
        return this;
    }

    @Override
    public List<ResidentChannelSessionView> upcoming(long residentId, LocalDate from, LocalDate to) {
        return sessions.getOrDefault(residentId, List.of()).stream()
                .filter(s -> {
                    LocalDate day = s.startAt().atZone(ZONE).toLocalDate();
                    return !day.isBefore(from) && !day.isAfter(to);
                })
                .toList();
    }
}
