package com.eldercare.backend.billing.internal.purchase.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.billing.internal.period.domain.*;
import com.eldercare.backend.billing.internal.period.service.*;
import com.eldercare.backend.billing.internal.purchase.domain.PurchaseDetails;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.notification.*;
import java.time.Instant;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** FR-023a/Q-269: nhắc từng vòng xin đồng ý, không lập bản ghi nghiệp vụ thay người dùng. */
@Service
public class PurchaseReminderTask implements CostTimedTask {
    private final SimpleRecordApi records;
    private final ParameterApi parameters;
    private final ReminderMarkers markers;
    private final CostNotifications notifications;
    private final ApplicationEventPublisher events;
    private final org.springframework.beans.factory.ObjectProvider<
                    com.eldercare.backend.notification.api.spi.FamilyNotificationPort>
            families;
    private final com.eldercare.backend.administration.shared.api.RecordEventApi history;

    public PurchaseReminderTask(
            SimpleRecordApi records,
            ParameterApi parameters,
            ReminderMarkers markers,
            CostNotifications notifications,
            ApplicationEventPublisher events,
            org.springframework.beans.factory.ObjectProvider<
                            com.eldercare.backend.notification.api.spi.FamilyNotificationPort>
                    families,
            com.eldercare.backend.administration.shared.api.RecordEventApi history) {
        this.records = records;
        this.parameters = parameters;
        this.markers = markers;
        this.notifications = notifications;
        this.events = events;
        this.families = families;
        this.history = history;
    }

    public String name() {
        return "PURCHASE_CONSENT_REMINDER";
    }

    public CostJobCadence cadence() {
        return CostJobCadence.HOURLY;
    }

    public boolean latestOnly() {
        return true;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public int run(Instant slot) {
        var reminder = parameters.read(ParameterCode.CFG_M15_05).asDuration();
        var escalation = parameters.read(ParameterCode.CFG_M15_06).asDuration();
        int count = 0;
        for (var candidate : records.listByStatus(PurchaseDetails.KIND, Set.of("AWAITING_CONSENT"))) {
            var row = records.lockForUpdate(PurchaseDetails.KIND, candidate.id());
            if (!row.status().equals("AWAITING_CONSENT") || row.content().sentAt() == null) continue;
            var familySource = families.getIfAvailable();
            if (familySource != null) {
                var sent = history
                        .list(
                                com.eldercare.backend.billing.internal.purchase.domain.PurchaseRecipient.KIND,
                                new com.eldercare.backend.administration.shared.api.dto.EntityRef(
                                        PurchaseDetails.KIND.code(), row.id()))
                        .stream()
                        .filter(e -> e.content().round() == row.content().round())
                        .map(e -> e.content().familyMemberId())
                        .collect(java.util.stream.Collectors.toSet());
                boolean hasPortal = familySource.activeRelationships(row.residentId()).stream()
                        .anyMatch(r -> sent.contains(r.familyMemberId())
                                && r.representative()
                                && r.portalAccountActive()
                                && r.portalAccountId().isPresent());
                if (!hasPortal
                        && markers.mark(
                                "M11.PURCHASE_NO_PORTAL",
                                PurchaseDetails.KIND.code(),
                                row.id(),
                                Integer.toString(row.content().round()),
                                null))
                    notifications.staff(
                            "PURCHASE_NO_PORTAL_REPRESENTATIVE",
                            PurchaseDetails.KIND.code(),
                            row.id(),
                            row.residentId(),
                            "purchase-no-portal:" + row.id() + ":"
                                    + row.content().round(),
                            "Không còn người đại diện dùng cổng; Quản lý viện quyết định đề nghị mua hộ",
                            Role.OFFICE_STAFF,
                            slot,
                            null);
            }
            for (boolean escalate : new boolean[] {false, true}) {
                if (!slot.isAfter(row.content().sentAt().plus(escalate ? escalation : reminder))) continue;
                String key = row.content().round() + ":" + (escalate ? "ESCALATE" : "REMIND");
                if (!markers.mark("M11.PURCHASE_REMINDER", PurchaseDetails.KIND.code(), row.id(), key, null)) continue;
                List<RecipientGroup> recipients = escalate
                        ? List.of(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER))
                        : List.of(
                                new RecipientGroup.Representative(row.residentId()),
                                new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
                events.publishEvent(new CostNotificationEvent(List.of(notifications.request(
                        "PURCHASE_CONSENT_OVERDUE",
                        PurchaseDetails.KIND.code(),
                        row.id(),
                        row.residentId(),
                        "purchase-reminder:" + row.id() + ":" + key,
                        "Đề nghị mua hộ #" + row.id() + " đang chờ đồng ý quá hạn",
                        recipients,
                        NotificationLevel.MEDIUM,
                        slot,
                        null))));
                count++;
            }
        }
        return count;
    }
}
