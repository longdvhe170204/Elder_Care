package com.eldercare.backend.care.internal.session.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipApi;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.care.internal.activity.domain.ActivityKinds;
import com.eldercare.backend.care.internal.session.domain.Session;
import com.eldercare.backend.care.internal.session.persistence.ActivitySessionStore;
import com.eldercare.backend.care.internal.trip.domain.TripPlan;
import com.eldercare.backend.care.internal.trip.persistence.ActivityTripStore;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 014 FR-068 (T026; schema chủ dự án duyệt 2026-11-10): hoạt động của người cao tuổi trên cổng người thân, loại thông tin
 * "chung" (012 FR-046): buổi đã đăng ký kèm đủ điều kiện đồng ý về muộn và mã bản đồng ý Hiệu lực (FR-016a), chuyến đi,
 * số buổi đã tham gia. Không trả mức tham gia, giao tiếp, tình trạng sau hoạt động, kết quả kiểm tra.
 */
@Service
@Transactional(readOnly = true)
public class FamilyActivityReadService {

    /** Khoảng mặc định khi không truyền: 30 ngày trước tới 30 ngày sau hôm nay. */
    static final int DEFAULT_DAYS = 30;

    private final ActivitySessionStore sessions;
    private final ActivityTripStore trips;
    private final SimpleRecordApi activities;
    private final LateReturnConsentService consents;
    private final FamilyRelationshipApi relationships;
    private final CurrentUser user;
    private final BusinessTime time;

    FamilyActivityReadService(
            ActivitySessionStore sessions,
            ActivityTripStore trips,
            SimpleRecordApi activities,
            LateReturnConsentService consents,
            FamilyRelationshipApi relationships,
            CurrentUser user,
            BusinessTime time) {
        this.sessions = sessions;
        this.trips = trips;
        this.activities = activities;
        this.consents = consents;
        this.relationships = relationships;
        this.user = user;
        this.time = time;
    }

    public record RegisteredSession(
            long sessionId,
            String activityName,
            Instant startsAt,
            Instant endsAt,
            String registrationStatus,
            boolean lateReturnConsentEligible,
            Long activeLateReturnConsentId) {}

    public record TripView(
            long sessionId, String destination, Instant departureAt, Instant expectedReturnAt, String status) {}

    public record FamilyActivities(
            LocalDate from,
            LocalDate to,
            List<RegisteredSession> registeredSessions,
            List<TripView> trips,
            int attendedSessionCount) {}

    public FamilyActivities of(long residentId, LocalDate from, LocalDate to) {
        long familyMember = user.familyMemberId()
                .orElseThrow(() -> new PermissionDeniedException("Chỉ người thân xem hoạt động trên cổng"));
        if (relationships.relationship(familyMember, residentId).isEmpty())
            throw new PermissionDeniedException("Không có quan hệ Hiệu lực với người cao tuổi này");
        LocalDate start = from != null ? from : time.today().minusDays(DEFAULT_DAYS);
        LocalDate end = to != null ? to : time.today().plusDays(DEFAULT_DAYS);
        if (end.isBefore(start)) throw new InvalidValueException("Khoảng ngày không hợp lệ");
        Instant fromAt = time.startOfDay(start);
        Instant toAt = time.startOfDay(end.plusDays(1));

        var registered = new ArrayList<RegisteredSession>();
        var tripViews = new ArrayList<TripView>();
        for (var registration : sessions.registrationsStartingIn(residentId, fromAt, toAt)) {
            Session session = sessions.find(registration.sessionId()).orElse(null);
            if (session == null) continue;
            if (session.tripStatus() != null) {
                TripPlan plan = trips.find(session.id()).map(t -> t.plan()).orElse(null);
                tripViews.add(new TripView(
                        session.id(),
                        plan == null ? null : plan.destination(),
                        plan == null ? session.startsAt() : plan.plannedDepartureAt(),
                        plan == null ? session.endsAt() : plan.plannedReturnAt(),
                        session.tripStatus()));
                continue;
            }
            var consent = consents.portalConsent(session, residentId);
            registered.add(new RegisteredSession(
                    session.id(),
                    activities
                            .require(ActivityKinds.ACTIVITIES, session.activityId())
                            .content()
                            .name(),
                    session.startsAt(),
                    session.endsAt(),
                    registration.status(),
                    consent.eligible(),
                    consent.activeConsentId()));
        }
        return new FamilyActivities(
                start,
                end,
                List.copyOf(registered),
                List.copyOf(tripViews),
                sessions.attendedCount(residentId, fromAt, toAt));
    }
}
