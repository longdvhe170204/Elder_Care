package com.eldercare.backend.incident.internal.infection.service;

import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.care.api.TaskStatisticsApi;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.incident.api.spi.AttendanceTracePort;
import com.eldercare.backend.incident.api.spi.EmergencyFamilyPort;
import com.eldercare.backend.incident.internal.incident.domain.SuspectedIncident;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.incident.internal.infection.domain.AbsencePeriod;
import com.eldercare.backend.incident.internal.infection.domain.ContactGroup;
import com.eldercare.backend.incident.internal.infection.domain.ContactLookup;
import com.eldercare.backend.incident.internal.infection.domain.ContactPersonContent;
import com.eldercare.backend.incident.internal.infection.domain.ContactType;
import com.eldercare.backend.incident.internal.infection.persistence.ContactListStore;
import com.eldercare.backend.medication.api.MedicationScheduleApi;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.ResidentStatusApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.roombedallocation.api.LocationHistoryApi;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tra cứu truy vết bốn nguồn trong khoảng CFG-M05-07 trước thời điểm phát hiện (FR-060, research S-15): cùng phòng
 * (003), cùng buổi hoạt động (014), nhân viên phân công hoặc đã ghi kết quả công việc, liều (008, 005, 006), người thân
 * và người đi cùng của lượt thăm (012). Chỉ đọc: không lập, không đề xuất danh sách (Q-307).
 */
@Service
@Transactional(readOnly = true)
public class ContactTracingService {
    private static final Set<ResidentStatus> ABSENT = EnumSet.of(
            ResidentStatus.TEMPORARILY_ABSENT, ResidentStatus.HOSPITALIZED, ResidentStatus.OFF_SITE_ACTIVITY);

    private final IncidentStore incidents;
    private final ContactListStore lists;
    private final ParameterApi parameters;
    private final LocationHistoryApi locations;
    private final ObjectProvider<AttendanceTracePort> attendance;
    private final AlertStaffPort staff;
    private final ObjectProvider<TaskStatisticsApi> careResults;
    private final MedicationScheduleApi doses;
    private final EmergencyFamilyPort family;
    private final ResidentStatusApi statuses;
    private final ResidentApi residents;

    public ContactTracingService(
            IncidentStore incidents,
            ContactListStore lists,
            ParameterApi parameters,
            LocationHistoryApi locations,
            ObjectProvider<AttendanceTracePort> attendance,
            AlertStaffPort staff,
            ObjectProvider<TaskStatisticsApi> careResults,
            MedicationScheduleApi doses,
            EmergencyFamilyPort family,
            ResidentStatusApi statuses,
            ResidentApi residents) {
        this.incidents = incidents;
        this.lists = lists;
        this.parameters = parameters;
        this.locations = locations;
        this.attendance = attendance;
        this.staff = staff;
        this.careResults = careResults;
        this.doses = doses;
        this.family = family;
        this.statuses = statuses;
        this.residents = residents;
    }

    /** Nhóm rỗng nghĩa là tra cả bốn nguồn. Người lập danh sách mới tra được (bảng FR-061). */
    @RequiresPermission(function = Function.CONTACT_LIST, operation = Operation.CREATE)
    public ContactLookup lookup(Target target, long incidentId, Set<ContactGroup> groups) {
        var incident = incidents.findSuspected(incidentId).orElseThrow(NotFoundException::new);
        var window = window(incident);
        Instant from = window[0];
        Instant to = window[1];
        long resident = incident.residentId();
        var wanted = groups == null || groups.isEmpty()
                ? EnumSet.of(
                        ContactGroup.SAME_ROOM,
                        ContactGroup.ACTIVITY,
                        ContactGroup.CARE_STAFF,
                        ContactGroup.FAMILY_VISIT)
                : EnumSet.copyOf(groups);
        wanted.remove(ContactGroup.OTHER);
        var result = new ArrayList<ContactLookup.Group>();
        for (var group : wanted) {
            var people = switch (group) {
                case SAME_ROOM -> sameRoom(resident, from, to);
                case ACTIVITY -> activity(resident, from, to);
                case CARE_STAFF -> careStaff(resident, from, to);
                case FAMILY_VISIT -> familyVisits(resident, from, to);
                case OTHER -> List.<ContactPersonContent>of();
            };
            result.add(new ContactLookup.Group(group, people));
        }
        return new ContactLookup(from, to, absences(resident, from, to), result);
    }

    /** Khoảng truy vết chốt khi lập danh sách; chưa lập thì tính như lúc lập (BR-M05-10). */
    private Instant[] window(SuspectedIncident incident) {
        var list = lists.findByIncident(incident.id());
        if (list.isPresent()) {
            return new Instant[] {
                list.get().content().traceFrom(), list.get().content().traceTo()
            };
        }
        var duration = parameters.read(ParameterCode.CFG_M05_07).asDuration();
        return new Instant[] {incident.discoveredAt().minus(duration), incident.discoveredAt()};
    }

    private List<ContactPersonContent> sameRoom(long resident, Instant from, Instant to) {
        var people = new ArrayList<ContactPersonContent>();
        for (var row : locations.roommates(resident, from, to)) {
            if (row.residentId() == resident || !overlaps(row.from(), row.to(), from, to)) continue;
            String room = row.location() == null || row.location().roomName() == null
                    ? ""
                    : " " + row.location().roomName();
            people.add(person(
                    ContactType.RESIDENT,
                    row.residentId(),
                    row.fullName(),
                    null,
                    null,
                    ContactGroup.SAME_ROOM,
                    "Cùng phòng" + room,
                    max(row.from(), from),
                    min(row.to(), to),
                    false));
        }
        return people;
    }

    /** 014 chưa cài {@link AttendanceTracePort} thì nhóm này không có dữ liệu ({@code noData}). */
    private List<ContactPersonContent> activity(long resident, Instant from, Instant to) {
        var people = new ArrayList<ContactPersonContent>();
        var port = attendance.getIfAvailable();
        if (port == null) return people;
        for (var session : port.coAttendance(resident, from, to)) {
            if (!overlaps(session.from(), session.to(), from, to)) continue;
            for (long other : session.residentIds()) {
                if (other == resident) continue;
                people.add(person(
                        ContactType.RESIDENT,
                        other,
                        residents.identity(other).fullName(),
                        null,
                        null,
                        ContactGroup.ACTIVITY,
                        "Buổi hoạt động " + session.sessionId(),
                        max(session.from(), from),
                        min(session.to(), to),
                        false));
            }
        }
        return people;
    }

    /** CHK018: phân công trong khoảng hợp với người đã ghi kết quả công việc hoặc liều cho người đó. */
    private List<ContactPersonContent> careStaff(long resident, Instant from, Instant to) {
        var people = new ArrayList<ContactPersonContent>();
        var assigned = new HashSet<Long>();
        for (var assignment : staff.assignmentsInRange(resident, from, to)) {
            if (!overlaps(assignment.from(), assignment.to(), from, to)) continue;
            assigned.add(assignment.staffId());
            people.add(staffPerson(
                    assignment.staffId(),
                    "Phân công " + assignment.assignmentKind(),
                    max(assignment.from(), from),
                    min(assignment.to(), to)));
        }
        var recorders = new LinkedHashSet<Long>();
        var results = careResults.getIfAvailable();
        if (results != null) recorders.addAll(results.resultRecorders(resident, from, to));
        var doseRecorders = doses.staffRecordedDose(resident, from, to);
        recorders.addAll(doseRecorders);
        for (long recorder : recorders) {
            if (assigned.contains(recorder)) continue;
            String source = doseRecorders.contains(recorder) ? "Ghi liều thuốc" : "Ghi kết quả công việc";
            people.add(staffPerson(recorder, source, from, to));
        }
        return people;
    }

    /** Họ tên nhân viên không đọc ở đây: 008 cài SPI của M05 nên M05 gọi api 008 sẽ tạo vòng phụ thuộc module. */
    private ContactPersonContent staffPerson(long staffId, String source, Instant from, Instant to) {
        return person(ContactType.STAFF, staffId, null, null, null, ContactGroup.CARE_STAFF, source, from, to, true);
    }

    private List<ContactPersonContent> familyVisits(long resident, Instant from, Instant to) {
        var people = new ArrayList<ContactPersonContent>();
        for (var visit : family.visitsInRange(resident, from, to)) {
            Instant end = visit.exitedAt().orElse(to);
            if (!overlaps(visit.enteredAt(), end, from, to)) continue;
            for (var visitor : visit.people()) {
                var type = visitor.familyMemberId().isPresent() ? ContactType.FAMILY_MEMBER : ContactType.COMPANION;
                people.add(person(
                        type,
                        visitor.familyMemberId().orElse(null),
                        visitor.fullName(),
                        visitor.relationship().orElse(null),
                        visitor.phoneNumber().orElse(null),
                        ContactGroup.FAMILY_VISIT,
                        "Lượt thăm " + visit.visitId(),
                        max(visit.enteredAt(), from),
                        min(end, to),
                        visitor.hasNotificationChannel()));
            }
        }
        return people;
    }

    private List<AbsencePeriod> absences(long resident, Instant from, Instant to) {
        return statuses.statusPeriods(resident, ABSENT, from, to).stream()
                .filter(period -> overlaps(period.from(), period.to().orElse(to), from, to))
                .map(period -> new AbsencePeriod(
                        period.status().name(),
                        max(period.from(), from),
                        min(period.to().orElse(to), to)))
                .toList();
    }

    private static ContactPersonContent person(
            ContactType type,
            Long subjectId,
            String fullName,
            String relationship,
            String phoneNumber,
            ContactGroup group,
            String source,
            Instant from,
            Instant to,
            boolean hasChannel) {
        return new ContactPersonContent(
                type,
                subjectId,
                fullName,
                relationship,
                phoneNumber,
                group,
                source,
                from,
                to,
                null,
                null,
                null,
                null,
                null,
                hasChannel);
    }

    private static boolean overlaps(Instant start, Instant end, Instant from, Instant to) {
        return start.isBefore(to) && (end == null || end.isAfter(from));
    }

    private static Instant max(Instant a, Instant b) {
        return a.isAfter(b) ? a : b;
    }

    private static Instant min(Instant a, Instant b) {
        return a == null || a.isAfter(b) ? b : a;
    }
}
