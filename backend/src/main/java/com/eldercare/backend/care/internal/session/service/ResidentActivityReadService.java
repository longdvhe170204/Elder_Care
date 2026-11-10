package com.eldercare.backend.care.internal.session.service;

import com.eldercare.backend.administration.account.api.spi.BedAllocationApi;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.care.api.ResidentActivityReadApi;
import com.eldercare.backend.care.api.dto.ResidentActivitySessionView;
import com.eldercare.backend.care.internal.activity.domain.Activity;
import com.eldercare.backend.care.internal.activity.domain.ActivityKinds;
import com.eldercare.backend.care.internal.session.persistence.ActivitySessionStore;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-068a, L-16: chỉ đọc lịch và cờ có tên cho 012, không trả dữ liệu sức khỏe. */
@Service
public class ResidentActivityReadService implements ResidentActivityReadApi {
    private final ActivitySessionStore sessions;
    private final SimpleRecordApi activities;
    private final BedAllocationApi beds;
    private final BusinessTime time;

    public ResidentActivityReadService(
            ActivitySessionStore sessions, SimpleRecordApi activities, BedAllocationApi beds, BusinessTime time) {
        this.sessions = sessions;
        this.activities = activities;
        this.beds = beds;
        this.time = time;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResidentActivitySessionView> upcoming(long residentId, LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from))
            throw new InvalidValueException("Khoảng ngày không hợp lệ");
        var result = new ArrayList<ResidentActivitySessionView>();
        for (var row : sessions.list(from, to, null)) {
            var s = row.session();
            if (!"SCHEDULED".equals(s.status()) || s.startsAt().isBefore(time.now())) continue;
            var activity =
                    activities.require(ActivityKinds.ACTIVITIES, s.activityId()).content();
            boolean facilityWide = Activity.FACILITY.equals(activity.scope());
            var floor = beds.openAt(residentId, s.startsAt())
                    .map(b -> b.floorAreaId())
                    .or(() -> beds.dayCareFloorAt(residentId, s.startsAt()));
            if (!facilityWide && (floor.isEmpty() || !Objects.equals(floor.get(), s.floorAreaId()))) continue;
            result.add(new ResidentActivitySessionView(
                    s.id(),
                    activity.name(),
                    s.startsAt(),
                    s.endsAt(),
                    facilityWide,
                    sessions.findActiveRegistration(s.id(), residentId).isPresent()));
        }
        return List.copyOf(result);
    }
}
