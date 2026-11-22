package com.eldercare.backend.family.internal.portal.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.care.api.DailyCareScheduleApi;
import com.eldercare.backend.care.api.dto.DailyScheduleEntry;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.api.FamilyRightsApi;
import com.eldercare.backend.family.internal.portal.domain.HealthAccess;
import com.eldercare.backend.family.internal.relationship.service.RelationshipQueries;
import com.eldercare.backend.family.internal.residentchannel.service.ResidentChannelService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * "Lịch chăm sóc hằng ngày" (012 FR-076, FR-083 (a); Q-416): người thân có quan hệ Hiệu lực xem của người cao tuổi, giá trị
 * đo chỉ khi quyền xem sức khỏe có tác dụng (FR-011); người cao tuổi xem của chính mình, không có giá trị đo. Chỉ xem.
 */
@Service
public class DailyCareScheduleView {
    private final RelationshipQueries relationships;
    private final FamilyRightsApi rights;
    private final ResidentChannelService channel;
    private final ObjectProvider<DailyCareScheduleApi> schedule;
    private final ObjectProvider<CurrentUser> currentUser;
    private final BusinessTime time;

    public DailyCareScheduleView(
            RelationshipQueries relationships,
            FamilyRightsApi rights,
            ResidentChannelService channel,
            ObjectProvider<DailyCareScheduleApi> schedule,
            ObjectProvider<CurrentUser> currentUser,
            BusinessTime time) {
        this.relationships = relationships;
        this.rights = rights;
        this.channel = channel;
        this.schedule = schedule;
        this.currentUser = currentUser;
        this.time = time;
    }

    public record Schedule(
            LocalDate date, boolean measurementsShown, boolean available, List<DailyScheduleEntry> items) {}

    public Schedule forFamily(long residentId, LocalDate date) {
        CurrentUser user = currentUser.getObject();
        if (user.type() != AccountType.FAMILY_MEMBER || user.familyMemberId().isEmpty()) throw denied();
        long self = user.familyMemberId().get();
        if (relationships.active(self, residentId).isEmpty()) throw denied();
        var now = time.now();
        boolean health = HealthAccess.decide(
                        relationships.rightEnabledAt(
                                self,
                                residentId,
                                com.eldercare.backend.common.notification.FamilyRight.VIEW_HEALTH,
                                now),
                        rights.rightEffective(
                                self,
                                residentId,
                                com.eldercare.backend.common.notification.FamilyRight.VIEW_HEALTH,
                                now))
                == HealthAccess.Decision.ALLOWED;
        return read(residentId, date, health);
    }

    public Schedule forResident(LocalDate date) {
        return read(channel.self(), date, false);
    }

    private Schedule read(long residentId, LocalDate date, boolean measurements) {
        var day = date == null ? time.today() : date;
        var api = schedule.getIfAvailable();
        if (api == null) return new Schedule(day, measurements, false, List.of());
        return new Schedule(day, measurements, true, api.onDate(residentId, day, measurements));
    }

    private static PermissionDeniedException denied() {
        return new PermissionDeniedException("PERMISSION_DENIED", "Không có quyền", Map.of("failedLayer", "SCOPE"));
    }
}
