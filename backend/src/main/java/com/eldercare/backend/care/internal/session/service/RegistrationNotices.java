package com.eldercare.backend.care.internal.session.service;

import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.care.api.events.ActivityRegistrationsCancelled;
import com.eldercare.backend.care.api.events.LateReturnConsentNotice;
import com.eldercare.backend.care.internal.session.domain.Registration;
import com.eldercare.backend.care.internal.session.domain.Session;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 014 bảng thông báo dòng "Buổi bị hủy hoặc dời; đăng ký bị hủy tự động": người phụ trách buổi, người đã đăng ký cho
 * người cao tuổi (nguồn thường xuyên: Trưởng tầng của tầng buổi), Trưởng tầng. Một sự kiện mỗi buổi mỗi lý do.
 */
@Component
public class RegistrationNotices {
    private final ApplicationEventPublisher events;

    public RegistrationNotices(ApplicationEventPublisher events) {
        this.events = events;
    }

    public void cancelled(Session session, List<Registration> registrations, String cause, Instant at) {
        if (registrations.isEmpty()) return;
        var recipients = new LinkedHashSet<RecipientGroup>();
        if (session.tripStatus() != null)
            recipients.add(new RecipientGroup.TripLeader(session.id(), new Substitution.Default()));
        if (session.responsibleStaffId() != null)
            recipients.add(new RecipientGroup.Staff(session.responsibleStaffId(), "SESSION_RESPONSIBLE"));
        if (session.floorAreaId() != null)
            recipients.add(new RecipientGroup.FloorManager(session.floorAreaId(), new Substitution.Default()));
        for (var r : registrations)
            if (r.registeredByStaffId() != null && !Registration.REGULAR.equals(r.source()))
                recipients.add(new RecipientGroup.Staff(r.registeredByStaffId(), "REGISTERED_BY"));
        var ids = registrations.stream().map(Registration::id).toList();
        events.publishEvent(new ActivityRegistrationsCancelled(
                session.id(),
                ids,
                cause,
                new CareEventContext(
                        ActivitySessionService.ENTITY,
                        session.id(),
                        "REGISTRATIONS_CANCELLED:" + cause + ":" + ids,
                        at,
                        Optional.ofNullable(session.floorAreaId()),
                        List.copyOf(recipients))));
    }

    /**
     * 014 bảng thông báo dòng FR-016a: rút đồng ý ({@code representativeId} null) báo người đã đăng ký (nguồn thường
     * xuyên: Trưởng tầng của tầng buổi, như dòng hủy tự động) và Hành chính; giờ về dời theo đồng ý báo thêm người đại
     * diện đã đồng ý. Khóa (loại, buổi, người cao tuổi), lần dời thêm giờ về mới.
     */
    public void lateReturn(
            Session session,
            Registration registration,
            String change,
            LocalDate date,
            LocalTime expectedReturnTime,
            Long representativeId,
            Instant at) {
        var recipients = new LinkedHashSet<RecipientGroup>();
        if (registration.registeredByStaffId() != null && !Registration.REGULAR.equals(registration.source()))
            recipients.add(new RecipientGroup.Staff(registration.registeredByStaffId(), "REGISTERED_BY"));
        else if (session.floorAreaId() != null)
            recipients.add(new RecipientGroup.FloorManager(session.floorAreaId(), new Substitution.Default()));
        recipients.add(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF));
        if (representativeId != null)
            recipients.add(new RecipientGroup.FamilyMember(representativeId, "REPRESENTATIVE"));
        String milestone = "LATE_RETURN:" + change + ":" + registration.residentId()
                + (expectedReturnTime == null ? "" : ":" + date + ":" + expectedReturnTime);
        events.publishEvent(new LateReturnConsentNotice(
                session.id(),
                registration.residentId(),
                change,
                date,
                expectedReturnTime,
                new CareEventContext(
                        ActivitySessionService.ENTITY,
                        session.id(),
                        milestone,
                        at,
                        Optional.ofNullable(session.floorAreaId()),
                        List.copyOf(recipients))));
    }
}
