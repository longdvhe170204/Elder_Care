package com.eldercare.backend.incident;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.care.api.TaskSourceApi;
import com.eldercare.backend.care.api.TaskStatisticsApi;
import com.eldercare.backend.care.api.spi.AttendanceTracePort;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.incident.api.spi.EmergencyFamilyPort;
import com.eldercare.backend.medication.api.MedicationScheduleApi;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.ResidentStatusApi;
import com.eldercare.backend.resident.api.dto.IdentityView;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.roombedallocation.api.LocationHistoryApi;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * Nền cho IT lây nhiễm (US8; T051): bốn nguồn truy vết (003 cùng phòng, 014 điểm danh, 008 phân công, 012 lượt thăm),
 * người ghi kết quả công việc, liều (005, 006), khoảng trạng thái (001), vị trí hiện tại (003) và nguồn công việc 005 là
 * bản giả Mockito chung cho các lớp để dùng một context. Sự cố lây nhiễm của {@link #resident} do Điều dưỡng ghi.
 */
@RecordApplicationEvents
abstract class ContactTracingTestBase extends IncidentHttpTestBase {
    static final String LOOKUP = "/api/v1/incidents/{id}/contact-lookup";
    static final String LIST = "/api/v1/incidents/{id}/contact-list";
    static final String CONTACTS = "/api/v1/contact-lists/{id}/contacts";
    static final String CONFIRM = "/api/v1/contact-lists/{id}/confirm";

    @MockitoBean
    LocationHistoryApi history;

    @MockitoBean
    AttendanceTracePort attendance;

    @MockitoBean
    AlertStaffPort staff;

    @MockitoBean
    TaskStatisticsApi careResults;

    @MockitoBean
    MedicationScheduleApi doses;

    @MockitoBean
    EmergencyFamilyPort family;

    @MockitoBean
    ResidentStatusApi statuses;

    @MockitoBean
    ResidentApi residents;

    @MockitoBean
    ResidentLocationApi locations;

    @MockitoBean
    TaskSourceApi taskSources;

    @Autowired
    ParameterApi parameters;

    TestUser doctor;
    long incidentId;
    Instant discoveredAt;
    Instant traceFrom;

    @BeforeEach
    void prepareInfection() {
        doctor = users.staff(Role.DOCTOR);
        when(residents.status(anyLong())).thenReturn(ResidentStatus.IN_RESIDENCE);
        when(residents.identity(anyLong()))
                .thenAnswer(call -> new IdentityView(call.getArgument(0), "HS", "[NCT kiểm thử]", Optional.empty()));
        when(locations.current(anyLong())).thenReturn(Optional.empty());
        when(locations.current(resident))
                .thenReturn(Optional.of(new LocationView(null, null, floor, "Tầng 2", 201L, "P201", 1L, "G1")));
        when(history.roommates(anyLong(), any(), any())).thenReturn(List.of());
        when(attendance.coAttendance(anyLong(), any(), any())).thenReturn(List.of());
        when(staff.assignmentsInRange(anyLong(), any(), any())).thenReturn(List.of());
        when(careResults.resultRecorders(anyLong(), any(), any())).thenReturn(Set.of());
        when(doses.staffRecordedDose(anyLong(), any(), any())).thenReturn(Set.of());
        when(family.visitsInRange(anyLong(), any(), any())).thenReturn(List.of());
        when(statuses.statusPeriods(anyLong(), any(), any(), any())).thenReturn(List.of());
        incidentId = incident("SUSPECTED_INFECTION", "MEDIUM", "NEW", null, nurse.accountId());
        discoveredAt = jdbc.queryForObject(
                        "SELECT discovered_at FROM incident WHERE id = ?", java.sql.Timestamp.class, incidentId)
                .toInstant();
        Duration window = parameters.read(ParameterCode.CFG_M05_07).asDuration();
        traceFrom = discoveredAt.minus(window);
    }

    /** Lập danh sách Nháp bằng lệnh của {@code as}; trả mã danh sách. */
    long createList(TestUser as) throws Exception {
        post(as, LIST, incidentId, "{}").andExpect(status().isCreated());
        return jdbc.queryForObject(
                "SELECT id FROM simple_record WHERE kind = 'M05.CONTACT_LIST' AND unique_key = ?",
                Long.class,
                Long.toString(incidentId));
    }

    long contactId(long listId, String type, long subjectId) {
        return jdbc.queryForObject(
                "SELECT MAX(id) FROM simple_record WHERE kind = 'M05.CONTACT_PERSON' AND entity_id = ?"
                        + " AND JSON_UNQUOTE(JSON_EXTRACT(content, '$.type')) = ?"
                        + " AND JSON_EXTRACT(content, '$.subjectId') = ?",
                Long.class,
                listId,
                type,
                subjectId);
    }

    static String contact(String type, Long subjectId, String group, Instant from, Instant to, String reason) {
        return "{\"type\":\"" + type + "\"" + (subjectId == null ? ",\"fullName\":\"Người đi cùng\"" : "")
                + (subjectId == null ? "" : ",\"subjectId\":" + subjectId) + ",\"group\":\"" + group
                + "\",\"source\":\"Tra cứu\",\"contactFrom\":\"" + from + "\",\"contactTo\":\"" + to + "\""
                + (reason == null ? "" : ",\"reason\":\"" + reason + "\"") + "}";
    }
}
