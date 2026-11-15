package com.eldercare.backend.care.internal.trip.service;

import static com.eldercare.backend.care.internal.trip.service.TripPreparationService.*;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.administration.shared.api.StatusHistoryApi;
import com.eldercare.backend.care.api.spi.TripIncidentPort;
import com.eldercare.backend.care.internal.trip.domain.*;
import com.eldercare.backend.care.internal.trip.persistence.ActivityTripStore;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.*;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-046–048: khóa chuyến trước khi gọi port; mã sự cố của mỗi lượt đi chỉ đặt một lần. */
@Service
public class TripMissingService {
    private final ActivityTripStore trips;
    private final StatusHistoryApi histories;
    private final ObjectProvider<TripIncidentPort> incidents;
    private final CurrentUser user;
    private final AuditLogApi audit;

    public TripMissingService(
            ActivityTripStore trips,
            StatusHistoryApi histories,
            ObjectProvider<TripIncidentPort> incidents,
            CurrentUser user,
            AuditLogApi audit) {
        this.trips = trips;
        this.histories = histories;
        this.incidents = incidents;
        this.user = user;
        this.audit = audit;
    }

    @RequiresPermission(function = Function.INCIDENT_EMERGENCY, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void report(
            Target target,
            long id,
            List<Long> residentIds,
            boolean afterReturn,
            String lastSeenPlace,
            Instant lastSeenAt,
            String description,
            Instant deviceAt) {
        if (deviceAt != null)
            throw conflict("REQUIRES_ONLINE", "Báo thiếu người chỉ thực hiện trực tuyến, gọi Trưởng tầng khi mất mạng");
        if (residentIds == null
                || residentIds.isEmpty()
                || residentIds.stream().anyMatch(Objects::isNull)
                || new HashSet<>(residentIds).size() != residentIds.size())
            throw new InvalidValueException("Cần danh sách người báo thiếu không trùng");
        if (lastSeenPlace == null
                || lastSeenPlace.isBlank()
                || lastSeenAt == null
                || description == null
                || description.isBlank()) throw new InvalidValueException("Cần nơi, lúc thấy lần cuối và mô tả");
        var trip = trips.lock(id);
        if (!(afterReturn ? "RETURNED" : "IN_PROGRESS").equals(trip.status()))
            throw conflict("STATE_CHANGED", "Chuyến không ở trạng thái cho phép ghi sự cố thiếu người");
        var duties = histories.list(TripKinds.DUTIES, id);
        var duty = duties.isEmpty()
                ? new TripDuties(null, List.of())
                : duties.getLast().details();
        boolean leader = user.staffId()
                .filter(staff -> Objects.equals(staff, duty.leaderId()))
                .isPresent();
        boolean companion = !afterReturn
                && user.staffId().filter(duty.companionIds()::contains).isPresent();
        if (!leader && !companion && !user.roles().contains(Role.FLOOR_MANAGER))
            throw new PermissionDeniedException("Chỉ trưởng đoàn, người đi cùng hoặc Trưởng tầng được báo thiếu người");
        var participants = trips.participants(id);
        for (long resident : residentIds.stream().sorted().toList()) {
            var p = participants.stream()
                    .filter(row -> row.residentId() == resident)
                    .findFirst()
                    .orElseThrow(NotFoundException::new);
            if (trips.missingIncident(p.registrationId()).isPresent())
                throw conflict("MISSING_INCIDENT_ALREADY_RECORDED", "Lượt đi đã có sự cố thiếu người");
            if (!(afterReturn
                    ? "MISSING_ON_RETURN".equals(p.status())
                    : Set.of("DEPARTED", "MISSING_ON_RETURN").contains(p.status())))
                throw conflict("STATE_CHANGED", "Người đã về, rời đoàn hoặc đã có sự cố thiếu người");
            long incident =
                    source().recordMissing(resident, id, duty.leaderId(), lastSeenPlace, lastSeenAt, description);
            trips.recordMissingIncident(p.registrationId(), incident);
            audit.write(AuditLogEntry.builder()
                    .action(new AuditAction("TRIP_MISSING_REPORTED", false))
                    .entity("M04.TRIP_PARTICIPANT", p.registrationId())
                    .before(Map.of("status", p.status()))
                    .after(Map.of("status", "MISSING_ON_RETURN", "incidentId", incident))
                    .build());
        }
    }

    /** Cùng transaction của điểm danh về, không tự đóng sự cố. */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void foundReturned(long registrationId, long residentId, long tripId, Instant at) {
        trips.missingIncident(registrationId)
                .ifPresent(incident -> source().recordFoundReturned(incident, residentId, tripId, at));
    }

    private TripIncidentPort source() {
        var source = incidents.getIfAvailable();
        if (source == null) throw unavailable("Chưa có nguồn ghi sự cố thiếu người của 007");
        return source;
    }
}
