package com.eldercare.backend.incident.internal.infection.service;

import static com.eldercare.backend.incident.internal.infection.domain.ContactListActions.*;
import static com.eldercare.backend.incident.internal.infection.persistence.ContactPersonStore.ACTIVE;
import static com.eldercare.backend.incident.internal.infection.persistence.ContactPersonStore.DRAFT;
import static com.eldercare.backend.incident.internal.infection.persistence.ContactPersonStore.EXCLUDED;
import static com.eldercare.backend.incident.internal.infection.persistence.ContactPersonStore.REMOVED;

import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.care.api.TaskSourceApi;
import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.ReasonRequiredException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.health.api.FacilityContactApi;
import com.eldercare.backend.incident.api.events.ContactAdded;
import com.eldercare.backend.incident.api.events.ContactNotificationRequested;
import com.eldercare.backend.incident.api.events.ContactTemperatureCheckRequested;
import com.eldercare.backend.incident.api.spi.EmergencyFamilyPort;
import com.eldercare.backend.incident.internal.incident.domain.SuspectedIncident;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.incident.internal.infection.domain.ContactGroup;
import com.eldercare.backend.incident.internal.infection.domain.ContactListContent;
import com.eldercare.backend.incident.internal.infection.domain.ContactListDetails;
import com.eldercare.backend.incident.internal.infection.domain.ContactMonitoringParameter;
import com.eldercare.backend.incident.internal.infection.domain.ContactPersonContent;
import com.eldercare.backend.incident.internal.infection.domain.ContactType;
import com.eldercare.backend.incident.internal.infection.domain.NewContact;
import com.eldercare.backend.incident.internal.infection.persistence.ContactListStore;
import com.eldercare.backend.incident.internal.infection.persistence.ContactPersonStore;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vòng đời danh sách tiếp xúc theo bảng FR-061 và tác động FR-062. Người có quyền tự lập, thêm, bỏ, xác nhận, loại;
 * hệ thống không tự thêm người từ kết quả truy vết (Q-307) và không tự tạo lịch đo hay công việc.
 */
@Service
@Transactional
public class ContactListService {
    private static final String LIST = "M05.CONTACT_LIST";
    private static final String PERSON = "M05.CONTACT_PERSON";

    private final IncidentStore incidents;
    private final ContactListStore lists;
    private final ContactPersonStore people;
    private final ContactListAccess access;
    private final ParameterApi parameters;
    private final AuditLogApi audit;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;
    private final TaskSourceApi taskSources;
    private final ResidentLocationApi locations;
    private final FacilityContactApi facilityContact;
    private final EmergencyFamilyPort family;

    public ContactListService(
            IncidentStore incidents,
            ContactListStore lists,
            ContactPersonStore people,
            ContactListAccess access,
            ParameterApi parameters,
            AuditLogApi audit,
            BusinessTime time,
            ApplicationEventPublisher events,
            TaskSourceApi taskSources,
            ResidentLocationApi locations,
            FacilityContactApi facilityContact,
            EmergencyFamilyPort family) {
        this.incidents = incidents;
        this.lists = lists;
        this.people = people;
        this.access = access;
        this.parameters = parameters;
        this.audit = audit;
        this.time = time;
        this.events = events;
        this.taskSources = taskSources;
        this.locations = locations;
        this.facilityContact = facilityContact;
        this.family = family;
    }

    /** Đối tượng của sự cố cho lớp phạm vi của {@code @RequiresPermission}. */
    public Target target(long incidentId) {
        return access.ofIncident(incidentId);
    }

    /** Đối tượng của danh sách (người nghi nhiễm của sự cố nguồn). */
    public Target listTarget(long listId) {
        return access.ofList(listId);
    }

    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.CONTACT_LIST, operation = Operation.CREATE)
    public SimpleRecord<ContactListContent> createDraft(Target target, long incidentId) {
        var incident = incidents.lockSuspected(incidentId).orElseThrow(NotFoundException::new);
        if (!List.of("NEW", "IN_PROGRESS", "MONITORING").contains(incident.status())) {
            throw blocked("FR-061", "Sự cố lây nhiễm đã đóng");
        }
        if (lists.findByIncident(incidentId).isPresent()) {
            throw blocked("FR-061", "Sự cố đã có danh sách tiếp xúc");
        }
        var duration = parameters.read(ParameterCode.CFG_M05_07).asDuration();
        var row = lists.create(
                incidentId, incident.discoveredAt().minus(duration), incident.discoveredAt(), access.accountId());
        audit.write(AuditLogEntry.builder()
                .action(CREATED)
                .entity(LIST, row.id())
                .resident(incident.residentId())
                .after(Map.of("status", row.status(), "incidentId", incidentId))
                .build());
        return row;
    }

    /** Bác sĩ, Điều dưỡng trong phạm vi, Quản lý viện; Trưởng tầng trong phạm vi chỉ xem (api-007). */
    @RequiresPermission(function = Function.CONTACT_LIST, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public ContactListDetails view(Target target, long incidentId) {
        var list = lists.findByIncident(incidentId).orElseThrow(NotFoundException::new);
        var current = people.byList(list.id()).stream()
                .filter(row -> !REMOVED.equals(row.status()))
                .toList();
        return new ContactListDetails(list, incidentId, current);
    }

    /** Thêm vào Nháp, hoặc bổ sung khi Đã xác nhận (bắt buộc lý do, áp FR-062 cho người mới). */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.CONTACT_LIST, operation = Operation.UPDATE)
    public SimpleRecord<ContactPersonContent> add(Target target, long listId, NewContact contact) {
        var list = lists.lock(listId);
        var incident = incidentOf(list);
        boolean confirmed = "CONFIRMED".equals(list.status());
        if (!confirmed && !"DRAFT".equals(list.status())) throw blocked("FR-061", "Danh sách tiếp xúc đã kết thúc");
        validate(contact, incident.residentId());
        if ((confirmed || contact.group() == ContactGroup.OTHER) && blank(contact.reason())) {
            throw new ReasonRequiredException();
        }
        if (people.current(listId, contact.type(), contact.subjectId()).isPresent()) {
            throw blocked("FR-061", "Người này đã có trong danh sách tiếp xúc");
        }
        var content = new ContactPersonContent(
                contact.type(),
                contact.subjectId(),
                contact.fullName(),
                contact.relationship(),
                contact.phoneNumber(),
                contact.group(),
                contact.source(),
                contact.contactFrom(),
                contact.contactTo(),
                blank(contact.reason()) ? null : contact.reason(),
                null,
                null,
                null,
                null,
                hasChannel(contact, incident.residentId(), list.content()));
        var at = time.now();
        var monitoring = confirmed ? monitoring() : null;
        if (monitoring != null)
            content = content.monitored(at, at.plus(monitoring.duration().toDuration()));
        var row = people.add(listId, content, confirmed ? ACTIVE : DRAFT, access.accountId());
        audit.write(AuditLogEntry.builder()
                .action(CONTACT_ADDED)
                .entity(PERSON, row.id())
                .resident(incident.residentId())
                .after(Map.of("contactListId", listId, "type", contact.type().name(), "status", row.status()))
                .build());
        if (confirmed) {
            applyEffects(list.id(), incident, List.of(row), monitoring, at);
        }
        return row;
    }

    /** Bỏ người khỏi danh sách Nháp; dòng giữ làm lịch sử (bảng FR-061). */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.CONTACT_LIST, operation = Operation.UPDATE)
    public void remove(Target target, long listId, long contactId) {
        var list = lists.lock(listId);
        var incident = incidentOf(list);
        if (!"DRAFT".equals(list.status())) {
            throw blocked("FR-061", "Chỉ bỏ người khi danh sách ở Nháp; sau xác nhận dùng lệnh loại người");
        }
        var row = contactOf(listId, contactId);
        if (!DRAFT.equals(row.status())) throw blocked("FR-061", "Người này đã bị bỏ khỏi danh sách");
        people.change(row, REMOVED, row.content());
        audit.write(AuditLogEntry.builder()
                .action(CONTACT_REMOVED)
                .entity(PERSON, contactId)
                .resident(incident.residentId())
                .before(Map.of("status", DRAFT))
                .after(Map.of("status", REMOVED))
                .build());
    }

    /** Loại người khỏi danh sách Đã xác nhận: gỡ dấu tiếp xúc, 005 hủy công việc đo nhiệt độ chưa đến hạn. */
    @BusinessCommand
    @RequiresPermission(function = Function.CONTACT_LIST, operation = Operation.UPDATE)
    public void exclude(Target target, long listId, long contactId, Reason reason) {
        if (reason == null) throw new ReasonRequiredException();
        var list = lists.lock(listId);
        var incident = incidentOf(list);
        if (!"CONFIRMED".equals(list.status())) {
            throw blocked("FR-061", "Chỉ loại người khỏi danh sách Đã xác nhận");
        }
        var row = contactOf(listId, contactId);
        if (!ACTIVE.equals(row.status())) throw blocked("FR-061", "Người này đã bị loại khỏi danh sách");
        people.change(row, EXCLUDED, row.content().excluded(time.now(), reason.value()));
        if (row.content().type() == ContactType.RESIDENT) {
            taskSources.sourceCancelled(SourceType.CONTACT_MONITORING, contactId, reason);
        }
        audit.write(AuditLogEntry.builder()
                .action(CONTACT_EXCLUDED)
                .entity(PERSON, contactId)
                .resident(incident.residentId())
                .before(Map.of("status", ACTIVE))
                .after(Map.of("status", EXCLUDED))
                .reason(reason)
                .build());
    }

    /** Nháp → Đã xác nhận: có ít nhất một người, hoặc "không có người tiếp xúc" kèm lý do (bảng FR-061). */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.CONTACT_LIST, operation = Operation.UPDATE)
    public SimpleRecord<ContactListContent> confirm(Target target, long listId, boolean noContacts, Reason reason) {
        var list = lists.lock(listId);
        var incident = incidentOf(list);
        var drafts = people.byList(listId).stream()
                .filter(row -> DRAFT.equals(row.status()))
                .toList();
        if (noContacts) {
            if (reason == null) throw new ReasonRequiredException();
            if (!drafts.isEmpty()) throw blocked("FR-061", "Danh sách đã có người, không ghi được \"không có người\"");
        } else if (drafts.isEmpty()) {
            throw blocked("FR-061", "Cần ít nhất một người hoặc ghi \"không có người tiếp xúc\" kèm lý do");
        }
        var at = time.now();
        var after = lists.confirm(listId, list.version(), at, access.accountId(), noContacts ? reason.value() : null);
        var monitoring = monitoring();
        var until = at.plus(monitoring.duration().toDuration());
        var confirmed = new ArrayList<SimpleRecord<ContactPersonContent>>();
        for (var row : drafts)
            confirmed.add(people.change(row, ACTIVE, row.content().monitored(at, until)));
        var entry = AuditLogEntry.builder()
                .action(CONFIRMED)
                .entity(LIST, listId)
                .resident(incident.residentId())
                .before(Map.of("status", "DRAFT"))
                .after(Map.of("status", after.status(), "contacts", confirmed.size()));
        if (noContacts) entry.reason(reason);
        audit.write(entry.build());
        applyEffects(listId, incident, confirmed, monitoring, at);
        return after;
    }

    /** Sự cố lây nhiễm đóng: Nháp, Đã xác nhận → Đã kết thúc; dấu tiếp xúc giữ tới hết khoảng theo dõi (FR-061). */
    public void endForIncident(long incidentId) {
        var list = lists.findByIncident(incidentId);
        if (list.isEmpty() || "ENDED".equals(list.get().status())) return;
        var after = lists.end(list.get().id(), list.get().version());
        audit.write(AuditLogEntry.builder()
                .action(ENDED)
                .entity(LIST, after.id())
                .before(Map.of("status", list.get().status()))
                .after(Map.of("status", after.status()))
                .build());
    }

    /** FR-062 cho từng người mới: dấu tiếp xúc (003), báo lập công việc đo nhiệt độ, thông báo người tiếp xúc, QL. */
    private void applyEffects(
            long listId,
            SuspectedIncident incident,
            List<SimpleRecord<ContactPersonContent>> added,
            ContactMonitoringParameter monitoring,
            Instant at) {
        if (added.isEmpty()) return;
        long actor = access.accountId();
        String place = locations
                .current(incident.residentId())
                .map(ContactListService::place)
                .orElse("viện");
        String phone = facilityContact.current().phoneNumber();
        var floors = new HashMap<Long, java.util.Optional<Long>>();
        for (var row : added) {
            var person = row.content();
            if (person.type() == ContactType.RESIDENT) {
                events.publishEvent(
                        new ContactAdded(person.subjectId(), at, PERSON + ":" + row.id() + ":ADDED", List.of()));
                var floor = floors.computeIfAbsent(
                        person.subjectId(), id -> locations.current(id).map(LocationView::floorId));
                events.publishEvent(new ContactTemperatureCheckRequested(
                        person.subjectId(),
                        row.id(),
                        person.monitoredFrom(),
                        person.monitoredTo(),
                        PERSON + ":" + row.id() + ":TEMPERATURE_CHECK",
                        at,
                        ContactNotices.temperatureCheck(
                                row.id(), floor, monitoring.timesPerDay(), person.monitoredTo(), actor, at)));
            } else {
                ContactNotices.toContact(row.id(), person, place, phone, actor, at)
                        .ifPresent(request -> events.publishEvent(new ContactNotificationRequested(
                                listId, PERSON + ":" + row.id() + ":NOTIFIED", at, List.of(request))));
            }
        }
        events.publishEvent(new ContactNotificationRequested(
                listId,
                LIST + ":" + listId + ":MANAGER:" + at.toEpochMilli(),
                at,
                List.of(ContactNotices.toManager(listId, incident.id(), added.size(), actor, at))));
    }

    /** Nơi tiếp xúc trong thông báo: tầng hoặc khu vực của người nghi nhiễm, không nêu phòng (Q-72). */
    private static String place(LocationView location) {
        if (location.floorName() != null) return location.floorName();
        return location.areaName() == null ? "viện" : location.areaName();
    }

    private ContactMonitoringParameter monitoring() {
        return parameters.read(ParameterCode.CFG_M05_08).asTable(ContactMonitoringParameter.class);
    }

    /** Người thân, người đi cùng: kênh nhận lấy từ lượt thăm 012 trong khoảng truy vết; không có thì để Hành chính. */
    private boolean hasChannel(NewContact contact, long suspectedResident, ContactListContent list) {
        return switch (contact.type()) {
            case STAFF -> true;
            case RESIDENT -> false;
            case FAMILY_MEMBER, COMPANION ->
                family.visitsInRange(suspectedResident, list.traceFrom(), list.traceTo()).stream()
                        .flatMap(visit -> visit.people().stream())
                        .filter(person -> contact.type() == ContactType.FAMILY_MEMBER
                                ? person.familyMemberId().equals(java.util.Optional.ofNullable(contact.subjectId()))
                                : person.familyMemberId().isEmpty()
                                        && Objects.equals(person.fullName(), contact.fullName()))
                        .anyMatch(person -> person.hasNotificationChannel());
        };
    }

    private static void validate(NewContact contact, long suspectedResident) {
        if (contact.type() == null || contact.group() == null || blank(contact.source())) {
            throw new InvalidValueException("Người tiếp xúc cần loại, nhóm và nguồn");
        }
        if (contact.contactFrom() == null
                || contact.contactTo() == null
                || !contact.contactFrom().isBefore(contact.contactTo())) {
            throw new InvalidValueException("Khoảng tiếp xúc không hợp lệ");
        }
        if (contact.type() == ContactType.COMPANION ? blank(contact.fullName()) : contact.subjectId() == null) {
            throw new InvalidValueException("Thiếu người tiếp xúc");
        }
        if (contact.type() == ContactType.RESIDENT && contact.subjectId() == suspectedResident) {
            throw new InvalidValueException("Người nghi nhiễm không thuộc danh sách tiếp xúc của chính sự cố");
        }
    }

    private SuspectedIncident incidentOf(SimpleRecord<ContactListContent> list) {
        return incidents.findSuspected(list.parent().entityId()).orElseThrow(NotFoundException::new);
    }

    private SimpleRecord<ContactPersonContent> contactOf(long listId, long contactId) {
        var row = people.lock(contactId);
        if (row.parent() == null || row.parent().entityId() != listId) throw new NotFoundException();
        return row;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static ConditionNotMetException blocked(String code, String message) {
        return new ConditionNotMetException(List.of(new UnmetCondition(code, message)));
    }
}
