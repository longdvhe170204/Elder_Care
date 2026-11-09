package com.eldercare.backend.billing.internal.period.service;

import com.eldercare.backend.billing.internal.period.domain.CostNotificationEvent;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.*;
import java.time.Instant;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class CostNotifications {
    private final ApplicationEventPublisher events;

    public CostNotifications(ApplicationEventPublisher events) {
        this.events = events;
    }

    public NotificationRequest request(
            String event,
            String type,
            long id,
            Long resident,
            String key,
            String content,
            List<RecipientGroup> recipients,
            NotificationLevel level,
            Instant at,
            Long actor) {
        boolean family = recipients.stream()
                .anyMatch(
                        r -> r instanceof RecipientGroup.FamilyWithRight || r instanceof RecipientGroup.Representative);
        return new NotificationRequest(
                "M11",
                event,
                SourceGroup.COSTS,
                new SourceRecord(type, Long.toString(id)),
                key,
                Optional.of(level),
                Optional.ofNullable(resident),
                Optional.empty(),
                List.copyOf(recipients),
                List.of(new ContentPart(InformationType.COSTS, content)),
                Optional.of("Có cập nhật chi phí"),
                Optional.of("Chi phí"),
                Optional.ofNullable(actor),
                true,
                at,
                Map.of(),
                false,
                family ? Optional.of(InformationSubject.ABOUT_FAMILY_MATTER) : Optional.empty(),
                Optional.empty());
    }

    public void staff(
            String event,
            long statement,
            long resident,
            String key,
            String content,
            Role role,
            Instant at,
            Long actor) {
        staff(event, "M11.COST_STATEMENT", statement, resident, key, content, role, at, actor);
    }

    public void staff(
            String event,
            String type,
            long statement,
            long resident,
            String key,
            String content,
            Role role,
            Instant at,
            Long actor) {
        events.publishEvent(new CostNotificationEvent(List.of(request(
                event,
                type,
                statement,
                resident,
                key,
                content,
                List.of(new RecipientGroup.FacilityRole(role)),
                NotificationLevel.LOW,
                at,
                actor))));
    }
}
