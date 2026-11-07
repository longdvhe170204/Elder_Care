package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.incident.api.dto.ActivityAttendanceTraceView;
import com.eldercare.backend.incident.api.dto.AssignmentTraceView;
import com.eldercare.backend.incident.api.dto.VisitTracePersonView;
import com.eldercare.backend.incident.api.dto.VisitTraceView;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusPeriod;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import com.eldercare.backend.roombedallocation.api.dto.RoommateView;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * BR-M05-10, FR-060, SC-008 (US8 #1, #2): tra cứu đủ bốn nguồn trong khoảng CFG-M05-07 trước thời điểm phát hiện, kèm
 * nhóm, nguồn, khoảng tiếp xúc và khoảng vắng của người nghi nhiễm; không lưu, không tự lập danh sách (Q-307).
 */
class BR_M05_10_ContactLookupIT extends ContactTracingTestBase {
    private static final Duration DAY = Duration.ofDays(1);

    long roommate;
    long staleRoommate;
    long activityC;
    long activityE;
    long carer;
    long doseRecorder;
    long taskRecorder;
    long relative;

    void fiveDaysOfData() {
        roommate = TestUsers.nextId();
        staleRoommate = TestUsers.nextId();
        activityC = TestUsers.nextId();
        activityE = TestUsers.nextId();
        carer = TestUsers.nextId();
        doseRecorder = TestUsers.nextId();
        taskRecorder = TestUsers.nextId();
        relative = TestUsers.nextId();
        var room = new LocationView(null, null, floor, "Tầng 2", 201L, "P201", 2L, "G2");
        when(history.roommates(eq(resident), any(), any()))
                .thenReturn(List.of(
                        new RoommateView(roommate, "Người B", room, traceFrom.plus(DAY), null, false, false, List.of()),
                        new RoommateView(
                                staleRoommate,
                                "Người ở trước khoảng",
                                room,
                                traceFrom.minus(DAY.multipliedBy(3)),
                                traceFrom.minus(DAY),
                                false,
                                false,
                                List.of())));
        var session = traceFrom.plus(DAY.multipliedBy(2));
        when(attendance.coAttendance(eq(resident), any(), any()))
                .thenReturn(List.of(new ActivityAttendanceTraceView(
                        TestUsers.nextId(),
                        session,
                        session.plus(Duration.ofHours(1)),
                        List.of(resident, activityC, activityE))));
        var shiftStart = traceFrom.plus(DAY.multipliedBy(3));
        when(staff.assignmentsInRange(eq(resident), any(), any()))
                .thenReturn(List.of(new AssignmentTraceView(
                        TestUsers.nextId(),
                        carer,
                        Role.CAREGIVER,
                        "SHIFT_MEMBER",
                        "RESIDENT",
                        resident,
                        shiftStart,
                        shiftStart.plus(Duration.ofHours(8)))));
        when(doses.staffRecordedDose(eq(resident), any(), any())).thenReturn(Set.of(carer, doseRecorder));
        when(careResults.resultRecorders(eq(resident), any(), any())).thenReturn(Set.of(taskRecorder));
        var entered = traceFrom.plus(DAY.multipliedBy(4));
        when(family.visitsInRange(eq(resident), any(), any()))
                .thenReturn(List.of(new VisitTraceView(
                        TestUsers.nextId(),
                        entered,
                        Optional.of(entered.plus(Duration.ofHours(1))),
                        List.of(
                                new VisitTracePersonView(
                                        Optional.of(relative),
                                        "Người thân N",
                                        Optional.of("Con gái"),
                                        Optional.of("0901000002"),
                                        true),
                                new VisitTracePersonView(
                                        Optional.empty(),
                                        "Người đi cùng",
                                        Optional.of("Bạn"),
                                        Optional.empty(),
                                        false)))));
        when(statuses.statusPeriods(eq(resident), any(), any(), any()))
                .thenReturn(List.of(new StatusPeriod(
                        ResidentStatus.TEMPORARILY_ABSENT,
                        traceFrom.plus(DAY.multipliedBy(2)),
                        Optional.of(traceFrom.plus(DAY.multipliedBy(3))))));
    }

    @Test
    void BR_M05_10_lookupReturnsFourSourcesInTraceWindowWithoutCreatingList() throws Exception {
        fiveDaysOfData();

        var body = lookup(nurse, "").andExpect(status().isOk());
        var result = json(body.andReturn().getResponse().getContentAsString());

        assertThat(Instant.parse(result.get("traceTo").asString())).isEqualTo(discoveredAt);
        assertThat(Instant.parse(result.get("traceFrom").asString())).isEqualTo(traceFrom);
        assertThat(subjects(result, "SAME_ROOM")).containsExactly(roommate);
        assertThat(subjects(result, "ACTIVITY")).containsExactlyInAnyOrder(activityC, activityE);
        assertThat(subjects(result, "CARE_STAFF")).containsExactlyInAnyOrder(carer, doseRecorder, taskRecorder);
        assertThat(people(result, "FAMILY_VISIT")).hasSize(2);
        assertThat(subjects(result, "FAMILY_VISIT")).containsExactly(relative);
        var roomRow = people(result, "SAME_ROOM").getFirst();
        assertThat(Instant.parse(roomRow.get("contactFrom").asString())).isEqualTo(traceFrom.plus(DAY));
        assertThat(Instant.parse(roomRow.get("contactTo").asString())).isEqualTo(discoveredAt);
        assertThat(roomRow.get("source").asString()).contains("P201");
        assertThat(result.get("suspectedPersonAbsences")).hasSize(1);
        assertThat(result.get("suspectedPersonAbsences").get(0).get("status").asString())
                .isEqualTo("TEMPORARILY_ABSENT");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM simple_record WHERE kind = 'M05.CONTACT_LIST' AND unique_key = ?",
                        Integer.class,
                        Long.toString(incidentId)))
                .isZero();
    }

    @Test
    void BR_M05_10_groupFilterReturnsOnlyRequestedSourceAndMarksEmptyGroups() throws Exception {
        fiveDaysOfData();
        when(history.roommates(eq(resident), any(), any())).thenReturn(List.of());

        var result = json(lookup(doctor, "?group=SAME_ROOM&group=CARE_STAFF")
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

        var groups = new ArrayList<String>();
        result.get("group").forEach(g -> groups.add(g.get("group").asString()));
        assertThat(groups).containsExactlyInAnyOrder("SAME_ROOM", "CARE_STAFF");
        for (var g : result.get("group")) {
            if (g.get("group").asString().equals("SAME_ROOM"))
                assertThat(g.get("noData").asBoolean()).isTrue();
        }
        lookup(doctor, "?group=OTHER").andExpect(status().isUnprocessableContent());
    }

    @Test
    void BR_M05_10_floorManagerAndManagerOnlyViewSoCannotLookUp() throws Exception {
        lookup(floorManager, "").andExpect(status().isForbidden());
        lookup(users.staff(Role.NURSING_HOME_MANAGER), "").andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.ResultActions lookup(TestUser as, String query) throws Exception {
        return mvc.perform(as(as, get(LOOKUP.replace("{id}", Long.toString(incidentId)) + query)));
    }

    private static JsonNode json(String body) {
        return JsonMapper.builder().build().readTree(body);
    }

    private static List<JsonNode> people(JsonNode result, String group) {
        var rows = new ArrayList<JsonNode>();
        for (var g : result.get("group")) {
            if (g.get("group").asString().equals(group)) g.get("people").forEach(rows::add);
        }
        return rows;
    }

    private static List<Long> subjects(JsonNode result, String group) {
        return people(result, group).stream()
                .filter(p -> p.hasNonNull("subjectId"))
                .map(p -> p.get("subjectId").asLong())
                .toList();
    }
}
