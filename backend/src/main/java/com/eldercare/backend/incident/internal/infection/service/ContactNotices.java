package com.eldercare.backend.incident.internal.infection.service;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.InformationSubject;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.Q354Case;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.internal.infection.domain.ContactPersonContent;
import com.eldercare.backend.incident.internal.infection.domain.ContactType;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Yêu cầu thông báo của FR-062. Người tiếp xúc (nhân viên, người thân) chỉ nhận nơi, khoảng thời gian và số liên hệ của
 * viện, không có danh tính, phòng hay tình trạng người nghi nhiễm (Q-72); người thân mang mã Q-354 {@code CONTACT_LIST}.
 */
final class ContactNotices {
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(BusinessTime.ZONE);

    private ContactNotices() {}

    /** Người tiếp xúc có kênh nhận: nhân viên, người thân có mã; người đi cùng, người thân không kênh để Hành chính. */
    static Optional<NotificationRequest> toContact(
            long personId, ContactPersonContent person, String place, String facilityPhone, long actorId, Instant at) {
        RecipientGroup recipient;
        Optional<Q354Case> q354 = Optional.empty();
        if (person.type() == ContactType.STAFF) {
            recipient = new RecipientGroup.Staff(person.subjectId(), "CONTACT_LIST");
        } else if (person.type() == ContactType.FAMILY_MEMBER
                && person.subjectId() != null
                && person.hasNotificationChannel()) {
            recipient = new RecipientGroup.FamilyMember(person.subjectId(), "CONTACT_LIST");
            q354 = Optional.of(Q354Case.CONTACT_LIST);
        } else {
            return Optional.empty();
        }
        String text = message(place, person, facilityPhone);
        return Optional.of(request(
                "CONTACT_NOTIFICATION",
                personId,
                List.of(recipient),
                text,
                "Thông báo có thể đã tiếp xúc",
                Optional.empty(),
                actorId,
                at,
                q354));
    }

    /** Người phụ trách ca của tầng người tiếp xúc: lập công việc đo nhiệt độ (FR-062, 005 FR-017), không tự tạo việc. */
    static List<NotificationRequest> temperatureCheck(
            long personId, Optional<Long> floorId, int timesPerDay, Instant until, long actorId, Instant at) {
        if (floorId.isEmpty()) return List.of();
        String text = "Người cao tuổi trong danh sách tiếp xúc: cần lập công việc đo nhiệt độ " + timesPerDay
                + " lần/ngày đến " + TIME.format(until) + ".";
        return List.of(request(
                "CONTACT_TEMPERATURE_CHECK",
                personId,
                List.of(new RecipientGroup.ShiftLead(floorId.get(), new Substitution.Default())),
                text,
                "Lập công việc đo nhiệt độ",
                Optional.of(floorId.get()),
                actorId,
                at,
                Optional.empty()));
    }

    /** Quản lý viện nhận đầy đủ (FR-062): số người và sự cố nguồn. */
    static NotificationRequest toManager(long listId, long incidentId, int added, long actorId, Instant at) {
        String text = "Danh sách tiếp xúc của sự cố " + incidentId + " có thêm " + added + " người đã xác nhận.";
        return new NotificationRequest(
                "M05",
                "CONTACT_LIST_CONFIRMED",
                SourceGroup.INCIDENT,
                new SourceRecord("M05.CONTACT_LIST", Long.toString(listId)),
                "M05.CONTACT_LIST:" + listId + ":CONFIRMED:" + at.toEpochMilli(),
                Optional.of(NotificationLevel.MEDIUM),
                Optional.empty(),
                Optional.empty(),
                List.of(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)),
                List.of(new ContentPart(InformationType.GENERAL, text)),
                Optional.of(text),
                Optional.of("Danh sách tiếp xúc đã xác nhận"),
                Optional.of(actorId),
                false,
                at,
                Map.of("contactListId", listId, "incidentId", incidentId, "added", added),
                false,
                Optional.empty(),
                Optional.empty());
    }

    static String message(String place, ContactPersonContent person, String facilityPhone) {
        String phone =
                facilityPhone == null || facilityPhone.isBlank() ? "" : " Số liên hệ của viện: " + facilityPhone + ".";
        return "Có thể đã tiếp xúc tại " + place + " từ " + TIME.format(person.contactFrom()) + " đến "
                + TIME.format(person.contactTo()) + ", đề nghị làm theo hướng dẫn của viện." + phone;
    }

    private static NotificationRequest request(
            String eventType,
            long personId,
            List<RecipientGroup> recipients,
            String text,
            String title,
            Optional<Long> floorId,
            long actorId,
            Instant at,
            Optional<Q354Case> q354) {
        return new NotificationRequest(
                "M05",
                eventType,
                SourceGroup.INCIDENT,
                new SourceRecord("M05.CONTACT_PERSON", Long.toString(personId)),
                "M05.CONTACT_PERSON:" + personId + ":" + eventType,
                Optional.of(NotificationLevel.MEDIUM),
                Optional.empty(),
                floorId,
                recipients,
                List.of(new ContentPart(InformationType.GENERAL, text)),
                Optional.of(text),
                Optional.of(title),
                Optional.of(actorId),
                false,
                at,
                Map.of("contactPersonId", personId),
                false,
                q354.map(c -> InformationSubject.ABOUT_RESIDENT),
                q354);
    }
}
