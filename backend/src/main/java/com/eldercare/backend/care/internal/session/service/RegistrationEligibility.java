package com.eldercare.backend.care.internal.session.service;

import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.care.api.spi.ActivityHealthGatePort;
import com.eldercare.backend.care.internal.activity.domain.Activity;
import com.eldercare.backend.care.internal.activity.domain.ActivityKinds;
import com.eldercare.backend.care.internal.session.domain.RegistrationConditions;
import com.eldercare.backend.care.internal.session.domain.RegistrationConditions.DayCare;
import com.eldercare.backend.care.internal.session.domain.RegistrationConditions.Facts;
import com.eldercare.backend.care.internal.session.domain.Session;
import com.eldercare.backend.care.internal.session.persistence.ActivitySessionStore;
import com.eldercare.backend.care.internal.wellbeing.service.PreferenceService;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.AssessmentApi;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 014 T011 (research L-05): gom dữ liệu cho {@link RegistrationConditions}. Nguồn 007 qua {@link ActivityHealthGatePort}
 * (chưa có bean thì không xét được khoanh vùng, nghi nhiễm: chặn bằng 503, chủ dự án duyệt 2026-11-05); lịch bán trú qua
 * {@code ContractReadApi} (004); lưu ý chăm sóc qua {@code AssessmentApi} (001); sở thích "không thích" khớp nhóm của
 * loại hoạt động là cảnh báo không chặn (FR-052, US6).
 */
@Service
public class RegistrationEligibility {
    private final ActivitySessionStore store;
    private final ResidentApi residents;
    private final AssessmentApi assessments;
    private final ContractReadApi contracts;
    private final ObjectProvider<ActivityHealthGatePort> gates;
    private final CatalogApi catalogs;
    private final PreferenceService preferences;

    public RegistrationEligibility(
            ActivitySessionStore store,
            ResidentApi residents,
            AssessmentApi assessments,
            ContractReadApi contracts,
            ObjectProvider<ActivityHealthGatePort> gates,
            CatalogApi catalogs,
            PreferenceService preferences) {
        this.store = store;
        this.residents = residents;
        this.assessments = assessments;
        this.contracts = contracts;
        this.gates = gates;
        this.catalogs = catalogs;
        this.preferences = preferences;
    }

    /** Lệnh của người dùng: nguồn 007 chưa có thì 503. */
    public RegistrationConditions.Result check(
            Session session, Activity activity, long residentId, Instant now, boolean lateReturnConsent) {
        return tryCheck(session, activity, residentId, now, lateReturnConsent)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "CARE_SOURCE_UNAVAILABLE",
                        "Chưa có nguồn khoanh vùng, nghi nhiễm của 007 nên chưa đăng ký được"));
    }

    /** Đăng ký sẵn (FR-008, FR-010): nguồn 007 chưa có thì rỗng, người gọi coi như không đạt. */
    public Optional<RegistrationConditions.Result> tryCheck(
            Session session, Activity activity, long residentId, Instant now, boolean lateReturnConsent) {
        var gate = gates.getIfAvailable();
        if (gate == null) return Optional.empty();
        var status = residents.status(residentId);
        var facts = new Facts(
                session.tripStatus() == null ? session.status() : session.tripStatus(),
                session.tripStatus() == null && !session.startsAt().isAfter(now),
                activity.groupActivity() || session.tripStatus() != null,
                store.activeRegistrations(session.id()).size(),
                activity.maxParticipants(),
                status == ResidentStatus.ADMISSION_IN_PROGRESS || residents.isFinalStatus(residentId),
                gate.residentIsolated(residentId, now),
                gate.locationIsolated(session.locationRoomId(), session.locationAreaId(), session.floorAreaId(), now),
                gate.suspectedInfection(residentId, now),
                gate.hasOpenMediumOrHigher(residentId),
                store.overlaps(residentId, session.id(), session.startsAt(), session.endsAt()),
                dayCare(session, residentId, lateReturnConsent),
                assessments.currentCareNote(List.of(residentId)).getOrDefault(residentId, Optional.empty()));
        var result = RegistrationConditions.check(facts);
        var groups = catalogs.require(ActivityKinds.ACTIVITY_TYPES, activity.typeId())
                .attributes()
                .interestGroupIds();
        var dislikes = preferences.dislikeWarnings(residentId, groups);
        if (dislikes.isEmpty()) return Optional.of(result);
        var warnings = new ArrayList<>(result.warnings());
        warnings.addAll(dislikes);
        return Optional.of(new RegistrationConditions.Result(result.blockers(), List.copyOf(warnings)));
    }

    /** Người bán trú vào ngày của buổi: hợp đồng hiệu lực ngày đó có loại hình Bán trú (004 FR-019). */
    private DayCare dayCare(Session session, long residentId, boolean consent) {
        var date = LocalDate.ofInstant(session.startsAt(), BusinessTime.ZONE);
        boolean dayCare = contracts
                .contentAtDate(residentId, date)
                .flatMap(c -> contracts.contract(c.contractId()))
                .map(c -> c.residenceType() == ResidenceType.DAY_CARE)
                .orElse(false);
        if (!dayCare) return null;
        var schedule = contracts.attendanceOn(residentId, date).stream().findFirst();
        return new DayCare(
                LocalTime.ofInstant(session.startsAt(), BusinessTime.ZONE),
                LocalTime.ofInstant(session.endsAt(), BusinessTime.ZONE),
                schedule.map(s -> s.arrivalTime()).orElse(null),
                schedule.map(s -> s.departureTime()).orElse(null),
                consent);
    }
}
