package com.eldercare.backend.family.internal.visit.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.dto.AccountStatus;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.api.events.ResidentVisitReminder;
import com.eldercare.backend.family.internal.relationship.domain.FamilyMember;
import com.eldercare.backend.family.internal.relationship.service.RelationshipQueries;
import com.eldercare.backend.family.internal.visit.domain.Visit;
import com.eldercare.backend.family.internal.visit.domain.Visit.Status;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Nhắc người cao tuổi có tài khoản Hoạt động về lượt thăm Đã duyệt trong ngày (012 T069; FR-069, research F-18): việc
 * nửa đêm cho mọi lượt của ngày, lượt lập trong ngày nhắc ngay khi lập. Không lập bản ghi (Q-306).
 */
@Component
public class ResidentVisitReminders {

    private final SimpleRecordApi records;
    private final AccountApi accounts;
    private final RelationshipQueries relationships;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;

    ResidentVisitReminders(
            SimpleRecordApi records,
            AccountApi accounts,
            RelationshipQueries relationships,
            BusinessTime time,
            ApplicationEventPublisher events) {
        this.records = records;
        this.accounts = accounts;
        this.relationships = relationships;
        this.time = time;
        this.events = events;
    }

    /** Việc nửa đêm: nhắc mọi lượt Đã duyệt của ngày {@code scheduledAt}. */
    int remindDay(Instant scheduledAt, Instant actualAt) {
        LocalDate day = scheduledAt.atZone(BusinessTime.ZONE).toLocalDate();
        int sent = 0;
        for (SimpleRecord<Visit> v : records.listByStatus(Visit.KIND, Set.of(Status.APPROVED.name()))) {
            if (v.content().date().equals(day) && remind(v, actualAt)) {
                sent++;
            }
        }
        return sent;
    }

    /** Lượt vừa lập cho chính ngày hôm nay. */
    void onRegistered(SimpleRecord<Visit> v) {
        if (v.content().date().equals(time.today())) {
            remind(v, time.now());
        }
    }

    private boolean remind(SimpleRecord<Visit> v, Instant at) {
        long residentId = v.residentId();
        boolean hasAccount = accounts.byResident(residentId)
                .filter(a -> a.status() == AccountStatus.ACTIVE)
                .isPresent();
        if (!hasAccount) {
            return false;
        }
        Visit visit = v.content();
        List<String> visitors = new ArrayList<>();
        relationships
                .familyMember(visit.familyMemberId())
                .map(FamilyMember::fullName)
                .ifPresent(visitors::add);
        visit.companions().forEach(c -> visitors.add(c.fullName()));
        events.publishEvent(
                new ResidentVisitReminder(v.id(), residentId, visit.startTime(), visit.endTime(), visitors, at));
        return true;
    }
}
