package com.eldercare.backend.incident.internal.infection.service;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.*;
import com.eldercare.backend.incident.internal.infection.domain.ContainmentScope;
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Các nhóm nhận tin khi đặt khoanh vùng theo FR-064, không mang dữ liệu người nghi nhiễm. */
final class ContainmentNotices {
    private ContainmentNotices() {}

    static List<NotificationRequest> created(
            long id, ContainmentScope scope, LocationStructureApi locations, long actorId, Instant at) {
        var floors = new HashSet<>(scope.floorIds());
        for (long area : scope.areaIds()) floors.addAll(locations.floorsInArea(area));
        var recipients = new ArrayList<RecipientGroup>();
        recipients.add(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF));
        for (long floor : floors) {
            recipients.add(new RecipientGroup.FloorManager(floor, new Substitution.Default()));
            recipients.add(new RecipientGroup.OnShiftStaff(
                    floor, Set.of(Role.NURSING_STAFF), Set.of(), new Substitution.NoSubstitute()));
        }
        return List.of(new NotificationRequest(
                "M05",
                "ISOLATION_CREATED",
                SourceGroup.INCIDENT,
                new SourceRecord("M05.CONTAINMENT", Long.toString(id)),
                "M05.CONTAINMENT:" + id + ":CREATED",
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                recipients,
                List.of(new ContentPart(
                        InformationType.GENERAL,
                        "Đã đặt khoanh vùng. Xem phạm vi và thực hiện theo quy trình của viện.")),
                Optional.of("Đã đặt khoanh vùng"),
                Optional.of("Khoanh vùng lây nhiễm"),
                Optional.of(actorId),
                false,
                at,
                Map.of("containmentId", id),
                false,
                Optional.empty(),
                Optional.empty()));
    }
}
